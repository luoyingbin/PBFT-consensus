package com.pbft;

import com.common.StringUtils;
import com.common.thread.Threads;
import com.db.DbManager;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import com.pbft.consensus.Status;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: luo
 * @Description: 共识管理器
 * 包含同步 共识节点加减
 * @Data: 9:43 2021/9/23
 */
@Service
@Slf4j
public class ConsensusManager {
    //TODO:临时，测试用
    @Value("${server.port}")
    int port;


    static PBFTProcess pbftProcess;


    /**
     * 共识节点列表
     */
    static ValidatorManager validatorManager;
    /**
     * 网络广播服务
     */
    static Net net;
    /**
     * 异步操作任务调度线程池
     */
    private static ScheduledExecutorService executor;
    /**
     *核心线程池大小
    * */
    private static final int corePoolSize = 50;


    private static ReentrantReadWriteLock isActiveLock;
    private static boolean  isActive;

    private  ReentrantReadWriteLock poolListLock;
    List<String> poolList;

    ScheduledFuture onTimer;

    private String loadValue(){
        try {
            poolListLock.writeLock().lock();
            String str=null;
            if(poolList.size() >0) {
                str=poolList.get(0);
                poolList.remove(0);
            }
            return str;
        }finally {
            poolListLock.writeLock().unlock();
        }
    }

    public void addPool(String value){
        try {
            poolListLock.writeLock().lock();
            poolList.add(value);
        }finally {
            poolListLock.writeLock().unlock();
        }
    }



    @PostConstruct
    public void init() {
        DbManager.getConsensusDB(port);
        pbftProcess = new PBFTProcess(port);
        //初始化启动，广播请求所有节点状态
        Message message = Status.newQuestStatus();
        broadMessage(message);
        //定时器，1秒一次
        onTimer = executor.scheduleWithFixedDelay(onTimer(), 0, 1, TimeUnit.SECONDS);
        if(pbftProcess.isLeader()){
            //启动交易池，轮流发送交易
            ConsensusManager.openConsensus();
        }
    }

    public ConsensusManager() {
        validatorManager = ValidatorManager.getProfile();
        isActiveLock = new ReentrantReadWriteLock();
        isActive = false;
        poolListLock=new ReentrantReadWriteLock();
        poolList = new ArrayList<>();
        net = Net.getIntance();
        executor = new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }

    /**
     * * TODO：测试正常流程，测试checkpoint，测试viewChange，补充日志输出
     * TODO:
     * 节点变化过程：
     * 1、发起节点变化请求
     * 2、检查请求合法性，合法则收到的节点保存请求到持久盘
     * 3、节点停止一切共识、包括pbft viewChange机制，发送同意消息（同意消息包含当前节点状态（checkpoint,checkpoint后已共识结果））、启用超时同步恢复定时器
     * 4、节点收集同意结果，达到一定数量后重新计算当前节点稳定检查点（如果落后启动同步过程），重新计算验证节点个数，恢复共识，启动viewChange定时器，发送joinNet
     * 5、重新计算后如果是leader，需要额外等待joinNet收集到一定数量后才发送共识请求
     * 6、
     */
    public void loadPool() {
        try {
            isActiveLock.readLock().lock();
            if(!isActive){
                return;
            }
            String value = loadValue();
            if(StringUtils.isNull(value)){
                return;
            }
            pbftProcess.request(value,0);
        }finally {
            isActiveLock.readLock().unlock();
        }
    }

    public static void openConsensus(){
        isActiveLock.writeLock().lock();
        isActive = true;
        isActiveLock.writeLock().unlock();
    }

    private void closeConsensus() {
        isActiveLock.writeLock().lock();
        isActive = false;
        isActiveLock.writeLock().unlock();
    }

    /**
     *  1、准备发起新的特殊共识
     *  2、在此之前需要原来的共识都已完成，特殊共识序号最好时进入checkpoint后下一个
     *  3、发起特殊共识
     * */
    private static TimerTask upgrade(PBFTProcess pbftProcess,String address,int upgrade) {
        return new TimerTask() {
            @Override
            public void run() {
                while (pbftProcess.inConsensus()){
                    log.debug("还有未完成的共识，暂时无法升级");
                    Threads.sleep(1000);
                }
                pbftProcess.request(address,upgrade);
            }
        };
    }

    public static void upgradeDown(){
        pbftProcess.closeUpgrade();
        if (pbftProcess.isLeader()) {
            //启动交易池，轮流发送交易
            openConsensus();
        }
    }

    private TimerTask onTimer() {
        return new TimerTask() {
            @Override
            public void run() {
              //log.debug("定时器任务:{}",System.currentTimeMillis());
                loadPool();
            }
        };
    }

    /**
     * 添加节点
     * */
    public void addNode(String address){
        Message message = new Message();
        message.setMessageType(MessageType.REQUEST_UP_NODE);
        message.setViewNumber(0);
        message.setValue(address);
        message.setNumber(0);
        message.setUpgrade(1);
        message.setTimeMillis(System.currentTimeMillis());
        message.setDigest("");
        broadMessage(message);
    }

    /**
     * 删除节点
     * */
    public void delNode(String address){
        Message message = new Message();
        message.setMessageType(MessageType.REQUEST_UP_NODE);
        message.setViewNumber(0);
        message.setValue(address);
        message.setNumber(0);
        message.setUpgrade(-1);
        message.setTimeMillis(System.currentTimeMillis());
        message.setDigest("");
        broadMessage(message);
    }


    /**
     * @Author: luo
     * @Description: 消息处理
     * @param message 消息体
     * @Data: 9:29 2021/11/29
     */
    public void receiveMessage(Message message) {
        if (!baseVerify(message)) {
            return;
        }
        if (MessageType.REQUEST_UP_NODE.equals(message.getMessageType())) {
            if (pbftProcess.isLeader()) {
                closeConsensus();
                executor.schedule(upgrade(pbftProcess,message.getValue(),message.getUpgrade()),0,TimeUnit.SECONDS);
            }
            return;
        }
        if (MessageType.QUERY_STATUS.equals(message.getMessageType())) {
            //TODO:处理收到请求最新的状态信息
            pbftProcess.sendStatus(message.getOrderNumber());
            return;
        }
        if (MessageType.STATUS.equals(message.getMessageType())) {
            //收集状态信息直到一半以上
            //如果发现落后，从落后的点开始同步数据
            // TODO:处理收到返回的最新状态信息和对应的共识结果
            log.debug("收到节点:{}状态信息:view:{},number:{},hash:{}", message.getOrderNumber(), message.getViewNumber(), message.getNumber(), message.getDigest());
            return;
        }
        pbftProcess.receiveMessage(message);
    }

    static public boolean baseVerify(Message message) {
        if (StringUtils.isEmpty(message.getPublicKey())) {
            log.debug("消息的公钥不存在");
            return false;
        }

        if (StringUtils.isNull(message.getSignData())) {
            log.debug("消息的签名不存在");
            return false;
        }

        if (StringUtils.isNull(message.getDigest())) {
            log.debug("摘要字段不能为null");
            return false;
        }

        if (StringUtils.isNull(message.getValue())) {
            log.debug("value字段不能为null");
            return false;
        }

        int orderNumber = validatorManager.verifyAndGetOrderNumber(message.getPublicKey(),
                message.getSignData(), message.getData().getBytes(StandardCharsets.UTF_8));
        if (orderNumber == -1) {
            log.debug("该消息不在验证节点列表");
            return false;
        }
        if (orderNumber == -2) {
            log.debug("签名验证失败");
            return false;
        }

        if (orderNumber != message.getOrderNumber()) {
            log.debug("消息中记录的发送者ID与签名者ID不一致");
            return false;
        }
        return true;
    }

    public static void broadMessage(Message message) {
        message.setOrderNumber(validatorManager.getOneSelfOrderNumber());
        String signData = validatorManager.sign(message.getData().getBytes(StandardCharsets.UTF_8));
        message.setPublicKey(validatorManager.getPublicKey());
        message.setSignData(signData);
        executor.schedule(net.asyncPostAll(message.toString()),0,TimeUnit.SECONDS);
    }

    public static void sendMessage(int index, Message message) {
        message.setOrderNumber(validatorManager.getOneSelfOrderNumber());
        String signData = validatorManager.sign(message.getData().getBytes(StandardCharsets.UTF_8));
        message.setPublicKey(validatorManager.getPublicKey());
        message.setSignData(signData);
        net.post(validatorManager.getAddress(index), message.toString());
    }
}
