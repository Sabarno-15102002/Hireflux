package com.sabarno.hireflux.service;

import java.util.List;
import java.util.UUID;

import com.sabarno.hireflux.dto.response.ResumeResponse;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;


public interface ResumeService {

    ResumeResponse saveParsedResume(User user, String fileKey, String fileName);
    List<Resume> getResumeForUser(User user);
    void processResume(UUID resumeId, String fileKey);
    Resume getResumeById(UUID resumeId);
}
