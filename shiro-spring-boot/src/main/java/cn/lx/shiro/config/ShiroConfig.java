package cn.lx.shiro.config;

import cn.lx.shiro.realm.MyRealm;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * cn.lx.shiro.config
 *
 * @Author Administrator
 * @date 10:54
 */

@Configuration
public class ShiroConfig {


    /**
     * 注入自己的realm域
     */
   @Autowired
   private MyRealm myRealm;

    /**
     * 定义一个安全管理器
     * 在springboot项目中，它已经自动向spring容器中注入了一个，但是，
     * 他的默认设置不符合我们的需求，我们需要自己创建一个，使用自己的配置
     * 原始配置我们会在下面给出来
     * @return
     */
   @Bean
   public SessionsSecurityManager securityManager(){
       DefaultWebSecurityManager defaultWebSecurityManager=new DefaultWebSecurityManager();
       defaultWebSecurityManager.setRealm(myRealm);
       return defaultWebSecurityManager;
   }


    /**
     * 路径映射到给定的过滤器，以允许不同的路径具有不同的访问级别
     * 这个我们也需要覆盖springboot的自动配置
     * @return
     */
    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();
        //匿名访问
        chainDefinition.addPathDefinition("/test/**", "anon");
        //登出的url
        chainDefinition.addPathDefinition("/logout", "logout");
        //其他所有路径全部需要认证
        chainDefinition.addPathDefinition("/**", "authc");
        return chainDefinition;
    }

}
