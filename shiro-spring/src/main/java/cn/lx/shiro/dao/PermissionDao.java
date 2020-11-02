package cn.lx.shiro.dao;

import cn.lx.shiro.domain.Permission;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionDao{

    /**
     * 根据用户id获取用户拥有的角色
     * @param roleId
     * @return
     */
    List<Permission> findPermissionsByRoleId(@Param("roleId") String roleId);
}
