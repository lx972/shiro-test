package cn.lx.shiro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Administrator
 * @date 15:58
 */
@RestController
@RequestMapping("/permission")
public class PermissionController{

    @GetMapping(value = "")
    public String findAll() {

        return "435465465";
    }

}
