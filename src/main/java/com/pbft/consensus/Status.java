package com.pbft.consensus;

import com.alibaba.fastjson.JSONObject;
import com.common.key.Hash;
import com.common.key.KeyType;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author: luo
 * @Description:
 * @Data: 9:01 2021/9/26
 */
public class Status {
    public static Message newQuestStatus() {
        Message message = new Message();
        message.setMessageType(MessageType.QUERY_STATUS);
        message.setViewNumber(0);
        message.setValue("");
        message.setNumber(0);
        message.setTimeMillis(System.currentTimeMillis());
        message.setDigest("");
        return message;
    }

    public static Message newStatus(int viewNumber, int number, List<String> validatorList) {
        Message message = new Message();
        message.setMessageType(MessageType.STATUS);
        message.setViewNumber(viewNumber);
        message.setValue(JSONObject.toJSONString(validatorList));
        message.setNumber(number);
        message.setTimeMillis(System.currentTimeMillis());
        message.setDigest((Hash.hash((viewNumber+number+message.getValue()).getBytes(StandardCharsets.UTF_8), KeyType.ED25519)));
        return message;
    }

}
