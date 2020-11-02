package cn.lx.shiro.config;

import java.util.LinkedHashMap;

/**
 * cn.lx.shiro.config
 * 我们通过一个工厂实例构建map集合
 *
 * @Author Administrator
 * @date 10:19
 */
public class FilterChainDefinitionMapBuilder {

    /**
     * 这个方法产生一个map集合
     * map集合的数据是从数据库中查询出来的
     * 这里为了方便，我们就硬编码了
     *
     * @return
     */
    public LinkedHashMap<String, String> buildFilterChainDefinitionMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        /*
        /admin/** = authc, roles[admin]
         /docs/** = authc, perms[document:read]
         /test/** = anon
         /login.html = anon
         /** = authc
         */
        map.put("/admin/**", "authc, roles[admin]");
        map.put("/docs/**", "authc, perms[document:read]");
        map.put("/test/**", "anon");
        map.put("/login.html", "anon");
        map.put("/**", "authc");
        return map;
    }
}
