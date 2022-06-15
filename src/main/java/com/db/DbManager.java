package com.db;

import com.alibaba.fastjson.JSONObject;
import com.common.StringUtils;
import com.pbft.common.ConstantsDB;
import com.pbft.common.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: luo
 * @Description:
 * @Data: 9:36 2021/9/17
 */
public class DbManager {
    static String  consensusPath = "E:\\CCScode\\GIT\\Consensus\\data\\consensus";

    private static Rocks consensusRocks;


    public static AbstractDB getConsensusDB(int port){
        if (StringUtils.isNull(consensusRocks)) {
            consensusRocks = new Rocks(consensusPath+port);
        }
        return consensusRocks;
    }

    /**
     * @Author: luo
     * @Description: 保存共识操作结果，后续同步会用
     * @param number 操作序号
     * @param msgList 共识最后阶段commit阶段消息+prePrepared消息
     * @Data: 12:45 2021/9/22
     */
    public static void saveCommitMsg(int number, List<Message> msgList){
        consensusRocks.save(ConstantsDB.REQUEST+ number, JSONObject.toJSONString(msgList).getBytes(StandardCharsets.UTF_8));
    }

    public static List<Integer> loadCommitNumber(int startNumber, int endNumber) {
        List<Integer> sendBackSet = new ArrayList<>();
        for (int number = startNumber; number <= endNumber; number++) {
            byte[] values = consensusRocks.get(ConstantsDB.REQUEST + number);
            if (StringUtils.isNotNull(values)) {
                sendBackSet.add(number);
                //List<Message> commitList = JSONObject.parseArray(new String(values), Message.class);
                //commitMap.put(commitList.get(0).getNumber(), commitList);
            }
        }
        return sendBackSet;
    }

    public static List<Message> loadCommitNumber(int number){
        byte[] values = consensusRocks.get(ConstantsDB.REQUEST + number);
        if (StringUtils.isNotNull(values)) {
            return JSONObject.parseArray(new String(values), Message.class);
        }
        return null;
    }

    public static void saveDBStableCheckpoint(List<Message>  checkpointMsgList) {
        consensusRocks.save(ConstantsDB.STABLE_CHECKPOINT,JSONObject.toJSONString(checkpointMsgList).getBytes(StandardCharsets.UTF_8));
    }

    public  static  List<Message> loadStableCheckpoint() {
        byte[] stableCheckpointByte = consensusRocks.get(ConstantsDB.STABLE_CHECKPOINT);
        if (StringUtils.isNotNull(stableCheckpointByte)) {
            return JSONObject.parseArray(new String(stableCheckpointByte), Message.class);
        }
        return null;
    }

    public static void saveViewNumber(int viewNumber){
        consensusRocks.save(ConstantsDB.VIEW_NUMBER,String.valueOf(viewNumber).getBytes(StandardCharsets.UTF_8));
    }

    public static int loadViewNumber(){
        byte[] viewNumberByte = consensusRocks.get(ConstantsDB.VIEW_NUMBER);
        if(StringUtils.isNull(viewNumberByte)){
            return 0;
        }
        return Integer.parseInt(new String(viewNumberByte));
    }
}
