package cn.lx.shiro.config;

import lombok.Setter;
import org.apache.shiro.cache.*;
import org.apache.shiro.util.LifecycleUtils;

import java.util.Iterator;


/**
 * cn.lx.shiro.config
 *
 * @Author Administrator
 * @date 11:27
 */
@Setter
public class RedisCacheManager extends AbstractCacheManager {

    /**
     * 仿写{@link MemoryConstrainedCacheManager}
     * 创建一个redis缓存，然后缓存实现增删改查
     *
     * @param name
     * @return
     * @throws CacheException
     */
    @Override
    protected Cache createCache(String name) throws CacheException {
        return new RedisCache(name);
    }

}
