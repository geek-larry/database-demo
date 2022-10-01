package com.jade.mongo.controller;

import com.jade.mongo.domain.User;
import com.jade.mongo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/test")
    public String getTest() {
        for (long i = 0; i < 10; i++) {
            User user = new User();
            user.setId(i);
            user.setUsername("larry" + i);
            user.setAge(18);
            user.setPassword("larry");
            userService.saveUser(user);
        }
        List<User> l1 = userService.findAllUser();
        List<User> l2 = userService.findAllUserByName("larry");
        System.out.println(l1);
        System.out.println(l2);
        return "ok";
    }
}
