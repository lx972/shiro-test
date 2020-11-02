package cn.lx.shiro.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Role implements Serializable {
    private static final long serialVersionUID = 594829320797158219L;

    private String id;
    /**
     * 角色名
     */
    private String name;
    /**
     * 说明
     */
    private String description;
    /**
     * 企业id
     */
    private String companyId;


   private List<Permission> permissions = new ArrayList<Permission>(0);//角色与模块  多对多

}
