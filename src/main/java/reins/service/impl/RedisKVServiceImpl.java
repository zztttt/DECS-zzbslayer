package reins.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reins.service.KeyValueService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisKVServiceImpl implements KeyValueService {
    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return JSONObject.parseObject(get(key), clazz);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> clazz) {
        return JSONArray.parseArray(get(key), clazz);
    }

    @Override
    public void put(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void put(String key, Object value) {
        put(key, JSON.toJSONString(value));
    }

    @Override
    public void put(String key, String value, int timeOut, TimeUnit tu) {
        redisTemplate.opsForValue().set(key, value, timeOut, tu);
    }

    @Override
    public void put(String key, Object value, int timeOut, TimeUnit tu) {
        put(key, JSON.toJSONString(value), timeOut, tu);
    }

    @Override
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
