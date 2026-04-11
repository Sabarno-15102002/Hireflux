package com.sabarno.hireflux.service.impl;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.FileProcessingException;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.repository.ResumeRepository;
import com.sabarno.hireflux.service.ResumeService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.service.util.OpenAIService;
import com.sabarno.hireflux.service.util.S3Service;
import com.sabarno.hireflux.utility.ResumeUploadStatus;

@Service
public class ResumeServiceImpl implements ResumeService {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Override
    public Resume saveParsedResume(User user, String fileKey, String fileName) {
        try {
            Optional<Resume> existing = resumeRepository.findByFileKey(fileKey);

            if (existing.isPresent()) {
                return existing.get(); // ✅ idempotent return
            }
            Resume resume = new Resume();
            resume.setUser(user);
            resume.setFileKey(fileKey);
            resume.setFileName(fileName);
            resume.setUploadStatus(ResumeUploadStatus.UPLOADED);

            userService.addResume(resume);
            return resumeRepository.save(resume);
        } catch (Exception e) {
            throw new FileProcessingException("Failed to save resume", e);
        }

    }

    @Override
    public Resume getResumeForUser(User user) {
        return resumeRepository.findByUser(user);
    }

    @Override
    @Transactional
    @Async
    public void processResumeAsync(UUID resumeId, String fileKey) {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        try {
            // Step 1: PROCESSING
            resume.setUploadStatus(ResumeUploadStatus.PROCESSING);
            resumeRepository.save(resume);

            // Step 2: Download file from S3
            InputStream inputStream = s3Service.getObject(fileKey);

            // Step 3: Extract text
            String text = extractText(inputStream);

            // Step 4: AI parsing
            ResumeParsedData data = parseResumeWithAI(text);

            // Step 5: Save result
            resume.setParsedData(objectMapper.writeValueAsString(data));
            resume.setUploadStatus(ResumeUploadStatus.PROCESSED);

        } catch (Exception e) {
            // Step 5: Handle failure
            resume.setUploadStatus(ResumeUploadStatus.FAILED);
            resume.setErrorMessage(
                e.getClass().getSimpleName() + ": " + e.getMessage()
            );
        } finally {
            resumeRepository.save(resume);
        }
    }

    private String extractText(InputStream inputStream) {
        try {
            Tika tika = new Tika();
            String text = tika.parseToString(inputStream);

            if (text == null || text.isBlank()) {
                throw new BadRequestException("Empty resume content");
            }

            return text;

        } catch (Exception e) {
            throw new FileProcessingException("Failed to extract text", e);
        }
    }

    private ResumeParsedData parseResumeWithAI(String text) {
        try {
            String json = openAIService.parseResume(text);

            return objectMapper.readValue(json, ResumeParsedData.class);

        } catch (Exception e) {
            throw new FileProcessingException("Failed to parse resume with AI", e);
        }
    }

    @Override
    public Resume getResumeById(UUID resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
    }

}
