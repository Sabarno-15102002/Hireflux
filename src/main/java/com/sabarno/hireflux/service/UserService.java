package com.sabarno.hireflux.service;

import com.sabarno.hireflux.entity.User;

public interface UserService {
    public User findUserByEmail(String email);
    public User createOAuthUser(String email, String name, String profilePicture);
    public User createUser(User user);

}
