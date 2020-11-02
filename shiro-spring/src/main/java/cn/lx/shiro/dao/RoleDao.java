package cn.lx.shiro.dao;

import cn.lx.shiro.domain.Role;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;


@Repository
public interface RoleDao{


    /**
     * 根据用户id获取用户拥有的角色
     * @param userId
     * @return
     */
    List<Role> findRolesByUserId(@Param("userId") String userId);
}


