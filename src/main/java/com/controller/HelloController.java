package com.controller;

import com.alibaba.fastjson.JSONObject;
import com.common.StringUtils;
import com.pbft.ConsensusManager;
import com.pbft.common.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: luo
 * @Description:
 * @Data: 11:04 2021/9/6
 */
@Slf4j
@RestController
public class HelloController {
    @Resource
    ConsensusManager consensusManager;

    @Value("${server.port}")
    int port;

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject getHello() {
        System.out.println("hello port:"+port);
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("result","hello port:"+port);
        return jsonObject;
    }

    @GetMapping(value = "/sendRequest", produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject sendRequest(String[] values) {
        JSONObject jsonObject=new JSONObject();
        consensusManager.addPool(values[0]);
        return jsonObject;
    }

    @GetMapping(value = "/addNode", produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject addNode(String value) {
        JSONObject jsonObject=new JSONObject();
        consensusManager.addNode(value);
        return jsonObject;
    }

    @GetMapping(value = "/delNode", produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject delNode(String value) {
        JSONObject jsonObject=new JSONObject();
        consensusManager.delNode(value);
        return jsonObject;
    }

    @GetMapping(value = "/viewChange", produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject viewChange(String[] values) {
        JSONObject jsonObject=new JSONObject();
       // pbftManager.startViewChange();
        return jsonObject;
    }

    @PostMapping(value = "/receiveMessage", produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject receiveMessage(@RequestBody @Valid Message msg) {
        log.debug("来自节点{}消息,消息类型:{},(v:{},n:{}),valueSize:{}",msg.getOrderNumber(),msg.getMessageType(),msg.getViewNumber(),msg.getNumber(),msg.getValue().length());
        consensusManager.receiveMessage(msg);
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("result","success");
        return jsonObject;
    }
}
