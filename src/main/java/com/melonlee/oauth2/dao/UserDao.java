package com.melonlee.oauth2.dao;

import com.melonlee.oauth2.entity.User;

import java.util.List;

/**
 * Created by Melon on 16/12/22.
 */
public interface UserDao {

    public User createUser(User user);

    public User updateUser(User user);

    public void deleteUser(Long userId);

    User findOne(Long userId);

    List<User> findAll();

    User findByUsername(String username);

}
