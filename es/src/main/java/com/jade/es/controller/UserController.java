package com.jade.es.controller;

import com.jade.elasticsearchdb.dto.LoginDto;
import com.jade.elasticsearchdb.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/login")
    public Result<Object> login(Map<String, String> params) {
        return Result.builder()
                .code(20000)
                .data(LoginDto.builder().token("admin-token").build())
                .build();
    }

    @GetMapping("/logout")
    public Result<Object> logout() {
        return Result.builder()
                .code(20000)
                .data("success")
                .build();
    }

    @GetMapping("/info")
    public Result<Object> info() {
        Map<String, Object> map = new HashMap<>();
        map.put("avatar", "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
        map.put("introduction", "I am a super administrator");
        map.put("name", "Super Admin");
        map.put("roles", Arrays.asList("admin"));
        return Result.builder()
                .code(20000)
                .data(map)
                .build();
    }
}
