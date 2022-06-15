package com.pbft.checkPoint;

import com.common.StringUtils;
import com.db.DbManager;
import com.pbft.Reply;
import com.pbft.common.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: luo
 * @Description:
 * @Data: 11:46 2021/9/10
 */
@Slf4j
public class CheckpointManager {
    private static CheckpointManager checkPointManager;
    public static CheckpointManager getProfile() {
        if (StringUtils.isNull(checkPointManager)) {
            checkPointManager = new CheckpointManager();
        }
        return checkPointManager;
    }

    public static final int START_STABLE_CHECKPOINT = 0;
    /**checkPoint检查点:允许同时进行的共识操作数量为window */
    private static final int cycle =5;
    private static final int multiple=2;
    private static final int window= cycle*multiple;
    /**最新稳定检查点*/
    private final ReentrantReadWriteLock checkpointLock;
    int stableCheckpoint;
    /**
     * 水线（water mark）
     * lowWaterMark = 最新stableCheckpoint
     * highWaterMark = lowWaterMark+ window-1
     * */
    private int lowWaterMark;
    private int highWaterMark;
    /** 已经完成共识的操作编号 */
    private final Set<Integer> sendBackSet;
    /** 已经使用但还没完成共识的编号 */
    private final  Set<Integer> borrowSet;

    CheckpointMessage checkpointMessage;

    Reply reply;

    private CheckpointManager() {
        List<Message> checkpointMsgList = DbManager.loadStableCheckpoint();
        checkpointMessage = new CheckpointMessage(window);

        this.stableCheckpoint = START_STABLE_CHECKPOINT;
        if (StringUtils.isNotNull(checkpointMsgList)) {
            this.stableCheckpoint = checkpointMsgList.get(0).getNumber();
            checkpointMessage.addCheckpointMessageList(stableCheckpoint, checkpointMsgList);
        }
        this.lowWaterMark = this.stableCheckpoint + 1;
        highWaterMark = this.stableCheckpoint + window;
        this.sendBackSet = new HashSet<>(window);

        List<Integer> sendBackSet = DbManager.loadCommitNumber(stableCheckpoint+1, stableCheckpoint + window);
        this.sendBackSet.addAll(sendBackSet);

        checkpointLock = new ReentrantReadWriteLock();
        borrowSet = new HashSet<>(window);
        reply=Reply.getProfile();
    }
    /**操作请求序号分配*/
    public int borrowNumber() {
        checkpointLock.writeLock().lock();
        try {
            int number = this.lowWaterMark;
            while (borrowSet.contains(number) || sendBackSet.contains(number)) {
                number++;
                if (number > this.highWaterMark) {
                    //没有待使用序号
                    return -1;
                }
            }
            borrowSet.add(number);
            return number;
        } finally {
            checkpointLock.writeLock().unlock();
        }
    }
    /**操作请求序号归还*/
    public boolean sendBackNumber(int number) {
        try {
            checkpointLock.writeLock().lock();
            borrowSet.remove(number);
            if (sendBackSet.contains(number)) {
                log.debug("操作序号已归还,重复归还，序号:{}",number);
                return false;
            }

            sendBackSet.add(number);
            return true;
        } finally {
            checkpointLock.writeLock().unlock();
        }
    }
    /**确认是否还有未归还的共识序号*/
    public boolean hasBorrow(){
        return borrowSet.size()!=0;
    }
    /**
     * @Author: luo
     * @Description: 尝试获取下一个检查点进行共识 下一个检查点必须是stableCheckpoint+cycle-1
     * 触发tryGetNextCheckpoint条件：共识操作完成，tryGetNextCheckpoint成功
     * 成功：返回下一个检查点序号
     * 失败：返回-1
     * @Data: 12:59 2021/9/10
     */
    public int tryGetCheckpoint(){
        checkpointLock.readLock().lock();
        try {
            int checkpoint = this.lowWaterMark + cycle -1;
            if(!checkCyclePoint(checkpoint)){
                return -1;
            }
            return checkpoint;
        }finally {
            checkpointLock.readLock().unlock();
        }
    }
    /**
     * @Author: luo
     * @Description: 获取最新检查点统一确认的checkpoint消息集合
     * @Data: 16:10 2021/9/10
     */
    public List<Message> getStableCheckpointMessageList() {
        try {
            checkpointLock.readLock().lock();
            if (stableCheckpoint == CheckpointManager.START_STABLE_CHECKPOINT) {
                return new ArrayList<>();
            }
            return checkpointMessage.getCheckpointMessageList(stableCheckpoint);
        } finally {
            checkpointLock.readLock().unlock();
        }
    }
    /**
     * @Author: luo
     * @Description: newView专用,接收checkpoint消息集合消息，尝试更新stableCheckpoint
     * @param checkpointList newView里checkpoint消息集合，满足：所有消息都是相同checkpoint，状态机状态相同（digest字段）
     * @param usedNumberSet newView里prepared集合中已使用的number集合
     * @Data: 15:33 2021/9/11
     */
    public boolean tryUpStableCheckpointAndBorrowSet(List<Message> checkpointList,Set<Integer> usedNumberSet) {
        try {
            this.checkpointLock.writeLock().lock();
            for (Integer usedNumber : usedNumberSet) {
                if (usedNumber < this.lowWaterMark || this.sendBackSet.contains(usedNumber) || this.borrowSet.contains(usedNumber)) {
                    continue;
                }
                log.debug("操作序号：{}进入prepared阶段", usedNumber);
                this.borrowSet.add(usedNumber);
            }

            if (checkpointList.size() == 0) {
                //当前newView中stableCheckpoint还是1
                return false;
            }
            int checkpoint = checkpointList.get(0).getNumber();
            if (checkpoint <= this.stableCheckpoint) {
                return false;
            }
            checkpointMessage.addCheckpointMessageList(checkpoint, checkpointList);
            if (!checkCyclePoint(checkpoint)) {
                return false;
            }

            this.stableCheckpoint = checkpoint;
            this.lowWaterMark = this.stableCheckpoint + 1;
            this.highWaterMark = this.stableCheckpoint + window;
            DbManager.saveDBStableCheckpoint(checkpointList);
            clear();
            return true;
        } finally {
            checkpointLock.writeLock().unlock();
        }
    }

    public boolean readyUpStableCheckpoint(int quorumSize){
        try {
            checkpointLock.readLock().lock();
            int continuityPoint= reply.getLastReplyNumber();
            int nextStableCheckpoint = checkpointMessage.getNextStableCheckpoint(quorumSize,stableCheckpoint,highWaterMark,continuityPoint);
            return nextStableCheckpoint != this.stableCheckpoint;
        }finally {
            checkpointLock.readLock().unlock();
        }
    }
    /**
     * @Author: luo
     * @Description:
     * 更新稳定检查点,需要满足两个条件
     * 1、收到不同节点checkpoint达到2f+1
     * 2、自身操作请求到checkpoint这一点均已完成共识完成replay
     * 当条件都满足修stableCheckpoint值为checkpoint
     * 更新水线（water mark）和缓存队列
     * @Data: 13:02 2021/9/10
     */
    public boolean upStableCheckpoint(int quorumSize) {
        try {
            checkpointLock.writeLock().lock();
            int continuityPoint = reply.getLastReplyNumber();
            int nextStableCheckpoint = checkpointMessage.getNextStableCheckpoint(quorumSize, stableCheckpoint, highWaterMark, continuityPoint);
            if (nextStableCheckpoint == this.stableCheckpoint) {
                return false;
            }

            this.stableCheckpoint = nextStableCheckpoint;
            this.lowWaterMark = this.stableCheckpoint + 1;
            this.highWaterMark =this.stableCheckpoint + window;
            DbManager.saveDBStableCheckpoint(checkpointMessage.getCheckpointMessageList(stableCheckpoint));
            clear();
            return true;
        } finally {
            checkpointLock.writeLock().unlock();
        }
    }

    public int getStableCheckpoint(){
        checkpointLock.readLock().lock();
        try {
            return stableCheckpoint;
        }finally {
            checkpointLock.readLock().unlock();
        }
    }

    public void addMessage(Message message) {
        int checkpoint = message.getNumber();
        if (checkpoint <= getStableCheckpoint()) {
            log.debug("收到checkpoint小于当前stableCheckpoint,拒绝");
            return;
        }
        if (checkpoint % cycle != 0) {
            log.debug("收到的checkPoint不是k的整数倍,拒绝");
            return;
        }
        checkpointMessage.addMessage(checkpoint,message);
    }

    /**
     * @Author: luo
     * @Description: 旧消息清理工作
     * @Data: 15:45 2021/9/17
     */
    private void clear(){
        List<Integer> deleteList = new ArrayList<>(window);
        for (Integer number : borrowSet) {
            if (number < this.stableCheckpoint) {
                deleteList.add(number);
            }
        }
        deleteList.forEach(borrowSet::remove);
        deleteList.clear();
        for (Integer number : sendBackSet) {
            if (number < stableCheckpoint) {
                deleteList.add(number);
            }
        }
        deleteList.forEach(sendBackSet::remove);
        deleteList.clear();

        checkpointMessage.clear(stableCheckpoint);
    }

    public boolean hasSendBackSet(int number){
        try {
            checkpointLock.readLock().lock();
            return borrowSet.contains(number);
        }finally {
            checkpointLock.readLock().unlock();
        }
    }

    private boolean checkCyclePoint(int checkpoint){
        for (int i = this.lowWaterMark; i <= checkpoint; i++) {
            if (!this.sendBackSet.contains(i)) {
                //下一个检查点过程中如果由尚未完成共识的point,下一个检查点尚未准备完毕
                return false;
            }
        }
        return true;
    }

    public int getLastContinuityPoint(){
        try {
            checkpointLock.readLock().lock();
            for (int i = lowWaterMark; i <= highWaterMark; i++) {
                if (!sendBackSet.contains(i)) {
                    //下一个检查点过程中如果由尚未完成共识的point,下一个检查点尚未准备完毕
                    return i - 1;
                }
            }
            return highWaterMark;
        }finally {
            checkpointLock.readLock().unlock();
        }
    }
}
