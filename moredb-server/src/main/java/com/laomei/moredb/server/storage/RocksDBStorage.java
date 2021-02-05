package com.laomei.moredb.server.storage;

import com.laomei.moredb.common.exception.MoreDBException;
import com.laomei.moredb.server.util.SerializeUtil;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author luobo.hwz on 2021/01/19 16:40
 */
@Slf4j
public class RocksDBStorage implements Closeable {

    static {
        RocksDB.loadLibrary();
    }

    private static final String BASE_DIR = "rocks";

    private final RocksDB rocksDB;

    public RocksDBStorage() {
        final Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            Files.createDirectories(Paths.get(BASE_DIR));
        } catch (FileAlreadyExistsException e) {
            log.info("rocks dir exist, ignore exception");
        } catch (IOException e) {
            log.error("create rocks db data dir failed", e);
            throw new RuntimeException(e);
        }
        try {
            this.rocksDB = RocksDB.open(options, "rocks");
        } catch (RocksDBException e) {
            log.error("init rocks db failed", e);
            throw new RuntimeException(e);
        }
    }

    public synchronized void save(final String key, final byte[] value) {
        log.info("saving value '{}' with key '{}'", key, value);
        try {
            rocksDB.put(SerializeUtil.serialize(key), value);
        } catch (RocksDBException e) {
            log.error("save data failed, key: '{}', value: '{}'", key, value, e);
            throw new MoreDBException("save data failed", e);
        }
    }

    public synchronized byte[] find(final String key) {
        try {
            return rocksDB.get(SerializeUtil.serialize(key));
        } catch (RocksDBException e) {
            log.error("find value for key '{}' failed", key, e);
            throw new MoreDBException("find value for key failed", e);
        }
    }

    public synchronized void delete(final String key) {
        try {
            rocksDB.delete(SerializeUtil.serialize(key));
        } catch (RocksDBException e) {
            log.error("delete value for key '{}' failed", key, e);
            throw new MoreDBException("delete value for key failed", e);
        }
    }

    @Override
    public void close() throws IOException {
        rocksDB.close();
    }
}
