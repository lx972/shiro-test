package cn.lx.shiro.service;

import java.util.Set;

/**
 * cn.lx.ihrm.user.service
 *
 * @Author Administrator
 * @date 15:57
 */
public interface IUserService {


    /**
     * 根据用户名或手机号获取密码
     *
     * @param username
     * @return
     */
    String findPasswordByMobileOrUsername(String username);

    /**
     * 根据用户名获取用户拥有的角色名集合
     *
     * @param username
     * @return
     */
    Set<String> getRoleNamesForUser(String username);

    /**
     * 根据用户名获取用户拥有的权限名集合
     *
     * @param username
     * @return
     */
    Set<String> getPermissions(String username);
}
