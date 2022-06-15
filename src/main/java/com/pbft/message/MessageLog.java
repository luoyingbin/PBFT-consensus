package com.pbft.message;


import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @Author: luo
 * @Description: PBFT消息管理器,一个(v,n)对应一个对象
 * @Data: 15:50 2021/8/30
 */
public class MessageLog {

    /** 消息阶段*/
    MessageType phase;
    /**视图编号*/
    int viewNumber;
    /**编号*/
    int number;

    public int getNumber() {
        return number;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    private final ReentrantReadWriteLock messageMapLock;
    Map<MessageType, List<Message>> messageMap;

    private final ReentrantLock messageListPointerLock;
    int messagePointer;

    private final ReentrantReadWriteLock prepareMapLock;
    Map<Integer,Message> prepareMap;

    private final ReentrantReadWriteLock prePrepareLock;
    private Message prePrepare;

    public boolean getCheckValue() {
        return checkValue;
    }

    private boolean checkValue;

    public void setPrePrepare(Message prePrepare) {
        prePrepareLock.writeLock().lock();
        this.prePrepare = prePrepare;
        prePrepareLock.writeLock().unlock();
    }

    public Message getPrePrepare() {
        try {
            prePrepareLock.readLock().lock();
            return prePrepare;
        }finally {
            prePrepareLock.readLock().unlock();
        }
    }

    boolean checkNumber(int number) {
        return this.number == number;
    }

    public void addPrepare(Message message){
        try {
            prepareMapLock.writeLock().lock();
            prepareMap.put(message.getOrderNumber(),message);
        }finally {
            prepareMapLock.writeLock().unlock();
        }
    }

    public int getPrepareSize(){
        try {
            prepareMapLock.readLock().lock();
       return prepareMap.size();
        }finally {
            prepareMapLock.readLock().unlock();
        }
    }

    public List<Message> getPrepareList(){
        try {
            prepareMapLock.readLock().lock();
            List<Message> messageList =prepareMap.values().stream().collect(Collectors.toList());
            return messageList;
        }finally {
            prepareMapLock.readLock().unlock();
        }
    }

    private final ReentrantReadWriteLock commitMapLock;
    Map<Integer,Message> commitMap;

    public void addCommit(Message message) {
        if(!checkNumber(message.getNumber())){
            //操作序号对不上，拒绝接收该消息
            return;
        }
        try {
            commitMapLock.writeLock().lock();
            commitMap.put(message.getOrderNumber(), message);
        } finally {
            commitMapLock.writeLock().unlock();
        }
    }

    public int getCommitSize(){
        try {
            commitMapLock.readLock().lock();
        return commitMap.size();
        } finally {
            commitMapLock.readLock().unlock();
        }
    }

    public List<Message> getCommitList(){
        try {
            commitMapLock.readLock().lock();
            List<Message> messageList = new ArrayList<>(commitMap.values());
            return messageList;
        }finally {
            commitMapLock.readLock().unlock();
        }
    }

    public MessageLog(int number,int viewNumber){
        //每种类型消息最大长度
        int messageMaxCount = 100;
        this.number=number;
        this.viewNumber=viewNumber;
        this.phase =MessageType.PRE_PREPARE;
        this.messagePointer = 0;
        this.prepareMap =new HashMap<>(messageMaxCount);
        this.commitMap =new HashMap<>(messageMaxCount);
        this.messageMap=new HashMap<>(MessageType.values().length);
        for(MessageType messageType:MessageType.values()){
            messageMap.put(messageType,new ArrayList<>(messageMaxCount));
        }
        this.messageMapLock =new ReentrantReadWriteLock();
        this.messageListPointerLock=new ReentrantLock();
        this.prepareMapLock=new ReentrantReadWriteLock();
        this.commitMapLock=new ReentrantReadWriteLock();
        this.prePrepareLock =new ReentrantReadWriteLock();
        this.checkValue=false;
    }

    public MessageLog(List<Message> preparedList){
        int messageMaxCount = 100;
        this.phase =MessageType.COMMIT;
        this.messagePointer = 0;
        this.prepareMap =new HashMap<>(messageMaxCount);
        this.commitMap =new HashMap<>(messageMaxCount);
        this.messageMap=new HashMap<>(MessageType.values().length);
        for(MessageType messageType:MessageType.values()){
            messageMap.put(messageType,new ArrayList<>(messageMaxCount));
        }
        this.messageMapLock =new ReentrantReadWriteLock();
        this.messageListPointerLock=new ReentrantLock();
        this.prepareMapLock=new ReentrantReadWriteLock();
        this.commitMapLock=new ReentrantReadWriteLock();
        this.prePrepareLock =new ReentrantReadWriteLock();
        for(Message msg:preparedList){
            if(MessageType.PRE_PREPARE.equals( msg.getMessageType())){
                this.prePrepare = msg;
            }else{
                prepareMap.put(msg.getOrderNumber(),msg);
            }
            messageMap.get(msg.getMessageType()).add(msg);
        }
        this.number= this.prePrepare.getNumber();
        this.viewNumber= this.prePrepare.getViewNumber();

    }

    /**
     * @Author: luo
     * @Description: 记录消息根据消息类型进入不同队列
     * @param message 待进入队列的消息
     * @Data: 9:10 2021/9/2
     */
    public void addMessage(Message message) {
        List<Message> messageList = messageMap.get(message.getMessageType());
        //ArrayList 不保证线程安全，这里加以保护
        try {
            messageMapLock.writeLock().lock();
            messageList.add(message);
        } finally {
            messageMapLock.writeLock().unlock();
        }
    }
    /**
     * @Author: luo
     * @Description: 弹出消息，根据当前消息处理阶段弹出响应类型消息
     * @Data: 9:10 2021/9/2
     */
    public Message red() {
        try {
            messageListPointerLock.lock();
            List<Message> messageList = null;
            //防止ConcurrentModificationException异常
            int size = 0;
            messageMapLock.readLock().lock();
            messageList = messageMap.get(phase);
            size = messageList.size();


            if (messagePointer >= size) {
                return null;
            }
            Message message = messageList.get(messagePointer);
            messagePointer++;
            return message;
        } finally {
            messageMapLock.readLock().unlock();
            messageListPointerLock.unlock();
        }
    }

    /**
     * @Author: luo
     * @Description: 消息处理进入下一个阶段
     * @param
     * @Data: 9:09 2021/9/2
     */
    public boolean tryUpgradePhase(MessageType messageType) {
        messageListPointerLock.lock();
        try {
            if (messageType.compare(phase) > 0) {
                phase = messageType;
                messagePointer = 0;
                return true;
            }
            return false;
        } finally {
            messageListPointerLock.unlock();
        }
    }

    public boolean tryReplyPhase(){
        try {
            messageListPointerLock.lock();
            if(phase.equals(MessageType.REPLY)) {
                return false;
            }
            phase= MessageType.REPLY;
            return true;
        }finally {
            messageListPointerLock.unlock();
        }
    }

    public boolean isReply(){
        try {
            messageListPointerLock.lock();
            if(phase.equals(MessageType.REPLY)) {
                return true;
            }
            return false;
        }finally {
            messageListPointerLock.unlock();
        }
    }

    public boolean isCommit(){
        try {
            messageListPointerLock.lock();
            if(phase.equals(MessageType.COMMIT)) {
                return true;
            }
            return false;
        }finally {
            messageListPointerLock.unlock();
        }
    }
}
