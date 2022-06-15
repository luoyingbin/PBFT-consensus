package com.pbft.common;

import com.alibaba.fastjson.JSONObject;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import com.pbft.view.ViewSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: luo
 * @Description: 消息生成工具
 * @Data: 15:02 2021/9/18
 */
public class MessageUtils {


    public static Message newMessage(MessageType messageType, Message message) {
        Message msg = new Message();
        msg.setMessageType(messageType);
        msg.setViewNumber(message.getViewNumber());
        msg.setNumber(message.getNumber());
        msg.setTimeMillis(System.currentTimeMillis());
        msg.setDigest(message.getDigest());
        return msg;
    }



    public static Message newNewView(ViewSet viewSet){
        List<Message> viewChangeList = viewSet.getViewChangeList();
        List<Message> preParedList = viewSet.getPreparedList();
        List<Message> cpList = viewSet.getCheckpointList();
        //新建newView消息
        List<Message> msgList = new ArrayList<>();
        msgList.addAll(viewChangeList);
        msgList.addAll(preParedList);
        msgList.addAll(cpList);
        Message message = new Message();
        message.setMessageType(MessageType.NEW_VIEW);
        message.setValue(JSONObject.toJSONString(msgList));
        message.setViewNumber(viewSet.getViewNumber());
        message.setTimeMillis(System.currentTimeMillis());
        message.setNumber(viewSet.getStableCheckpoint());
        message.setDigest(viewSet.getStatemachine());
        return message;
    }


}
