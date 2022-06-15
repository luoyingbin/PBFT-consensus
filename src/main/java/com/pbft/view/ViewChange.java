package com.pbft.view;

import com.alibaba.fastjson.JSONObject;
import com.common.StringUtils;
import com.pbft.ConsensusManager;
import com.pbft.ValidatorManager;
import com.pbft.common.MessageUtils;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @Author: luo
 * @Description:
 * @Data: 11:07 2021/9/23
 */
@Slf4j
public class ViewChange {

    public static Message newViewChange(int newViewNumber, int stableCheckpoint, List<Message> messageList) {
        Message message = new Message();
        message.setMessageType(MessageType.VIEW_CHANGE);
        message.setViewNumber(newViewNumber);
        message.setValue(JSONObject.toJSONString(messageList));
        message.setNumber(stableCheckpoint);
        message.setTimeMillis(System.currentTimeMillis());
        message.setDigest("");
        return message;
    }

    public static void onViewChange(Message message, ValidatorManager validatorManager, ViewManager viewManager) {
        int quorumSize = validatorManager.getQuorumSize();
        int nextViewNumber = message.getViewNumber();
        if (viewManager.getViewNumber() >= nextViewNumber) {
            log.debug("不需要处理该viewChange,当前视图:{},viewChange视图：{}", viewManager.getViewNumber(), nextViewNumber);
            return;
        }
        //TODO：这里其他节点可以收集2f+1个 viewchange，然后启动定时器定时V+1+1 v+1次数过多可以考虑同步
        if (!validatorManager.isLeader(nextViewNumber)) {
            log.debug("不是新视图的leader，无需处理viewChange");
            return;
        }
        if (StringUtils.isEmpty(message.getValue())) {
            log.debug("value应该携带checkpoint消息和prepared消息");
            return;
        }
        List<Message> messageList = JSONObject.parseArray(message.getValue(), Message.class);
        if (!messageList.stream().allMatch(ConsensusManager::baseVerify)) {
            log.debug("viewChange中携带的message不合法，验证失败");
            return;
        }
        Map<Integer, List<Message>> preparedMap = ViewUtils.getPreparedMap(messageList, quorumSize);
        if (StringUtils.isNull(preparedMap)) {
            log.debug("该viewChange中prepared校验失败");
            return;
        }
        List<Message> checkpointList = ViewUtils.getCheckpointList(messageList, message.getNumber(), quorumSize);
        if (StringUtils.isNull(checkpointList)) {
            log.debug("该viewChange中checkpoint校验失败");
            return;
        }
        //TODO:这里没有队列长度限制，也没去重
        ViewSet viewSet = viewManager.getViewChangeManager(nextViewNumber);
        viewSet.addCheckpointMessage(checkpointList);
        viewSet.addPrePared(preparedMap);
        viewSet.addViewChange(message);
        log.debug("接收{}节点viewChange消息,viewNumber:{},stableCheckpoint:{}", message.getOrderNumber(), message.getViewNumber(), message.getNumber());

        // 1：新viewNumber的viewChange数量达到2f+1
        // 2：该view尚未执行过
        if (viewSet.getViewChangeCount() >= quorumSize &&
                viewSet.tryEnd(System.currentTimeMillis())) {
            Message msg = MessageUtils.newNewView(viewSet);
            log.debug("收到足够的viewChange，发起newView，view:{},valueSize:{}", msg.getViewNumber(), msg.getValue().length());
            ConsensusManager.broadMessage(msg);
        }
    }
}
