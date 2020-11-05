package cn.lx.shiro.controller;

import cn.lx.shiro.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.springframework.web.bind.annotation.*;

/**
 * cn.lx.shiro.controller
 *
 * @Author Administrator
 * @date 11:12
 */
@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @GetMapping
    @RequiresRoles("admin")
    public String test() {
        return "aaaaaaaaaaaaaaaa";
    }

    @PostMapping(value = "/login")
    public String login(@RequestBody User user) {

        Subject currentUser = SecurityUtils.getSubject();

        if (!currentUser.isAuthenticated()) {
            //collect user principals and credentials in a gui specific manner
            //such as username/password html form, X509 certificate, OpenID, etc.
            //We'll use the username/password example here since it is the most common.
            //(do you know what movie this is from? ;)
            UsernamePasswordToken token = new UsernamePasswordToken(user.getUsername(), user.getPassword());
            //this is all you have to do to support 'remember me' (no config - built in!):
            token.setRememberMe(false);
            try {
                currentUser.login(token);

                //print their identifying principal (in this case, a username):
                log.info( "User [" + currentUser.getPrincipal() + "] logged in successfully." );

                //if no exception, that's it, we're done!

            } catch (UnknownAccountException uae) {
                //username wasn't in the system, show them an error message?
            } catch (IncorrectCredentialsException ice) {
                //password didn't match, try again?
            } catch (LockedAccountException lae) {
                //account for that username is locked - can't login.  Show them a message?
            } catch (AuthenticationException ae) {
                //unexpected condition - error?
            }
        }

        return "登录成功";
    }


    @PostMapping(value = "/logout")
    public String logout() {

        Subject currentUser = SecurityUtils.getSubject();

        if (currentUser.isAuthenticated()) {
            currentUser.logout();
        }
        return "退出成功";
    }
}
