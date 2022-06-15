package com.db;

import com.common.StringUtils;
import com.pbft.ValidatorManager;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.nio.charset.StandardCharsets;

/**
 * @Author: luo
 * @Description:
 * @Data: 9:40 2021/9/17
 */
public class Rocks implements AbstractDB {
//    private static Rocks rocks;
//    public static AbstractDB getProfile(String path) {
//        if (StringUtils.isNull(rocks)) {
//            rocks = new Rocks(path);
//        }
//        return rocks;
//    }



    static RocksDB rocksDB;

    public Rocks(String path){
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            rocksDB = RocksDB.open(options, path);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(String key, byte[] value) {
        try {
            rocksDB.put(key.getBytes(),value);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }
    @Override
    public byte[] get(String key){
        try {
            return rocksDB.get(key.getBytes(StandardCharsets.UTF_8));
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }
}
