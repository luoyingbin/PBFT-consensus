package com.db;

/**
 * @Author: luo
 * @Description:
 * @Data: 9:37 2021/9/17
 */
public interface AbstractDB {

    /**
     * 单条数据存储
     * @param key 键
     * @param value 值
     * */
    public  void save(String key,byte[] value);

    public  byte[] get(String key);
}
