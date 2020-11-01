package cn.lx.shiro.service.impl;

import cn.lx.shiro.dao.UserDao;
import cn.lx.shiro.domain.Permission;
import cn.lx.shiro.domain.Role;
import cn.lx.shiro.domain.User;
import cn.lx.shiro.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * cn.lx.ihrm.user.service.impl
 *
 * @Author Administrator
 * @date 15:57
 */
@Service
@Slf4j
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserDao userDao;

    /**
     * 根据用户名或手机号获取密码
     *
     * @param username
     * @return
     */
    public String findPasswordByMobileOrUsername(String username) {
        User user = userDao.findUserByUsernameOrMobile(username,username);
        if (user == null) {
            throw new RuntimeException("不存在此用户");
        }
        return user.getPassword();
    }

    /**
     * 根据用户名获取用户拥有的角色名集合
     *
     * @param username
     * @return
     */
    public Set<String> getRoleNamesForUser(String username) {
        User user = userDao.findUserByUsernameOrMobile(username,username);
        if (user == null) {
            throw new RuntimeException("不存在此用户");
        }
        Set<Role> roleSet = user.getRoles();
        Set<String> roleNames=new HashSet<String>();
        for (Role role : roleSet) {
            roleNames.add(role.getName());
        }
        return roleNames;
    }

    /**
     * 根据用户名获取用户拥有的权限名集合
     *
     * @param username
     * @return
     */
    public Set<String> getPermissions(String username) {
        User user = userDao.findUserByUsernameOrMobile(username,username);
        if (user == null) {
            throw new RuntimeException("不存在此用户");
        }
        Set<Role> roleSet = user.getRoles();
        Set<String> permissions=new HashSet<String>();
        for (Role role : roleSet) {
            Set<Permission> permissionSet = role.getPermissions();
            for (Permission permission : permissionSet) {
                permissions.add(permission.getCode());
            }
        }
        return permissions;
    }
}
