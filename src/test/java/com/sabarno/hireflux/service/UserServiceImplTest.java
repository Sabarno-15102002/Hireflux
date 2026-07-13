package com.sabarno.hireflux.service;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import com.sabarno.hireflux.config.JwtProvider;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.SavedJob;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.repository.JobRepository;
import com.sabarno.hireflux.repository.SavedJobRepository;
import com.sabarno.hireflux.repository.UserRepository;
import com.sabarno.hireflux.service.impl.UserServiceImpl;
import com.sabarno.hireflux.utility.enums.AuthProvider;
import com.sabarno.hireflux.utility.projection.UserSummary;


@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private JwtProvider jwtProvider;
    
    @Test
    void testCreateOAuthUser() {
        //input data for the test
        String email = "john.doe@example.com";
        String name = "John Doe";
        String profilePicture = "https://example.com/profile.jpg";
        //expected output
        User expectedUser = new User();
        expectedUser.setEmail(email);
        expectedUser.setName(name);
        expectedUser.setProfilePicture(profilePicture);
        expectedUser.setAuthProvider(AuthProvider.GOOGLE);

        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(expectedUser);
        User actualUser = userService.createOAuthUser(email, name, profilePicture);
        Assertions.assertNotNull(actualUser);
        Assertions.assertEquals(expectedUser.getEmail(), actualUser.getEmail());
        Assertions.assertEquals(expectedUser.getName(), actualUser.getName());
        Assertions.assertEquals(expectedUser.getProfilePicture(), actualUser.getProfilePicture());
        Assertions.assertEquals(expectedUser.getAuthProvider(), actualUser.getAuthProvider());
        verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    void testCreateUser() {
        //input data for the test
        User user = new User();
        user.setEmail("john.doe@example.com");
        user.setName("John Doe");
        user.setProfilePicture("https://example.com/profile.jpg");
        user.setAuthProvider(AuthProvider.LOCAL);

        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);
        User actualUser = userService.createUser(user);
        Assertions.assertNotNull(actualUser);
        Assertions.assertEquals(user.getEmail(), actualUser.getEmail());
        Assertions.assertEquals(user.getName(), actualUser.getName());
        Assertions.assertEquals(user.getProfilePicture(), actualUser.getProfilePicture());
        Assertions.assertEquals(user.getAuthProvider(), actualUser.getAuthProvider());
        verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    void testAddResume(){
        Resume resume = Mockito.mock(Resume.class);
        User user = Mockito.mock(User.class);
        Mockito.when(resume.getUser()).thenReturn(user);
        Mockito.when(user.getResumes()).thenReturn(new java.util.ArrayList<>());
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);
        User actualUser = userService.addResume(resume);
        Assertions.assertNotNull(actualUser);
        Assertions.assertTrue(actualUser.getResumes().contains(resume));
        Assertions.assertEquals(user, actualUser);
        verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    void testAddApplication(){
        JobApplication application = Mockito.mock(JobApplication.class);
        User user = Mockito.mock(User.class);
        Mockito.when(application.getApplicant()).thenReturn(user);
        Mockito.when(user.getApplications()).thenReturn(new java.util.ArrayList<>());
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);
        User actualUser = userService.addApplication(application);
        Assertions.assertNotNull(actualUser);
        Assertions.assertTrue(actualUser.getApplications().contains(application));
        Assertions.assertEquals(user, actualUser);
        verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    void testSaveJobPositive(){
        User user = Mockito.mock(User.class);
        UUID jobId = UUID.randomUUID();
        Job job = Mockito.mock(Job.class);
        SavedJob savedJob = Mockito.mock(SavedJob.class);

        Mockito.when(savedJob.getUser()).thenReturn(user);
        Mockito.when(job.getId()).thenReturn(jobId);
        Mockito.when(user.getName()).thenReturn("John Doe");
        Mockito.when(savedJob.getJob()).thenReturn(job);
        Mockito.when(savedJobRepository.save(Mockito.any(SavedJob.class))).thenReturn(savedJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        SavedJob expectedSavedJob = userService.saveJob(jobId, user);
        Assertions.assertNotNull(expectedSavedJob);
        Assertions.assertEquals(user, expectedSavedJob.getUser());
        Assertions.assertEquals(job, expectedSavedJob.getJob());
        Assertions.assertEquals(user.getName(), expectedSavedJob.getUser().getName());
        Assertions.assertEquals(job.getId(), expectedSavedJob.getJob().getId());
        verify(jobRepository).findById(jobId);
        verify(savedJobRepository).save(Mockito.any(SavedJob.class));
    }

    @Test
    void testSaveJobNegative(){
        UUID jobId = UUID.randomUUID();
        User user = Mockito.mock(User.class);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            userService.saveJob(jobId, user);
        });

        Assertions.assertEquals("No job found with the ID:" + jobId, exception.getMessage());
        verify(jobRepository).findById(jobId);
    }

    @Test
    void testGetProfilePositive(){
        UUID userId = UUID.randomUUID();
        UserSummary userSummary = Mockito.mock(UserSummary.class);
        Mockito.when(userRepository.findProfileById(userId)).thenReturn(Optional.of(userSummary));

        UserSummary actualUserSummary = userService.getProfile(userId);
        Assertions.assertNotNull(actualUserSummary);
        Assertions.assertEquals(userSummary, actualUserSummary);
        verify(userRepository).findProfileById(userId);
    }

    @Test
    void testGetProfileNegative(){
        UUID userId = UUID.randomUUID();
        Mockito.when(userRepository.findProfileById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            userService.getProfile(userId);
        });

        Assertions.assertEquals("No profile found with the ID:" + userId, exception.getMessage());
        verify(userRepository).findProfileById(userId);
    }

    @Test
    void testFindUserByEmailPositive() {
        String email = "john.doe@example.com";
        User user = Mockito.mock(User.class);
        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        User actualUser = userService.findUserByEmail(email);
        Assertions.assertNotNull(actualUser);
        Assertions.assertEquals(user, actualUser);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testFindUserByEmailNegative() {
        String email = "john.doe@example.com";
        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            userService.findUserByEmail(email);
        });

        Assertions.assertEquals("No user found with the email:" + email, exception.getMessage());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testFindUserFromTokenPositive() {
        String token = "valid-token";
        String email = "john.doe@example.com";
        Mockito.when(jwtProvider.getEmailFromJwtToken(token)).thenReturn(email);
        User user = Mockito.mock(User.class);
        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        User actualUser = userService.findUserFromToken(token);
        Assertions.assertNotNull(actualUser);
        Assertions.assertEquals(user, actualUser);
        verify(jwtProvider).getEmailFromJwtToken(token);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testFindUserFromTokenNegative() {
        String token = "invalid-token";
        String email = "john.doe@example.com";
        Mockito.when(jwtProvider.getEmailFromJwtToken(token)).thenReturn(email);
        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            userService.findUserFromToken(token);
        });

        Assertions.assertEquals("No user found with the email:" + email, exception.getMessage());
        verify(jwtProvider).getEmailFromJwtToken(token);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testGetAllUsers(){
        Pageable pageable = PageRequest.of(0, 10);

        UserSummary user1 = mock(UserSummary.class);
        UserSummary user2 = mock(UserSummary.class);

        List<UserSummary> users = List.of(user1, user2);
        Page<UserSummary> expectedPage =
                new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAllProjectedBy(pageable))
                .thenReturn(expectedPage);

        Page<UserSummary> result = userService.getAllUsers(pageable);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getTotalElements());
        Assertions.assertEquals(expectedPage, result);

        verify(userRepository).findAllProjectedBy(pageable);
    }
}