import com.alibaba.fastjson.JSONObject;
import com.common.key.Hash;
import com.common.key.KeyType;
import com.pbft.Net;
import com.pbft.common.emun.MessageType;
import com.pbft.common.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName BroadcastTest
 * @Description 广播测试
 * @Author ZhanZhuo
 * @Date 2021/9/7 8:31
 **/
@Slf4j
public class NetTest {
    @Test
    public void messageToStringTest() {
        Message msg = new Message();
        msg.setMessageType(MessageType.PRE_PREPARE);
        msg.setNumber(1);
        msg.setDigest("");
        System.out.println(msg.toString());
    }

    @Test
    public void postAllTest() {
        Message msg = new Message();
        msg.setMessageType(MessageType.NONE);
        msg.setNumber(1);
        msg.setDigest("");
        System.out.println("Send msg: " + msg);

        Net net = Net.getIntance();
        net.addUrl("http://127.0.0.1:8899/hello");
        net.asyncPostAll(msg.toString());
    }

    @Test
    public void jsonString(){
        List<Message> messageList =new ArrayList<>();
        for(int i=0;i<10;i++) {
            Message message = new Message();
            message.setOrderNumber(i);
            message.setNumber(i);
            message.setDigest("123");
            message.setMessageType(MessageType.COMMIT);
            message.setTimeMillis(System.currentTimeMillis());
            messageList.add(message);
        }
        String jsonString= JSONObject.toJSONString(messageList);
        System.out.println(jsonString);

        List<Message> messageList1 = JSONObject.parseArray(jsonString,Message.class);

        System.out.println("end");
    }

    @Test
    public void testListToMap(){

        List<Message> messageList =new ArrayList<>();
        Message message = new Message();
        message.setOrderNumber(3);
        message.setNumber(1);
        message.setViewNumber(1);
        message.setValue("123");
        message.setDigest(Hash.hash(message.getValue().getBytes(StandardCharsets.UTF_8), KeyType.ED25519));
        message.setMessageType(MessageType.PRE_PREPARE);
        message.setTimeMillis(System.currentTimeMillis());
        messageList.add(message);

        for(int i=0;i<3;i++) {
            Message msg = new Message();
            msg.setOrderNumber(i);
            msg.setNumber(1);
            msg.setDigest(message.getDigest());
            msg.setMessageType(MessageType.PREPARE);
            msg.setTimeMillis(System.currentTimeMillis());
            messageList.add(msg);
        }

       message = new Message();
        for(int i=0;i<3;i++) {
            Message msg = new Message();
            msg.setOrderNumber(i);
            msg.setNumber(2);
            msg.setDigest(message.getDigest());
            msg.setMessageType(MessageType.PREPARE);
            msg.setTimeMillis(System.currentTimeMillis());
            messageList.add(msg);
        }

        message.setOrderNumber(3);
        message.setNumber(2);
        message.setViewNumber(1);
        message.setValue("1234");
        message.setDigest(Hash.hash("".getBytes(StandardCharsets.UTF_8), KeyType.ED25519));
        message.setMessageType(MessageType.PRE_PREPARE);
        message.setTimeMillis(System.currentTimeMillis());
        messageList.add(message);
    }

    @Test
    public void testList(){
        List<String> bList= new ArrayList<>();
        bList.add("序号1");
        bList.add("序号1");
        bList.add("序号2");
        String str="";
        Map<String, String> map = bList.stream().collect(Collectors.toMap(String::toString,String::toString));
        System.out.println("");
    }
}
