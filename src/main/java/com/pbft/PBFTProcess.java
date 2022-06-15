package com.pbft;
import com.alibaba.fastjson.JSONObject;
import com.common.StringUtils;
import com.db.DbManager;
import com.pbft.checkPoint.Checkpoint;
import com.pbft.checkPoint.CheckpointManager;
import com.pbft.common.MessageUtils;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import com.pbft.consensus.Commit;
import com.pbft.consensus.PrePrepare;
import com.pbft.consensus.Prepare;
import com.pbft.consensus.Status;
import com.pbft.message.MessageLog;
import com.pbft.message.MessageManager;
import com.pbft.view.ViewChange;
import com.pbft.view.ViewManager;
import com.pbft.view.ViewUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: luo
 * @Description: PBFT消息处理器 要拆
 * v: 视图
 * n: 操作编号 一个编号对应一个value
 * value: 具体操作内容（可以是交易集，也可以是别的）
 */
@Slf4j
public class PBFTProcess {

    /**
     * 消息管理器
     */
    MessageManager messageManager;
    /**
     * 共识节点列表
     */
    static ValidatorManager validatorManager;
    /**
     * 视图管理器
     */
    ViewManager viewManager;
    /**
     * 操作序号管理器
     */
    CheckpointManager checkPointManager;
    /**
     * 执行器
     * */
    Reply reply;
    /**
     * 共识服务开关
     * 启动时会接收并处理pre-prepared消息、prepare消息、commit消息、viewChange消息，checkpoint消息
     * 暂停时只会接收并处理viewChange消息、checkpoint消息、newView消息
     * 关闭的触发条件包括：发起viewChang、处理newView消息
     * 启动的触发条件：第一次初始化启动，newView消息处理完毕
     */
    private final ReentrantReadWriteLock isActiveLock;
    boolean isActive;


    /**
     * 接收共识请求开关
     */
    private final ReentrantReadWriteLock isUpgradeLock;
    boolean isUpgrade;

    public PBFTProcess(int port) {
        validatorManager = ValidatorManager.getProfile();
        validatorManager.init(port);
        messageManager = MessageManager.getProfile();
        viewManager = ViewManager.getProfile();
        checkPointManager = CheckpointManager.getProfile();
        reply = Reply.getProfile();
        reply.setValidatorManager(validatorManager);
        isActive = true;
        isUpgrade = false;
        isActiveLock = new ReentrantReadWriteLock();
        isUpgradeLock = new ReentrantReadWriteLock();
        log.debug("节点加载完毕,当前节点视图:{},稳定检查点:{},当前已执行最新的共识操作{}", viewManager.getViewNumber(), checkPointManager.getStableCheckpoint(), reply.getLastReplyNumber());
    }

    public boolean request(String value,int upgrade) {
        try {
            isActiveLock.readLock().lock();
            if (!isActive) {
                log.debug("正在发起viewChange，禁止发起共识请求");
                return false;
            }
            int viewNumber = viewManager.getViewNumber();
            if (!validatorManager.isLeader(viewNumber)) {
                log.debug("当前节点不是leader，禁止发起共识请求");
                return false;
            }
            int newNumber = checkPointManager.borrowNumber();
            if (newNumber < 0) {
                log.debug("当前正在进行共识操作数量已达上限，暂时无法发起新共识操作请求");
                return false;
            }
            Message message= null;
            if(upgrade == 0) {
                message = PrePrepare.newPrePrepare(viewNumber, newNumber, value);
            }else{
                message = PrePrepare.upgrade(viewNumber, newNumber, value,upgrade);
            }
            log.debug("发送共识请求:n:{},v:{},value:{}",newNumber,viewNumber,value);
            ConsensusManager.broadMessage(message);
            return true;
        } finally {
            isActiveLock.readLock().unlock();
        }
    }

    public void receiveMessage(Message message) {
        if (MessageType.CHECK_POINT.equals(message.getMessageType())) {
            log.debug("checkpoint消息处理");
            if (Checkpoint.onCheckPoint(message, validatorManager, checkPointManager)) {
                messageManager.clearOldNumber(checkPointManager.getStableCheckpoint());
            }
            return;
        }
        if (MessageType.VIEW_CHANGE.equals(message.getMessageType())) {
            log.debug("view_change处理");
            ViewChange.onViewChange(message, validatorManager, viewManager);
            return;
        }
        if (MessageType.NEW_VIEW.equals(message.getMessageType())) {
            log.debug("new_view_change处理");
            onNewView(message);
            return;
        }
        try {
            isActiveLock.readLock().lock();
            if (!isActive) {
                //正在进行view change，停止接收消息
                // TODO：可以先进入缓存区 ,newView结束后再次发送处理
                return;
            }
            if (message.getViewNumber() != viewManager.getViewNumber()) {
                log.debug("消息视图编号不正确");
                return;
            }
            if (message.getNumber() <= checkPointManager.getStableCheckpoint() && message.getNumber() != CheckpointManager.START_STABLE_CHECKPOINT) {
                log.debug("消息的操作序号过低");
                return;
            }
            //获取(v,n）消息队列
            MessageLog messageLog = messageManager.getMessageLog(message.getViewNumber(), message.getNumber());
            //PRE_PREPARE、PREPARE、COMMIT类型消息，暂时塞到(v,n)消息队列等待后续处理
            messageLog.addMessage(message);
            //读取(v,n)消息队列中消息内容
            processMessageLog(messageLog);
        } finally {
            isActiveLock.readLock().unlock();
        }

    }

    public void startViewChange() {
        //进入view change状态,停止接收除了checkpoint，view-change和new view-change以外的请求
        //TODO：有可能线程饿死
        isActiveLock.writeLock().lock();
        isActive = false;
        int viewNumber = viewManager.getViewNumber();
        int nextViewNumber = viewManager.getNextViewNumber();
        isActiveLock.writeLock().unlock();
        //获取最后稳定检查点消息集合
        List<Message> checkpointMsgList = checkPointManager.getStableCheckpointMessageList();
        //应该收集进入commit阶段(prepared)的的pre-prepare消息和prepare消息
        int stableCheckpoint = CheckpointManager.START_STABLE_CHECKPOINT;
        if (checkpointMsgList.size() > 0) {
            stableCheckpoint = checkpointMsgList.get(0).getNumber();
        }
        //获得stableCheckpoint之后操作请求并达到prepared的消息集合
        int quorumSize = validatorManager.getQuorumSize();
        List<Message> preparedMsgList = messageManager.getPrepared(stableCheckpoint, viewNumber, quorumSize);
        List<Message> messageList = new ArrayList<>();
        messageList.addAll(preparedMsgList);
        messageList.addAll(checkpointMsgList);
        //（v,n,C,P）
        // v:下一个视图编号
        // n:节点最后一个稳定检查点（stableCheckpoint）
        // C:是经过2f+1节点确认stableCheckpoint的消息集合
        // P:是n之后的进入commit阶段的消息集合
        Message message = ViewChange.newViewChange(nextViewNumber, stableCheckpoint, messageList);
        log.debug("发起viewChange，当前view:{},nexView:{},stableCheckpoint:{}", viewNumber, nextViewNumber, stableCheckpoint);
        ConsensusManager.broadMessage(message);
    }

    public void sendStatus(int index) {
        Message statusMessage = Status.newStatus(viewManager.getViewNumber(), checkPointManager.getStableCheckpoint(), validatorManager.getList());
        log.debug("收到节点:{}状态请求,返回状态信息给目标,viewNumber:{},number:{},hash:{}", index, statusMessage.getViewNumber(), statusMessage.getNumber(), statusMessage.getDigest());
        ConsensusManager.sendMessage(index, statusMessage);
    }

    private void onNewView(Message message) {
        int newViewNumber = message.getViewNumber();
        if (viewManager.getViewNumber() >= newViewNumber) {
            log.debug("newView视图编号:{}过低,当前view:{},抛弃消息",newViewNumber,viewManager.getViewNumber());
            return;
        }
        //验证newView中每一条viewChain和pre-prepare和prepare和checkpoint数据合法性
        List<Message> messageList = JSONObject.parseArray(message.getValue(), Message.class);
        //基础验证
        if (!messageList.stream().allMatch(ConsensusManager::baseVerify)) {
            return;
        }

        int quorumSize = validatorManager.getQuorumSize();

        List<Message> viewChangeList = ViewUtils.getViewChangeList(messageList, newViewNumber, quorumSize);
        if (StringUtils.isNull(viewChangeList)) {
            log.debug("newView中viewChange校验失败");
            return;
        }
        List<Message> checkpointList = ViewUtils.getCheckpointList(messageList, message.getNumber(), quorumSize);
        if (StringUtils.isNull(checkpointList)) {
            log.debug("newView中checkpoint校验失败");
            return;
        }
        Map<Integer, List<Message>> preparedMap = ViewUtils.getPreparedMap(messageList, quorumSize);
        if (StringUtils.isNull(preparedMap)) {
            log.debug("newView中prepared校验失败");
            return;
        }

        /**
         * newView阶段
         * 停止共识（messageManager停止接收并处理pre-prepare、prepare、commit）、新消息线程挂起
         * 尝试更换更高的view视图，更换成功主要有以下动作：（更新失败条件：新视图序号小于等于当前节点视图）
         * 尝试更新stableCheckpoint（更新失败条件:当前节点请求序号到新stableCheckpoint尚有未完成point，新stableCheckpoint小于等于当前节点stableCheckpoint，）
         * 更新checkPointManager中已使用number集合（集合最小number比stableCheckpoint值大）
         * messageManager旧视图中已经共识完成的（v,n）中消息迁移到(v+1,n) ？防止恶意节点发送(v+1,n)消息占领(v+1,n)消息体？
         * messageManager加入newView中prepared消息到(v+1)视图中
         * 开启共识，恢复正常流程
         */
        boolean repeat = false;
        isActiveLock.writeLock().lock();
        isActive = false;
        int oldViewNumber = viewManager.getViewNumber();
        if (viewManager.tryUpViewNumber(newViewNumber)) {
            //一个视图只能进来一次，同时newView不允许多个视图同时进行
            checkPointManager.tryUpStableCheckpointAndBorrowSet(checkpointList, preparedMap.keySet());
            int stableCheckpoint = checkPointManager.getStableCheckpoint();
            messageManager.upViewNumber(oldViewNumber, newViewNumber, stableCheckpoint, preparedMap);
            log.debug("切换视图,旧视图:{},新视图:{},稳定检查点:{}", oldViewNumber, newViewNumber, stableCheckpoint);

            //收到onNewView可能会造成Upgrade共识请求丢失，但如果已进入commit阶段的Upgrade不会丢失
            int lastContinuityPoint = reply.getLastReplyNumber();
            MessageLog messageLog = messageManager.getMessageLog(newViewNumber, lastContinuityPoint + 1);
            Message prePrepareMsg = messageLog.getPrePrepare();
            if (StringUtils.isNull(prePrepareMsg) || prePrepareMsg.getUpgrade() == 0) {
                //当前节点最后一个（连续）检查点后下一个共识请求如果不是Upgrade就关闭共识升级
                closeUpgrade();
            }
            if(isLeader()){
                //启动交易池，轮流发送交易
                ConsensusManager.openConsensus();
            }
            repeat = true;
        }
        isActive = true;
        isActiveLock.writeLock().unlock();

        if (repeat) {
            for (Integer number : preparedMap.keySet()) {
                MessageLog messageLog = messageManager.getMessageLog(newViewNumber, number);
                Message msg = MessageUtils.newMessage(MessageType.COMMIT, messageLog.getPrePrepare());
                ConsensusManager.broadMessage(msg);
            }
        }
    }

    /**
     * PrePrepare消息处理前节点升级检查
     * */
    public boolean receivePrePrepare(MessageLog messageLog, Message message) {
        boolean result = false;
        isUpgradeLock.readLock().lock();
        if (isUpgrade) {
            log.warn("共识服务正在升级，拒绝接收prePrepare消息");
            isUpgradeLock.readLock().unlock();
            return false;
        }
        if (message.getUpgrade() != 0) {
            isUpgradeLock.readLock().unlock();
            try {
                isUpgradeLock.writeLock().lock();
                if (!isUpgrade) {
                    if("".equals(message.getValue())){
                        log.warn("升级共识节点共识消息value必须存地址");
                        return false;
                    }
                    //判定当前特殊共识序号是否是最后一个（连续）检查点的下一个，并且后续无任何共识操作过程
                    int lastContinuityPoint = reply.getLastReplyNumber();
                    if (lastContinuityPoint + 1 != message.getNumber()) {
                        log.warn("无法处理升级共识请求,当前节点最后一个（连续）检查点:{},升级共识请求操作序号:{}", lastContinuityPoint, message.getNumber());
                        return false;
                    }
                    if (!messageManager.hasConsensus(lastContinuityPoint)) {
                        log.warn("无法处理升级共识请求,当前节点最后一个（连续）检查点:{}后仍有共识请求", lastContinuityPoint);
                        return false;
                    }
                    result = PrePrepare.on(messageLog, message, validatorManager);
                    if(result){
                        //停止接收新的共识,进入升级状态
                        isUpgrade = true;
                        log.debug("共识节点升级请求,v:{},n:{},命令:{},节点地址：{}",message.getViewNumber(),message.getNumber(),message.getUpgrade(),message.getValue());
                    }
                }
            }finally {
                //锁降级，在释放写锁前获取读锁
                isUpgradeLock.writeLock().unlock();
            }
            return result;
        }
        result = PrePrepare.on(messageLog, message, validatorManager);
        isUpgradeLock.readLock().unlock();
        return result;
    }

    public void closeUpgrade(){
        isUpgradeLock.writeLock().lock();
        isUpgrade = false;
        isUpgradeLock.writeLock().unlock();
    }

    private void processMessageLog(MessageLog messageLog) {
        //只会读取一个阶段（PRE_PREPARE、PREPARE、COMMIT）的消息
        Message message = messageLog.red();
        //只要(v,n)队列还有没处理完的消息内容，循环读取
        boolean result = true;
        while (StringUtils.isNotNull(message)) {
            switch (message.getMessageType()) {
                case PRE_PREPARE: {
                    //平时使用读锁，判定状态是否能接收PrePrepare消息
                    //如果收到特殊共识,使用读锁，修改状态拒绝接收PrePrepare消息
                    //判定当前特殊共识序号是否是checkpoint下一个，并且后续无任何共识操作过程
                    result = receivePrePrepare(messageLog, message);
                    break;
                }
                case PREPARE: {
                    result = Prepare.on(messageLog, message, validatorManager);
                    break;
                }
                case COMMIT: {
                    //如果特殊共识完成:更新节点数量，使用写锁，修改状态允许继续接收
                    result = Commit.on(messageLog, message, validatorManager);
                    if (result) {
                        log.debug("共识完成,v:{},n:{},value:{}", messageLog.getViewNumber(), messageLog.getNumber(), messageLog.getPrePrepare().getValue());
                        if (checkPointManager.sendBackNumber(messageLog.getNumber())) {
                            List<Message> msgList = messageLog.getCommitList();
                            msgList.add(messageLog.getPrePrepare());
                            DbManager.saveCommitMsg(messageLog.getNumber(), msgList);
                            reply(checkPointManager.getLastContinuityPoint(),messageLog);
                            //当checkpoint达到2f+1,提升为stableCheckpoint,并执行日志清理工作
                            if (checkPointManager.upStableCheckpoint(validatorManager.getQuorumSize())) {
                                log.debug("stableCheckpoint更新成功，最新稳定检查点:{}", checkPointManager.getStableCheckpoint());

                                //删除stableCheckpoint之前的历史消息
                                messageManager.clearOldNumber(checkPointManager.getStableCheckpoint());
                            }
                            Checkpoint.trySendNextCheckpoint(checkPointManager);
                        }
                    }
                    break;
                }
                default: {
                    break;
                }
            }
            message = messageLog.red();
        }
    }

    public boolean inConsensus(){
        return checkPointManager.hasBorrow();
    }

    private void reply(int lastContinuityPoint,MessageLog messageLog) {
        Message msg= messageLog.getPrePrepare();
        reply.replyNumber(lastContinuityPoint,msg.getNumber(),msg);
    }

    public boolean isLeader() {
        return validatorManager.isLeader(viewManager.getViewNumber());
    }
}
