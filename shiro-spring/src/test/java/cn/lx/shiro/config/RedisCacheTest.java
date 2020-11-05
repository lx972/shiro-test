package cn.lx.shiro.config;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Set;


/**
 * cn.lx.shiro.config
 *
 * @Author Administrator
 * @date 10:53
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:spring-context.xml","classpath:spring-mvc.xml"})
public class RedisCacheTest {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 测试scan
     */
    @org.junit.Test
    public void clear() {

        Set keys = new RedisCache<String,String>("").keys();
        new RedisCache<String,String>("").clear();
        System.out.println(keys);

    }
}
