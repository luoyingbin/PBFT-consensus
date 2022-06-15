package com.pbft.consensus;

import com.pbft.ValidatorManager;
import com.pbft.common.model.Message;
import com.pbft.message.MessageLog;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: luo
 * @Description:
 * @Data: 10:10 2021/9/23
 */
@Slf4j
public class Commit {

    public static boolean on(MessageLog messageLog, Message message, ValidatorManager validatorManager) {
        int quorumSize=validatorManager.getQuorumSize();
        Message prePrepareMsg = messageLog.getPrePrepare();
        if (!prePrepareMsg.getDigest().equals(message.getDigest())) {
            log.debug("收到的Digest与PRE-PREPARE的Digest不相同，拒绝");
            return false;
        }

        messageLog.addCommit(message);
        //确认是否已收到2f+1个COMMIT消息,如果收到尝试进入reply阶段（多线程下只有一个能进入）
        return messageLog.getCommitSize() >= quorumSize && messageLog.tryReplyPhase();
    }
}
