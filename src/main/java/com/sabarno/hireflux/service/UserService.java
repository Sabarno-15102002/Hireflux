package com.sabarno.hireflux.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.SavedJob;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.utility.projection.UserSummary;

public interface UserService {
    User findUserByEmail(String email);
    User createOAuthUser(String email, String name, String profilePicture);
    User createUser(User user);
    User findUserFromToken(String token);
    User addResume(Resume resume);
    User addApplication(JobApplication application);
    SavedJob saveJob(UUID jobId, User user);
    UserSummary getProfile(UUID userId);
    Page<UserSummary> getAllUsers(Pageable pageable);
}
