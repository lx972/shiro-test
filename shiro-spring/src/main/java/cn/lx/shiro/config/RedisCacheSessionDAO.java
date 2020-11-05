package cn.lx.shiro.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * cn.lx.shiro.config
 *
 * @Author Administrator
 * @date 11:09
 */
@Getter
@Setter
public class RedisCacheSessionDAO extends EnterpriseCacheSessionDAO {

    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 根据session创建sessionId
     *
     * @param session
     * @return
     */
    @Override
    protected Serializable doCreate(Session session) {
        //父类方法，生成sessionId
        Serializable sessionId = super.doCreate(session);
        //将session保存到redis中
        storeSession(sessionId, session);
        return sessionId;
    }

    /**
     * 将session存储在redis中
     *
     * @param sessionId
     * @param session
     */
    protected void storeSession(Serializable sessionId, Session session) {
        if (sessionId == null) {
            throw new NullPointerException("id argument cannot be null.");
        }
        //todo:将session存储在redis中
        redisTemplate.opsForValue().set("session:"+sessionId,session,10, TimeUnit.MINUTES);

    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        Session session = (Session) redisTemplate.opsForValue().get("session:"+sessionId);
        return session;
    }

    @Override
    protected void doUpdate(Session session) {
        //将session保存到redis中
        storeSession(session.getId(), session);
       }

    @Override
    protected void doDelete(Session session) {
        redisTemplate.delete("session:"+session.getId());
    }
}
