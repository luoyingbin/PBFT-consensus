package com.pbft.message;

import com.common.StringUtils;
import com.pbft.common.model.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: luo
 * @Description: （v,n）管理器
 * @Data: 9:14 2021/9/2
 */
public class MessageManager {
    private static MessageManager messageManager;

    /**消息列表  (n,v,m) 消息队列 */
    int mapSize = 100;
    Map<Integer, Map<Integer, MessageLog>>  messageLogMap;

    private final ReentrantReadWriteLock messageLogMapLock;

    private MessageManager(){
        messageLogMapLock=new ReentrantReadWriteLock();
        messageLogMap =new HashMap<>(mapSize);
    }

    public static MessageManager getProfile() {
        if (StringUtils.isNull(messageManager)) {
            messageManager = new MessageManager();
        }
        return messageManager;
    }

    /**
     * 确认在number操作序号后有无共识在进行
     * */
    public boolean hasConsensus(int number) {
        messageLogMapLock.readLock().lock();
        try {
            for (int n : messageLogMap.keySet()) {
                if (n > number) {
                    Map<Integer, MessageLog> map = messageLogMap.get(n);
                    for (MessageLog msg : map.values()) {
                        if (StringUtils.isNotNull(msg.getPrePrepare())) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } finally {
            messageLogMapLock.readLock().unlock();
        }
    }



    /**
     * @Author: luo
     * @Description: 读取(v,n)消息队列 优先使用读锁，如果数据不存在，升级为写锁创建资源后再降级读锁返回
     * @param number 编号
     * @param viewNumber 视图
     * @Data: 16:15 2021/9/1
     */
    public MessageLog getMessageLog(int viewNumber,int number) {
        Map<Integer, MessageLog> map =null;
        MessageLog messageLog = null;
        //读取（v,n）前先加上锁
        messageLogMapLock.readLock().lock();
        map = messageLogMap.get(number);
        //如果获取不到(v,n)第一个map，加上写锁
        if (StringUtils.isNull(map)) {
            //获取写锁前须释放读锁
            messageLogMapLock.readLock().unlock();
            //升级为写锁
            messageLogMapLock.writeLock().lock();
            map = messageLogMap.get(number);
            if(StringUtils.isNull(map)) {
                map = new HashMap<>(mapSize);
                map.put(viewNumber, new MessageLog(number, viewNumber));
                messageLogMap.put(number,map);
            }
            messageLog = map.get(viewNumber);
            if(StringUtils.isNull(messageLog)) {
                map.put(viewNumber, new MessageLog(number, viewNumber));
            }
            //锁降级，在释放写锁前获取读锁
            messageLogMapLock.readLock().lock();
            messageLogMapLock.writeLock().unlock();
        }
        messageLog = map.get(viewNumber);
        if (StringUtils.isNull(messageLog)) {
            //获取写锁前须释放读锁
            messageLogMapLock.readLock().unlock();
            //升级为写锁
            messageLogMapLock.writeLock().lock();
            messageLog = map.get(viewNumber);
            if(StringUtils.isNull(messageLog)) {
                map.put(viewNumber, new MessageLog(number, viewNumber));
            }
            //锁降级，在释放写锁前获取读锁
            messageLogMapLock.readLock().lock();
            messageLogMapLock.writeLock().unlock();
        }
        //释放读锁
        messageLogMapLock.readLock().unlock();;
        return messageLog;
    }

    /**
     * @Author: luo
     * @Description: 清除number以下的消息
     * @param number 操作序号
     * @Data: 10:50 2021/9/6
     */
    public void clearOldNumber(int number) {
        try {
            messageLogMapLock.writeLock().lock();
            List<Integer> keyList=new ArrayList<>();
            for (Integer key : messageLogMap.keySet()) {
                if (key < number && StringUtils.isNotNull(messageLogMap.get(number))) {
                    keyList.add(key);
                }
            }
            keyList.forEach(key->{ messageLogMap.remove(key);});
        } finally {
            messageLogMapLock.writeLock().unlock();
        }
    }
    /**
     * @Author: luo
     * @Description: 提取已进入commit阶段的(v,n)
     * @param number 操作序号
     * @param viewNumber 视图
     * @Data: 8:57 2021/9/7
     */
    public List<Message> getPrepared(int number, int viewNumber,int quorumSize){
        List<Message> messageList = new ArrayList<>();
        //读取（v,n）前先加上锁
        messageLogMapLock.readLock().lock();
        for(Integer key:messageLogMap.keySet()){
            if(key > number){
                Map<Integer, MessageLog> map  = messageLogMap.get(key);
                MessageLog messageLog = map.get(viewNumber);
                if(StringUtils.isNotNull(messageLog) && messageLog.isCommit()){
                    Message prePrePare = messageLog.getPrePrepare();
                    List<Message> prepareList= messageLog.getPrepareList();
                    if(prepareList.size() < quorumSize){
                        //quorumSize有可能发生变化，需要判断收集的prepare是否达到quorumSize个数
                        continue;
                    }
                    messageList.add(prePrePare);
                    messageList.addAll(prepareList);
                }
            }
        }
        messageLogMapLock.readLock().unlock();
        return messageList;
    }

    /**
     * @Author: luo
     * @Description: 收集那些已经进入commit阶段的number
     * @param preparedMap 已进入commit阶段的pre-prepare消息和prepare消息
     * @param newViewNumber 新的视图
     * @Data: 17:01 2021/9/8
     */
    public void upViewNumber(int oldViewNumber,int newViewNumber,int stableCheckpoint, Map<Integer,List<Message>>  preparedMap) {
        messageLogMapLock.writeLock().lock();
        int maxNumber= 0;
        Map<Integer,MessageLog> oldMessageLogMap=new HashMap(mapSize);
        for (Integer number : messageLogMap.keySet()) {
            if (stableCheckpoint >= number) {
                continue;
            }
            Map<Integer, MessageLog> map = messageLogMap.get(number);
            MessageLog messageLog = map.get(oldViewNumber);
            if (StringUtils.isNull(messageLog) || !messageLog.isReply()) {
                continue;
            }
            maxNumber= Math.max(maxNumber, messageLog.getNumber());
            oldMessageLogMap.put(messageLog.getNumber(), messageLog);
        }
        for(int number: preparedMap.keySet()){
            maxNumber= Math.max(maxNumber, number);
        }

        for(int point=stableCheckpoint+1;point<=maxNumber;point++){
            Map<Integer, MessageLog> map = messageLogMap.get(point);
            if(StringUtils.isNull(map)){
                map=new HashMap<>(mapSize);
                messageLogMap.put(point,map);
            }
            MessageLog oldMessageLog= oldMessageLogMap.get(point);
            if(StringUtils.isNotNull(oldMessageLog)){
                map.put(newViewNumber,oldMessageLog);
                continue;
            }
            List<Message> preparedList = preparedMap.get(point);
            if(StringUtils.isNotNull(preparedList)) {
                MessageLog messageLog = new MessageLog(preparedList);
                map.put(newViewNumber,messageLog);
            }
        }

        messageLogMapLock.writeLock().unlock();
    }
}
