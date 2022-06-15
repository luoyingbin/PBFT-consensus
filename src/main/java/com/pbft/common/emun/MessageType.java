package com.pbft.common.emun;

/**
 * @Author: luo
 * @Description: 消息状态
 */
public enum MessageType {
    //枚举值
    NONE(0),
    PRE_PREPARE(1),
    PREPARE(2),
    COMMIT(3),
    REPLY(4),
    VIEW_CHANGE(5),
    NEW_VIEW(6),
    CHECK_POINT(7),
    //状态同步所用消息
    QUERY_STATUS(8),
    STATUS(9),
    //增删节点过程
    REQUEST_UP_NODE(10),
    ADD_NODE(11),
    AGREE_NODE(12),
    JOIN_NET(13),

    //获取最新状态
    //请求下载数据
    //返回请求下载的具体commit数据
    ;

    public int getNumber() {
        return number;
    }

    private final int number;

    MessageType(int number) {
        this.number = number;
    }

    public boolean equals(MessageType messageType) {
        return this.number == messageType.getNumber();
    }

    public int compare(MessageType messageType) {
        if (this.number > messageType.getNumber()) {
            return 1;
        } else if (this.number < messageType.getNumber()) {
            return -1;
        }
        return 0;
    }
}
