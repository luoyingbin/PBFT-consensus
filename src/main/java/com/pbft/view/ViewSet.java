package com.pbft.view;

import com.common.StringUtils;
import com.pbft.common.model.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @Author: luo
 * @Description: （v,n）管理器
 * @Data: 9:14 2021/9/2
 */
public class ViewSet {

    /**消息列表  key:ViewNumber value:number   消息队列 */
    int mapSize = 100;
    private final ReentrantReadWriteLock viewLogMapLock;
    private final Map<Integer, Message>  viewChangeMap;
    /**稳定检查点统计*/
    private final ReentrantReadWriteLock checkPointNumberLock;
    private final List<Message>  checkpointList;
    private int stableCheckpoint;
    private String statemachine;

    private final ReentrantReadWriteLock preparedMapLock;
    private final Map<Integer,List<Message>>  preparedMap;

    private final ReentrantReadWriteLock endTimeLock;
    private long endTime;

    private final int viewNumber;

    public ViewSet(int viewNumber) {
        viewLogMapLock = new ReentrantReadWriteLock();
        viewChangeMap = new HashMap<>(mapSize);
        preparedMap = new HashMap<>(mapSize);
        endTimeLock = new ReentrantReadWriteLock();
        preparedMapLock =new ReentrantReadWriteLock();
        checkpointList =new ArrayList<>(mapSize);
        checkPointNumberLock =new ReentrantReadWriteLock();
        endTime = 0 ;
        stableCheckpoint = 1;
        this.viewNumber = viewNumber;
        this.statemachine="";
    }

    public boolean tryEnd(long time) {
        try {
            endTimeLock.writeLock().lock();
            if ( endTime != 0) {
                return false;
            }
            endTime = time;
           return true;
        }finally {
            endTimeLock.writeLock().unlock();
        }
    }

    public void addPrePared(Map<Integer,List<Message>> preparedMap){
        try {
            endTimeLock.readLock().lock();
            if ( endTime != 0) {
                return ;
            }
            preparedMapLock.writeLock().lock();
            for (Integer number : preparedMap.keySet()) {
                if (StringUtils.isNotNull(this.preparedMap.get(number))) {
                    //已有该number的prepared，无需再添加
                    continue;
                }
                this.preparedMap.put(number, preparedMap.get(number));
            }
            preparedMapLock.writeLock().unlock();
        }finally {
            endTimeLock.readLock().unlock();
        }

    }


    public void addViewChange(Message message) {
        try {
            endTimeLock.readLock().lock();
            if ( endTime != 0) {
                return ;
            }
            int orderNumber = message.getOrderNumber();
            int viewNumber = message.getViewNumber();
            viewLogMapLock.writeLock().lock();
            if (viewNumber == this.viewNumber && StringUtils.isNull(viewChangeMap.get(orderNumber))) {
                //无需保存viewChange中携带checkpoint消息和prepared消息
                message.setValue("");
                viewChangeMap.put(orderNumber, message);
            }
            viewLogMapLock.writeLock().unlock();
        }finally {
            endTimeLock.readLock().unlock();
        }
    }


    public void addCheckpointMessage(List<Message> checkpointList) {
        int newStableCheckpoint = checkpointList.get(0).getNumber();
        if(checkpointList.size() ==0 || newStableCheckpoint < this.stableCheckpoint){
            return;
        }
        try {
            endTimeLock.readLock().lock();
            checkPointNumberLock.writeLock().lock();
            if ( endTime != 0) {
                return ;
            }
            if (newStableCheckpoint > this.stableCheckpoint) {
                this.stableCheckpoint = newStableCheckpoint;
                this.statemachine = checkpointList.get(0).getDigest();
                this.checkpointList.clear();
                this.checkpointList.addAll(checkpointList);
            }
        } finally {
            checkPointNumberLock.writeLock().unlock();
            endTimeLock.readLock().unlock();
        }
    }

    public String getStatemachine() {
        return statemachine;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public int getViewChangeCount(){
        int size=-1;
        viewLogMapLock.readLock().lock();
        size = viewChangeMap.size();
        viewLogMapLock.readLock().unlock();
        return size;
    }

    public List<Message> getViewChangeList(){
        viewLogMapLock.readLock().lock();
        List<Message> messageList = viewChangeMap.values().stream().collect(Collectors.toList());
        viewLogMapLock.readLock().unlock();
        return messageList;
    }

    public List<Message> getCheckpointList(){
        try {
            checkPointNumberLock.readLock().lock();
            return new ArrayList<>(this.checkpointList);
        }finally {
            checkPointNumberLock.readLock().unlock();
        }
    }

    public int getStableCheckpoint() {
        try {
            checkPointNumberLock.readLock().lock();
            return stableCheckpoint;
        } finally {
            checkPointNumberLock.readLock().unlock();
        }
    }

    public List<Message> getPreparedList(){
        int stableCheckpoint =getStableCheckpoint();
        preparedMapLock.readLock().lock();
        List<Message> messageList= new ArrayList<>();
        //TODO:应该进行过滤，小于stableCheckpoint的放弃
        preparedMap.values().stream().filter(msgList-> msgList.get(0).getNumber()>=stableCheckpoint).forEach(messageList::addAll);
        preparedMapLock.readLock().unlock();
        return messageList;
    }
}
