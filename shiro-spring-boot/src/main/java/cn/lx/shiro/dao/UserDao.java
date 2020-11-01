package cn.lx.shiro.dao;

import cn.lx.shiro.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * cn.lx.ihrm.system.dao
 * JpaRepository<Company,String>中有两个泛型，第一个是指操作的对象，对象中必须有和
 * 数据库的映射，第二个是主键的类型
 * JpaSpecificationExecutor<Company> 它是一个条件构造器，后面会将如何使用
 *
 * @Author Administrator
 * @date 15:54
 */
public interface UserDao extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名或手机号查询用户对象
     *
     * @param username 用户名
     * @param mobile 手机号
     * @return
     */
    User findUserByUsernameOrMobile(String username, String mobile);
}
