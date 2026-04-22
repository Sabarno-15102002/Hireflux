package com.sabarno.hireflux.service;

import java.util.UUID;

import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;


public interface ResumeService {

    Resume saveParsedResume(User user, String fileKey, String fileName);
    Resume getResumeForUser(User user);
    void processResumeAsync(UUID resumeId, String fileKey);
    Resume getResumeById(UUID resumeId);
}
