package gameserver.memcached;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 使用memcached范例（使用memcached保存临时数据）
 *
 * @author caoxin
 */
public class UseMemcached<T extends Object, V extends Object & Serializable> {

    private MemcachedClientService<V> memcachedClientService;

    public void put(T o, String key, V v, Date expireDate) {
        String clazzName = o.getClass().getSimpleName();
        memcachedClientService.put(clazzName + key, v, expireDate);
    }

    public void put(T o, String key, V v) {
        put(o, key, v, null);
    }

    public V get(T o, String key) {
        String clazzName = o.getClass().getSimpleName();
        return memcachedClientService.get(clazzName + key);
    }

    public Map<String, V> get(T o, List<String> keys) {
        return memcachedClientService.gets(keys);
    }

    public void setMemcachedClinetService(MemcachedClientService memcachedClientService) {
        this.memcachedClientService = memcachedClientService;
    }
}