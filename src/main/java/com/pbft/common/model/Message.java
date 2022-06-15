package com.pbft.common.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.pbft.common.emun.MessageType;

/**
 * @Author: luo
 * @Description:
 * @Data: 15:39 2021/8/30
 */
public class Message {
    /**消息类型*/
    MessageType messageType;
    /**视图编号*/
    int viewNumber;
    /**编号*/
    int number;
    /**需共识的值*/
    String value;
    /**操作消息摘要*/
    String digest;
    /**时间戳*/
    long timeMillis;
    /**消息发送者序号*/
    int orderNumber;


    int upgrade;

    String signData;
    String publicKey;

    public Message() {
        this.messageType = MessageType.NONE;
        this.viewNumber = 0;
        this.number = 0;
        this.value = "";
        this.digest = "";
        this.timeMillis = 0;
        this.orderNumber = 0;
        this.signData = "";
        this.publicKey = "";
        this.upgrade =0;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @JSONField(serialize = false)
    public String getData() {
        StringBuilder str = new StringBuilder();
        return str.append(viewNumber).append(orderNumber).append(messageType.getNumber()).append(this.number).append(this.digest).append(this.timeMillis).toString();
    }

    public String getSignData() {
        return signData;
    }

    public void setSignData(String signData) {
        this.signData = signData;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public void setViewNumber(int viewNumber) {
        this.viewNumber = viewNumber;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }


    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    @Override
    public String toString() {
        return JSONObject.toJSON(this).toString();
    }

    public JSONObject toJSON() {
        return (JSONObject)JSONObject.toJSON(this);
    }


    public int getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(int upgrade) {
        this.upgrade = upgrade;
    }
}
