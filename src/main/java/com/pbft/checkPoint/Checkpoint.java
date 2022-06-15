package com.pbft.checkPoint;

import com.common.key.Hash;
import com.common.key.KeyType;
import com.db.DbManager;
import com.pbft.ConsensusManager;
import com.pbft.ValidatorManager;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @Author: luo
 * @Description:
 * @Data: 10:55 2021/9/23
 */
@Slf4j
public class Checkpoint {
    public static Message newCheckPoint(int number){
        Message message = new Message();
        message.setMessageType(MessageType.CHECK_POINT);
        message.setValue("");
        message.setViewNumber(0);
        message.setTimeMillis(System.currentTimeMillis());
        message.setNumber(number);
        //n执行后的状态机状态的摘要
        message.setDigest(Hash.hash("".getBytes(StandardCharsets.UTF_8), KeyType.ED25519));
        return message;
    }

    public static boolean onCheckPoint(Message message, ValidatorManager validatorManager, CheckpointManager checkPointManager) {
        int quorumSize= validatorManager.getQuorumSize();
        if (checkPointManager.getStableCheckpoint() >= message.getNumber()) {
            log.debug("收到的stableCheckpoint过小,拒绝");
        }
        log.debug("接收{}节点checkpoint消息 checkpoint:{}", message.getOrderNumber(), message.getNumber());
        checkPointManager.addMessage(message);
        //当checkpoint达到2f+1,提升为stableCheckpoint,并执行日志清理工作
        if (checkPointManager.readyUpStableCheckpoint(quorumSize) && checkPointManager.upStableCheckpoint(quorumSize)) {
            log.debug("stableCheckpoint更新成功，最新稳定检查点:{}", checkPointManager.getStableCheckpoint());
            //删除stableCheckpoint之前的历史消息
            // 避免不会发起下一个stableCheckpoint请求，出现条件：当前stableCheckpoint更新完成，但stableCheckpoint后续的共识操作请求都早已完成,不会发起下一个stableCheckpoint请求
            trySendNextCheckpoint(checkPointManager);
            return true;
        }
        return false;
    }

    public static void trySendNextCheckpoint(CheckpointManager checkPointManager){
        int nextCheckpoint = checkPointManager.tryGetCheckpoint();
        if (nextCheckpoint != -1) {
            log.debug("当前节点满足下一个稳定检查点条件，发送下一个checkpoint:{}消息", nextCheckpoint);
            Message msg = newCheckPoint(nextCheckpoint);
            ConsensusManager.broadMessage(msg);
        }
    }


}
