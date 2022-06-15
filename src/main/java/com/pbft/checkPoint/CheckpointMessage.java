package com.pbft.checkPoint;

import com.common.StringUtils;
import com.pbft.common.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: luo
 * @Description:
 * @Data: 16:41 2021/9/17
 */
@Slf4j
public class CheckpointMessage {

    /**(c,i,m) c:checkpoint,i节点id,m消息*/
    private final int MAX_MESSAGE_COUNT = 16;
    private final ReentrantReadWriteLock messageLock;
    private final Map<Integer, Map<Integer, Message>> messageSet;

    CheckpointMessage(int window){
        messageSet = new HashMap<>(window);
        messageLock = new ReentrantReadWriteLock();
    }

    List<Message> getCheckpointMessageList(int checkpoint){
        try {
            messageLock.readLock().lock();
            return new ArrayList<>(messageSet.get(checkpoint).values());
        } finally {
            messageLock.readLock().unlock();
        }
    }

    public void addCheckpointMessageList(int checkpoint, List<Message> checkpointList){
        try {
            messageLock.writeLock().lock();
            Map<Integer, Message> checkpointMessageSet = getCheckpointMessageSet(checkpoint);
            checkpointList.forEach(msg -> checkpointMessageSet.put(msg.getOrderNumber(), msg));
        }finally {
            messageLock.writeLock().unlock();
        }
    }


    private Map<Integer,Message> getCheckpointMessageSet(int checkpoint){
        Map<Integer,Message> map= messageSet.get(checkpoint);
        if (StringUtils.isNull(map)) {
            map = new HashMap<>(MAX_MESSAGE_COUNT);
            messageSet.put(checkpoint, map);
        }
        return map;
    }

    public void addMessage( int checkpoint ,Message message) {
        try {
            messageLock.writeLock().lock();
            Map<Integer, Message> checkpointMessageSet = getCheckpointMessageSet(checkpoint);
            //TODO:状态机状态过滤
            checkpointMessageSet.put(message.getOrderNumber(), message);
        } finally {
            messageLock.writeLock().unlock();
        }
    }
    /**
     * @Author: luo
     * @Description: 获取最新一个可以达到下一个StableCheckpoint的点
     * @param quorumSize 法定人数
     * @Data: 15:38 2021/9/17
     */
    public int getNextStableCheckpoint(int quorumSize,int stableCheckpoint,int highWaterMark,int continuityPoint){
        try {
            messageLock.readLock().lock();
            int nextStableCheckpoint = stableCheckpoint;
            for (int checkpoint : this.messageSet.keySet()) {
                if (checkpoint <= stableCheckpoint) {
                    continue;
                }
                if (messageSet.get(checkpoint).size() < quorumSize) {
                    log.debug("当前checkpoint:{}收到的message数量为{},不足{},",checkpoint,messageSet.get(checkpoint).size(),quorumSize);
                    continue;
                }
                if (checkpoint > highWaterMark) {
                    log.warn("当前处于落后状态，节点收到的下一个checkpoint大于当前水线最高位,stableCheckpoint:{},checkpoint:{}",stableCheckpoint,checkpoint);
                    continue;
                }
                boolean isReady = true;
                if(checkpoint > continuityPoint){
                    log.info("节点尚有未完成共识请求，拒绝升级checkpoint:{}为stableCheckpoint", checkpoint);
                    isReady = false;
                }
                if (isReady && nextStableCheckpoint < checkpoint) {
                    nextStableCheckpoint = checkpoint;
                }
            }
            return nextStableCheckpoint;
        }finally {
            messageLock.readLock().unlock();
        }
    }

    public void clear(int stableCheckpoint){
        List<Integer> deleteList = new ArrayList<>(MAX_MESSAGE_COUNT);
        try {
            messageLock.writeLock().lock();
            for (Integer number : this.messageSet.keySet()) {
                if (number < stableCheckpoint) {
                    deleteList.add(number);
                }
            }
            deleteList.forEach(this.messageSet::remove);
        }finally {
            messageLock.writeLock().unlock();
        }
    }
}
