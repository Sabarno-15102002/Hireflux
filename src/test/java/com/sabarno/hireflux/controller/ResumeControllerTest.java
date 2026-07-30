package com.sabarno.hireflux.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sabarno.hireflux.dto.UploadDTO;
import com.sabarno.hireflux.dto.response.ResumeResponse;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.event.ResumeEventProducer;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.service.ResumeService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.service.util.RateLimitService;
import com.sabarno.hireflux.service.util.S3Service;
import com.sabarno.hireflux.utility.RateLimitUtil;

import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

        @InjectMocks
        private ResumeController resumeController;

        @Mock
        private UserService userService;

        @Mock
        private ResumeService resumeService;

        @Mock
        private S3Service s3Service;

        @Mock
        private MeterRegistry meterRegistry;

        @Mock
        private ResumeEventProducer resumeEventProducer;

        @Mock
        private RateLimitService rateLimitService;

        @Mock
        private Bucket bucket;

        @Mock
        private Timer timer;

        @Mock
        private Timer.Sample sample;

        private User user;

        private Resume resume;

        private ResumeResponse resumeResponse;

        @BeforeEach
        void setUp() {

                user = new User();
                user.setId(UUID.randomUUID());
                user.setEmail("test@test.com");

                resume = new Resume();
                resume.setId(UUID.randomUUID());
                resume.setUser(user);
                resume.setFileKey("resumes/file.pdf");

                resumeResponse = new ResumeResponse();
                resumeResponse.setId(resume.getId());

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                Collections.emptyList());

                SecurityContext context = SecurityContextHolder.createEmptyContext();

                context.setAuthentication(authentication);

                SecurityContextHolder.setContext(context);
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void testtestGenerateUploadUrl_shouldReturnUploadUrl() {

                when(userService.findUserByEmail(user.getEmail()))
                                .thenReturn(user);

                when(s3Service.generateUploadUrl(
                                anyString(),
                                anyString()))
                                .thenReturn("https://upload-url");

                ResponseEntity<Map<String, String>> response = resumeController.generateUploadUrl(
                                "resume.pdf",
                                "application/pdf");

                assertEquals(
                                HttpStatus.CREATED,
                                response.getStatusCode());

                assertEquals(
                                "https://upload-url",
                                response.getBody().get("uploadUrl"));

                assertEquals(
                                "resumes/" + user.getId() + "/resume.pdf",
                                response.getBody().get("fileKey"));

                verify(s3Service)
                                .generateUploadUrl(
                                                "resumes/" + user.getId() + "/resume.pdf",
                                                "application/pdf");
        }

        @Test
        void testGenerateUploadUrl_shouldThrowException_whenFileTypeInvalid() {

                assertThrows(
                                BadRequestException.class,
                                () -> resumeController.generateUploadUrl(
                                                "resume.docx",
                                                "application/msword"));

                verifyNoInteractions(s3Service);
        }

        @Test
        void testUploadResume_shouldSaveResumeAndPublishEvent() {

                UploadDTO dto = new UploadDTO();

                dto.setFileKey("resumes/test.pdf");
                dto.setFileName("resume.pdf");

                when(userService.findUserByEmail(user.getEmail()))
                                .thenReturn(user);

                when(rateLimitService.resolveBucket(
                                anyString(),
                                anyLong(),
                                any(Duration.class)))
                                .thenReturn(bucket);

                when(resumeService.saveParsedResume(
                                any(User.class),
                                anyString(),
                                anyString()))
                                .thenReturn(resumeResponse);

                when(meterRegistry.timer(anyString()))
                                .thenReturn(timer);

                try (MockedStatic<Timer> timerMock = Mockito.mockStatic(Timer.class);

                                MockedStatic<RateLimitUtil> rateLimitMock = Mockito.mockStatic(RateLimitUtil.class)) {

                        timerMock.when(() -> Timer.start(meterRegistry))
                                        .thenReturn(sample);

                        ResponseEntity<ResumeResponse> response = resumeController.uploadResume(dto);

                        assertEquals(
                                        HttpStatus.CREATED,
                                        response.getStatusCode());

                        assertEquals(
                                        resumeResponse,
                                        response.getBody());

                        verify(resumeService)
                                        .saveParsedResume(
                                                        user,
                                                        "resumes/test.pdf",
                                                        "resume.pdf");

                        verify(resumeEventProducer)
                                        .publishResumeUploaded(
                                                        resumeResponse.getId(),
                                                        "resumes/test.pdf");

                        verify(sample)
                                        .stop(timer);
                }
        }

        @Test
        void testUploadResume_shouldSanitizeFilename() {

                UploadDTO dto = new UploadDTO();

                dto.setFileKey("key");

                dto.setFileName("my resume@2025.pdf");

                when(userService.findUserByEmail(user.getEmail()))
                                .thenReturn(user);

                when(rateLimitService.resolveBucket(
                                anyString(),
                                anyLong(),
                                any(Duration.class)))
                                .thenReturn(bucket);

                when(resumeService.saveParsedResume(
                                any(User.class),
                                anyString(),
                                anyString()))
                                .thenReturn(resumeResponse);

                when(meterRegistry.timer(anyString()))
                                .thenReturn(timer);

                try (MockedStatic<Timer> timerMock = Mockito.mockStatic(Timer.class);

                                MockedStatic<RateLimitUtil> rateLimitMock = Mockito.mockStatic(RateLimitUtil.class)) {

                        timerMock.when(() -> Timer.start(meterRegistry))
                                        .thenReturn(sample);

                        resumeController.uploadResume(dto);

                        verify(resumeService)
                                        .saveParsedResume(
                                                        user,
                                                        "key",
                                                        "my_resume_2025.pdf");
                }
        }

        @Test
        void testGetDownloadUrl_shouldReturnDownloadUrl() {

                when(userService.findUserByEmail(user.getEmail()))
                                .thenReturn(user);

                when(resumeService.getResumeById(resume.getId()))
                                .thenReturn(resume);

                when(s3Service.generateDownloadUrl(
                                "resumes/file.pdf"))
                                .thenReturn("https://download-url");

                ResponseEntity<Map<String, String>> response = resumeController.getDownloadUrl(
                                resume.getId());

                assertEquals(
                                HttpStatus.OK,
                                response.getStatusCode());

                assertEquals(
                                "https://download-url",
                                response.getBody()
                                                .get("downloadUrl"));

                verify(s3Service)
                                .generateDownloadUrl(
                                                "resumes/file.pdf");
        }

        @Test
        void testGetDownloadUrl_shouldThrowUnauthorized_whenNotOwner() {

                User anotherUser = new User();

                anotherUser.setId(UUID.randomUUID());

                resume.setUser(anotherUser);

                when(userService.findUserByEmail(user.getEmail()))
                                .thenReturn(user);

                when(resumeService.getResumeById(resume.getId()))
                                .thenReturn(resume);

                assertThrows(
                                UnauthorizedException.class,
                                () -> resumeController
                                                .getDownloadUrl(resume.getId()));

                verifyNoInteractions(s3Service);
        }

        @Test
        void testGetUserResumes_shouldReturnUserResumes() {

                when(userService.findUserByEmail(user.getEmail()))
                                .thenReturn(user);

                when(resumeService.getResumeForUser(user))
                                .thenReturn(List.of(resume));

                ResponseEntity<List<Resume>> response = resumeController.getUserResumes();

                assertEquals(
                                HttpStatus.OK,
                                response.getStatusCode());

                assertEquals(
                                1,
                                response.getBody().size());

                verify(resumeService)
                                .getResumeForUser(user);
        }
}