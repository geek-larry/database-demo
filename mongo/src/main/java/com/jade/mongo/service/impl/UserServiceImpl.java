package com.jade.mongo.service.impl;

import com.jade.mongo.domain.User;
import com.jade.mongo.service.UserService;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserServiceImpl implements UserService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void saveUser(User user) {
        mongoTemplate.save(user);
    }

    @Override
    public List<User> findAllUser() {
        return mongoTemplate.findAll(User.class);
    }

    @Override
    public List<User> findAllUserByName(String username) {
        Query query = new Query(Criteria.where("username").regex(username));
        return mongoTemplate.find(query, User.class, "user");
    }

    @Override
    public void updateUser(User user) {
        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update().set("username", user.getUsername()).set("passWord", user.getPassword());
        //更新查询返回结果的第一个数据
        UpdateResult u = mongoTemplate.updateFirst(query, update, User.class);
        System.out.println(u);
    }

    @Override
    public void delUser(User user) {
        Query query = new Query(Criteria.where("id").is(user.getId()));
        mongoTemplate.remove(query, User.class);
    }
}
