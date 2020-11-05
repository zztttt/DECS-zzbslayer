package reins.service;

import java.util.List;

public interface KeyValueService {
    void put(String key, String value);

    void put(String key, Object value);

    String get(String key);

    <T> T get(String key, Class<T> clazz);

    <T> List<T> getList(String key, Class<T> clazz);

    boolean exists(String key);

    void delete(String key);

}
