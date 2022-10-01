package com.jade.mongo.service;

import com.jade.mongo.domain.User;

import java.util.List;

public interface UserService {
    //保存一个用户
    void saveUser(User user);

    //查询所有用户
    List<User> findAllUser();

    //根据用户名模糊查询所有用户
    List<User> findAllUserByName(String username);

    //修改用户
    void updateUser(User user);

    //删除用户
    void delUser(User user);
}
