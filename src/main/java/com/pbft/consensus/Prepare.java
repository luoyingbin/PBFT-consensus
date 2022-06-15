package com.pbft.consensus;

import com.pbft.ConsensusManager;
import com.pbft.ValidatorManager;
import com.pbft.common.MessageUtils;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import com.pbft.message.MessageLog;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: luo
 * @Description:
 * @Data: 10:07 2021/9/23
 */
@Slf4j
public class Prepare {

    public static boolean on(MessageLog messageLog, Message message, ValidatorManager validatorManager) {
        Message prePrepareMsg = messageLog.getPrePrepare();
        if (prePrepareMsg.getOrderNumber() == message.getOrderNumber()) {
            log.debug("拒绝接收leader的prepare");
            return false;
        }

        if (!prePrepareMsg.getDigest().equals(message.getDigest())) {
            log.debug("收到的Digest与PRE-PREPARE的Digest不相同，拒绝");
            return false;
        }

        messageLog.addPrepare(message);
        //TODO:如果验证节点数量发生变化，会影响这里判断getQuorumSize()
        //确认是否已收到2f+1个PREPARE消息,尝试commit阶段，只处理commit消息
        if (messageLog.getPrepareSize() >= validatorManager.getQuorumSize() - 1 && messageLog.tryUpgradePhase(MessageType.COMMIT)) {
            log.debug("进入Commit阶段(v:{},n:{})",messageLog.getViewNumber(),messageLog.getNumber());
            Message msg = MessageUtils.newMessage(MessageType.COMMIT, prePrepareMsg);
            ConsensusManager.broadMessage(msg);
            return true;
        }
        return false;
    }
}
