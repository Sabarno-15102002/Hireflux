package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.config.JwtProvider;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.SavedJob;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.repository.SavedJobRepository;
import com.sabarno.hireflux.repository.UserRepository;
import com.sabarno.hireflux.service.JobService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.enums.AuthProvider;

@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JobService jobService;

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("No user found with the email:" + email));
    }

    @Override
    public User createOAuthUser(String email, String name, String profilePicture) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setProfilePicture(profilePicture);
        user.setAuthProvider(AuthProvider.GOOGLE);
        return userRepository.save(user);
    }

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findUserFromToken(String token) {
        String email = jwtProvider.getEmailFromJwtToken(token);
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("No user found with the email:" + email));
    }

    @Override
    public User addResume(Resume resume) {
        User user = resume.getUser();
        user.getResumes().add(resume);
        return userRepository.save(user);
    }

    @Override
    public User addApplication(JobApplication application) {
        User user = application.getApplicant();
        user.getApplications().add(application);
        return userRepository.save(user);
    }

    @Override
    public void saveJob(UUID jobId, User user) {
        Job job = jobService.getJobById(jobId);

        SavedJob savedJob = new SavedJob();
        savedJob.setUser(user);
        savedJob.setJob(job);
        savedJob.setSavedAt(LocalDateTime.now());

        savedJobRepository.save(savedJob);
    }

}
