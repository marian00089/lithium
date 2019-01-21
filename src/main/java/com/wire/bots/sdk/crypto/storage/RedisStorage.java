package com.wire.bots.sdk.crypto.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;
import com.wire.bots.cryptobox.PreKey;
import com.wire.bots.cryptobox.StorageException;
import com.wire.bots.sdk.tools.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

public class RedisStorage implements IStorage {
    private static final byte[] EMPTY = new byte[0];
    private static final int TIMEOUT = 5000;
    private static JedisPool pool;
    private final String host;
    private final Integer port;
    private final String password;

    public RedisStorage(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public RedisStorage(String host, int port) {
        this.host = host;
        this.port = port;
        password = null;
    }

    public RedisStorage(String host) {
        this.host = host;
        password = null;
        port = null;
    }

    private static JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(1100);
        poolConfig.setMaxIdle(16);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    private static JedisPool pool(String host, Integer port, String password) {
        if (pool == null) {
            JedisPoolConfig poolConfig = buildPoolConfig();
            if (password != null && port != null)
                pool = new JedisPool(poolConfig, host, port, TIMEOUT, password);
            else if (port != null)
                pool = new JedisPool(poolConfig, host, port, TIMEOUT);
            else
                pool = new JedisPool(poolConfig, host);
        }
        return pool;
    }

    @Override
    public IRecord fetchSession(String id, String sid) throws StorageException {
        Jedis jedis = getConnection();
        String key = key(id, sid);
        byte[] data = jedis.getSet(key.getBytes(), EMPTY);
        if (data == null) {
            Logger.debug("redis: fetch key: %s size: %d", key, 0);
            return new Record(key, null, jedis);
        }

        for (int i = 0; i < 1000 && data.length == 0; i++) {
            sleep(5);
            data = jedis.getSet(key.getBytes(), EMPTY);
        }

        if (data.length == 0) {
            throw new StorageException("Redis Timeout when fetching Session with key: " + key);
        }

        Logger.debug("redis: fetch key: %s size: %d", key, data.length);
        return new Record(key, data, jedis);
    }

    @Override
    public byte[] fetchIdentity(String id) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("id_%s", id);
            byte[] bytes = jedis.get(key.getBytes());
            Logger.debug("fetchIdentity: %s, is NULL: %s", key, bytes == null);
            return bytes;
        }
    }

    @Override
    public void insertIdentity(String id, byte[] data) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("id_%s", id);
            jedis.set(key.getBytes(), data);
        }
    }

    @Override
    public PreKey[] fetchPrekeys(String id) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("pk_%s", id);
            Long llen = jedis.llen(key);
            PreKey[] ret = new PreKey[llen.intValue()];
            for (int i = 0; i < llen.intValue(); i++) {
                byte[] data = jedis.lindex(key.getBytes(), i);
                ret[i] = new PreKey(i, data);
            }
            return ret;
        }
    }

    @Override
    public void insertPrekey(String id, int kid, byte[] data) {
        try (Jedis jedis = getConnection()) {
            String key = String.format("pk_%s", id);
            jedis.lpush(key.getBytes(), data);
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private String key(String id, String sid) {
        return String.format("ses_%s-%s", id, sid);
    }

    private Jedis getConnection() {
        return pool(host, port, password).getResource();
    }

    private class Record implements IRecord {
        private final String key;
        private final byte[] data;
        private final Jedis jedis;

        Record(String key, byte[] data, Jedis jedis) {
            this.key = key;
            this.data = data;
            this.jedis = jedis;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public void persist(byte[] data) {
            if (data != null) {
                jedis.set(key.getBytes(), data);
                //Logger.info("redis: persist key: %s size: %d", key, data.length);
            } else {
                jedis.del(key);
                //Logger.info("redis: deleted key: %s", key);
            }
            jedis.close();
        }
    }
}
