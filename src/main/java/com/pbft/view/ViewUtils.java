package com.pbft.view;

import com.common.key.Hash;
import com.common.key.KeyType;
import com.pbft.checkPoint.CheckpointManager;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: luo
 * @Description:
 * @Data: 16:42 2021/9/18
 */
@Slf4j
public class ViewUtils {
    /**
     * @Author: luo
     * @Description: 把列表中内容按照(操作number,message)分类，并校验是否满足2f+1，
     * 每个操作下对应的message只有一个pre-prepare消息和多个prepare消息，消息发送者都不应相同
     * @param messageList 待处理消息队列
     * @Data: 14:16 2021/9/9
     */
    public static Map<Integer, List<Message>> getPreparedMap(List<Message> messageList ,int quorumSize){
        //根据(操作number,message)分类
        Map<Integer, List<Message>> preparedMap = messageList.stream().
                filter(msg-> MessageType.PRE_PREPARE.equals(msg.getMessageType())|| MessageType.PREPARE.equals(msg.getMessageType())).
                collect(Collectors.toMap(Message::getNumber,
                        msg ->  {
                            List<Message> getNameList = new ArrayList<>();
                            if(MessageType.PRE_PREPARE.equals(msg.getMessageType())) {
                                if (!Hash.hash(msg.getValue().getBytes(StandardCharsets.UTF_8), KeyType.ED25519).equals(msg.getDigest())) {
                                    return getNameList;
                                }
                                getNameList.add(msg);
                            }
                            if(MessageType.PREPARE.equals(msg.getMessageType())) {
                                getNameList.add(msg);
                            }
                            return getNameList;
                        },
                        (List<Message> value1, List<Message> value2) -> {
                            if(value2.size() ==0){
                                return value1;
                            }
                            Message msg=value2.get(0);
                            boolean isPrePrePare= MessageType.PRE_PREPARE.equals(msg.getMessageType());
                            for(Message v1Msg:value1){
                                //重复的pre-prepare消息不接收
                                if(MessageType.PRE_PREPARE.equals(v1Msg.getMessageType()) && isPrePrePare ){
                                    return value1;
                                }
                                //重复的消息发送者消息不接收
                                if(v1Msg.getOrderNumber() == msg.getOrderNumber()){
                                    return value1;
                                }
                                //摘要不相等不接收
                                if(!v1Msg.getDigest().equals(msg.getDigest())){
                                    return value1;
                                }
                            }
                            value1.addAll(value2);
                            return value1;
                        }
                ));
        if(preparedMap.size() ==0){
            return preparedMap;
        }
        //收到的pre-prepare和prepare总数达到2f+1 并且每个操作队列里至少有一个pre-prepare
        boolean legal = preparedMap.values().stream().allMatch(
                msgList->msgList.size()>= quorumSize &&
                        msgList.stream().anyMatch(
                                msg->MessageType.PRE_PREPARE.equals(msg.getMessageType())));
        if(!legal){
            log.debug("收到的pre-prepare和prepare总数没达到2f+1或不存在pre-prepare消息");
            return null;
        }
        return preparedMap;
    }

    public static List<Message> getCheckpointList( List<Message> messageList, int stableCheckpoint ,int quorumSize){
        if(stableCheckpoint == CheckpointManager.START_STABLE_CHECKPOINT){
            return new ArrayList<>();
        }
        List<Message> checkpointList =  messageList.stream().filter(msg -> MessageType.CHECK_POINT.equals(msg.getMessageType())).collect(Collectors.toList());
        //将checkpoint发送者进行去重操作
        Set<Integer> nodeCount= new HashSet<>();
        checkpointList.forEach(msg->nodeCount.add(msg.getOrderNumber()));
        if(nodeCount.size() < quorumSize){
            log.debug("该viewChange中stableCheckpoint校验失败");
            return null;
        }

        //状态机状态
         /*
        String statemachine = checkpointList.get(0).getDigest();
        boolean stableCheckpointAdopt= checkpointList.stream().anyMatch(msg->msg.getDigest().equals(statemachine)&& msg.getNumber()==stableCheckpoint);
        if(!stableCheckpointAdopt){
            log.debug("该viewChange中stableCheckpoint校验失败");
            return null;
        }
        */
        return checkpointList;
    }

    public static List<Message> getViewChangeList(List<Message> messageList,int newViewNumber ,int quorumSize){
        List<Message> viewChangeList =  messageList.stream().filter(
                msg -> MessageType.VIEW_CHANGE.equals(msg.getMessageType()) && newViewNumber==msg.getViewNumber()).
                collect(Collectors.toList());
        Set<Integer>  nodeCount= new HashSet<>();
        viewChangeList.forEach(msg->nodeCount.add(msg.getOrderNumber()));
        if(nodeCount.size() < quorumSize){
            log.debug("该newView中viewChange数量不足，校验失败");
            return null;
        }
        return viewChangeList;
    }

}
