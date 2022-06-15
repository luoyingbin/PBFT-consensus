package com.pbft;

import com.common.StringUtils;
import com.common.http.HttpUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @ClassName Broadcast
 * @Description TODO
 * @Author ZhanZhuo
 * @Date 2021/9/6 15:02
 **/
@Slf4j
public class Net {
    List<String> urls;

    Map<String,String> urlMap;

    private static Net net;

    private Net() {
        urls = new ArrayList<>();
        urls.add("http://127.0.0.1:8000/receiveMessage");
        urls.add("http://127.0.0.1:8001/receiveMessage");
        urls.add("http://127.0.0.1:8002/receiveMessage");
        urls.add("http://127.0.0.1:8003/receiveMessage");
        urlMap=new HashMap<>();
        urlMap.put("ccs3D2dQHJk2KdT7HnYXbpQWFoAV5nPe4TbG","http://127.0.0.1:8000/receiveMessage");
        urlMap.put("ccs3GufLniMhbTRr19QP5WL22hMHGa86AhyF","http://127.0.0.1:8001/receiveMessage");
        urlMap.put("ccs3CUMwePYUHCzFuytbKk3KURiH2gSPsyFp","http://127.0.0.1:8002/receiveMessage");
        urlMap.put("ccs3Eez8KkKf36GAzNZAQaHw4q2MAPWRCCGG","http://127.0.0.1:8003/receiveMessage");
    }

    public static Net getIntance() {
        if(null == net || null == net.urls) {
            net = new Net();
        }
        return net;
    }

    public void addUrl(String url) {
        this.urls.add(url);
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public void postAll(String param) {
        for(String url : this.urls) {
            HttpUtils.sendPost(url, param);
        }
    }

    public TimerTask asyncPostAll(String param) {
        return new TimerTask() {
            @Override
            public void run() {
                for(String url : urls) {
                    HttpUtils.sendPost(url, param);
                }
            }
        };
    }

    public void post(String address,String param){
        String url=urlMap.get(address);
        if(StringUtils.isEmpty(url)){
            log.error("url不存在");
            return;
        }
        HttpUtils.sendPost(url, param);
    }
}
