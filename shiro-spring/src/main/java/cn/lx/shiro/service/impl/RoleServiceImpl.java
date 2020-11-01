package cn.lx.shiro.service.impl;

import cn.lx.shiro.dao.RoleDao;
import cn.lx.shiro.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * cn.lx.ihrm.role.service.impl
 *
 * @Author Administrator
 * @date 15:57
 */
@Service
public class RoleServiceImpl implements IRoleService {

    @Autowired
    private RoleDao roleDao;

}
