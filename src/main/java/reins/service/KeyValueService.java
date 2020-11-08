package reins.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface KeyValueService {
    void put(String key, String value);

    void put(String key, Object value);

    void put(String key, String value, int timeOut, TimeUnit tu);

    void put(String key, Object value, int timeOut, TimeUnit tu);

    String get(String key);

    <T> T get(String key, Class<T> clazz);

    <T> List<T> getList(String key, Class<T> clazz);

    boolean exists(String key);

    void delete(String key);

}
