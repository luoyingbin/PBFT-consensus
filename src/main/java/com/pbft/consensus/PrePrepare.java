package com.pbft.consensus;

import com.common.key.Hash;
import com.common.key.KeyType;
import com.pbft.ConsensusManager;
import com.pbft.ValidatorManager;
import com.pbft.common.MessageUtils;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import com.pbft.message.MessageLog;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @Author: luo
 * @Description:
 * @Data: 10:00 2021/9/23
 */
@Slf4j
public class PrePrepare {

    public static Message newPrePrepare(int viewNumber, int number, String value) {
        Message message = new Message();
        message.setMessageType(MessageType.PRE_PREPARE);
        message.setViewNumber(viewNumber);
        message.setValue(value);
        message.setNumber(number);
        message.setTimeMillis(System.currentTimeMillis());
        message.setDigest(Hash.hash(value.getBytes(StandardCharsets.UTF_8), KeyType.ED25519));
        return message;
    }

    public static Message upgrade(int viewNumber, int number, String value,int upgrade){
        Message message = new Message();
        message.setMessageType(MessageType.PRE_PREPARE);
        message.setViewNumber(viewNumber);
        message.setValue(value);
        message.setNumber(number);
        message.setUpgrade(upgrade);
        message.setTimeMillis(System.currentTimeMillis());
        message.setDigest(Hash.hash(value.getBytes(StandardCharsets.UTF_8), KeyType.ED25519));
        return message;
    }

    public static boolean on(MessageLog messageLog, Message message, ValidatorManager validatorManager) {
        if (!Hash.hash(message.getValue().getBytes(StandardCharsets.UTF_8), KeyType.ED25519).equals(message.getDigest())) {
            log.debug("摘要信息与原数据验证失败");
            return false;
        }
        if (messageLog.getViewNumber() % validatorManager.getValidatorList().size() != message.getOrderNumber()) {
            log.debug("该消息发送者不是这个view的leader,只能处理ledger的PRE-PREPARE消息");
            return false;
        }
        if(!messageLog.getCheckValue()){
            //log.debug("prePrepare消息value验证失败");
           // return false;
        }

        //TODO:需要执行Prepare Value行为

        //提升消息阶段，尝试转进Prepare阶段（如果已有别的线程转进Prepare阶段则失败）
        if (messageLog.tryUpgradePhase(MessageType.PREPARE)) {
            log.debug("进入Prepare阶段(v:{},n:{})",messageLog.getViewNumber(),messageLog.getNumber());
            messageLog.setPrePrepare(message);
            if(validatorManager.isLeader(messageLog.getViewNumber())){
                return true;
            }
            Message msg = MessageUtils.newMessage(MessageType.PREPARE, message);
            ConsensusManager.broadMessage(msg);
            return true;
        }
        return false;
    }

}
