import com.common.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.rocksdb.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @Author: luo
 * @Description:
 * @Data: 9:45 2021/9/13
 */
@Slf4j
public class DBTest {
    static{
        RocksDB.loadLibrary();
    }
    static RocksDB rocksDB;
    static String path ="E:\\CCScode\\GIT\\Consensus\\data\\crawldb";
    @Test
    public void DBTest() throws Exception {
        Options options = new Options();
        options.setCreateIfMissing(true);
        rocksDB = RocksDB.open(options, path);

        Integer a=1;
        rocksDB.put("123".getBytes(StandardCharsets.UTF_8),String.valueOf(a).getBytes(StandardCharsets.UTF_8));
        byte[] b= rocksDB.get("123".getBytes(StandardCharsets.UTF_8));
        //单条插入
        rocksDB.put("hello".getBytes(), "world".getBytes());
        //单条查询
        System.out.println("单条查询:" + new String(rocksDB.get("hello".getBytes())));
        //批量插入
        try (final WriteOptions writeOpt = new WriteOptions()) {
            for (int i = 10; i <= 19; ++i) {
                try (final WriteBatch batch = new WriteBatch()) {
                    for (int j = 10; j <= 19; ++j) {
                        batch.put(String.format("%dx%d", i, j).getBytes(),
                                String.format("%d", i * j).getBytes());
                    }
                    rocksDB.write(writeOpt, batch);
                }
            }
        }
        RocksIterator iter = rocksDB.newIterator();
        for(iter.seekToFirst(); iter.isValid(); iter.next()) {
            System.out.println("iter key:" + new String(iter.key()) + ", iter value:" + new String(iter.value()));
        }

    }
}
