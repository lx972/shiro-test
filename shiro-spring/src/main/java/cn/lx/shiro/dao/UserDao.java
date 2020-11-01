package cn.lx.shiro.dao;

import cn.lx.shiro.domain.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao {

    /**
     * 根据用户名或手机号查询用户对象
     *
     * @param username 用户名
     * @param mobile 手机号
     * @return
     */
    User findUserByUsernameOrMobile(@Param("username") String username, @Param("mobile") String mobile);
}
