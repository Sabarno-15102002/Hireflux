package com.sabarno.hireflux.service;

import java.util.UUID;

import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;

public interface UserService {
    User findUserByEmail(String email);
    User createOAuthUser(String email, String name, String profilePicture);
    User createUser(User user);
    User findUserFromToken(String token);
    User addResume(Resume resume);
    User addApplication(JobApplication application);
    void saveJob(UUID jobId, User user);
}
