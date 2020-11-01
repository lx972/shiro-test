package cn.lx.shiro.service.impl;

import cn.lx.shiro.dao.PermissionDao;
import cn.lx.shiro.service.IPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * cn.lx.ihrm.permission.service.impl
 *
 * @Author Administrator
 * @date 15:57
 */
@Service
public class PermissionServiceImpl implements IPermissionService {

    @Autowired
    private PermissionDao permissionDao;


}
