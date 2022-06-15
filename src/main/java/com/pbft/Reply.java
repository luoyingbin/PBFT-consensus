package com.pbft;

import com.common.StringUtils;
import com.db.DbManager;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @Author: luo
 * @Description:
 * @Data: 9:56 2021/10/12
 */
@Slf4j
public class Reply {

    public void setValidatorManager(ValidatorManager validatorManager) {
        this.validatorManager = validatorManager;
    }

    ValidatorManager validatorManager;

    private Map<Integer,Message> cacheMap;

    private static Reply reply;
    public static Reply getProfile() {
        if (StringUtils.isNull(reply)) {
            reply = new Reply();
        }
        return reply;
    }

    /** 已经执行完成的共识的操作编号*/
    private final ReentrantReadWriteLock replySetLock;
    private  Integer replyNumber;

    private Reply(){
        replyNumber=0;
        replySetLock = new ReentrantReadWriteLock();
        validatorManager = ValidatorManager.getProfile();
        cacheMap = new HashMap<>();
    }

    private void reply(int number) {
        Message msg = loadCache(number);
        if(StringUtils.isNull(msg)){
            List<Message> messageList = DbManager.loadCommitNumber(number);
            msg =messageList.stream().filter(e->MessageType.PRE_PREPARE.equals(e.getMessageType())).collect(Collectors.toList()).get(0);
        }
        delCache(number);

        if (msg.getUpgrade() != 0) {
            if (msg.getUpgrade() > 0) {
                validatorManager.addValidator(msg.getValue());
            } else {
                validatorManager.delValidator(msg.getValue());
            }
            ConsensusManager.upgradeDown();
        }


    }

    private Message loadCache(Integer number){
       return cacheMap.get(number);
    }

    private void saveCache(Integer number,Message message){
        cacheMap.put(number,message);
    }

    private  void delCache(Integer number){
        cacheMap.remove(number);
    }

    public boolean replyNumber(int lastContinuityPoint,Integer number,Message message) {
        saveCache(number,message);
        replySetLock.readLock().lock();
        if (lastContinuityPoint > replyNumber) {
            replySetLock.readLock().unlock();
            replySetLock.writeLock().lock();
            if (lastContinuityPoint > replyNumber) {
                for(int i = replyNumber+1;i<=lastContinuityPoint;i++){
                    reply(i);
                }
            }
            replySetLock.writeLock().unlock();
        }else{
            replySetLock.readLock().unlock();
        }
        return true;
    }

    public int getLastReplyNumber(){
        return replyNumber;
    }
}
