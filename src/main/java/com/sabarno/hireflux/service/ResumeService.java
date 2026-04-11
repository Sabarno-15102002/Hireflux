package com.sabarno.hireflux.service;

import java.util.UUID;

import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;


public interface ResumeService {

    public Resume saveParsedResume(User user, String fileKey, String fileName);
    public Resume getResumeForUser(User user);
    public void processResumeAsync(UUID resumeId, String fileKey);
    public Resume getResumeById(UUID resumeId);
}
