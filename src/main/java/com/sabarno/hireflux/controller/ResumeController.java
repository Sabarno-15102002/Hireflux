package com.sabarno.hireflux.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.UploadDTO;
import com.sabarno.hireflux.dto.response.ResumeResponse;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.event.ResumeEventProducer;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.service.ResumeService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.service.util.S3Service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/resume")
@Tag(name = "Resume Controller", description = "APIs for managing resume uploads, downloads, and retrieval")
public class ResumeController {

    @Autowired
    private UserService userService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ResumeEventProducer resumeEventProducer;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userService.findUserByEmail(email);
    }

    // ✅ 1. Generate pre-signed upload URL
    @Operation(summary = "Generate pre-signed upload URL", description = "Generates a pre-signed URL for uploading a resume file to S3. Requires authentication.")
    @PostMapping("/presign")
    public ResponseEntity<Map<String, String>> generateUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType
    ) {

        User user = getCurrentUser();

        // Optional: validate file type
        if (!"application/pdf".equals(contentType)) {
            throw new BadRequestException("Only PDF files allowed");
        }

        String fileKey = "resumes/" + user.getId() + "/"  + fileName;

        String uploadUrl = s3Service.generateUploadUrl(fileKey, contentType);

        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", uploadUrl);
        response.put("fileKey", fileKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ 2. Confirm upload + trigger processing
    @Operation(summary = "Confirm resume upload and trigger processing", description = "Confirms that a resume has been uploaded to S3 and triggers asynchronous processing to extract information. Requires authentication.")
    @PostMapping("/upload")
    public ResponseEntity<ResumeResponse> uploadResume(
            @Valid @RequestBody UploadDTO uploadDTO
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        User user = getCurrentUser();

        String fileKey = uploadDTO.getFileKey();
        String fileName = uploadDTO.getFileName();
        String safeFileName = fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

        // Save initial record
        ResumeResponse resume = resumeService.saveParsedResume(user, fileKey, safeFileName);

        // Async processing (from S3)
        resumeEventProducer.publishResumeUploaded(resume.getId(), fileKey);
        sample.stop(meterRegistry.timer("resume.processing.time"));
        return ResponseEntity.status(HttpStatus.CREATED).body(resume);
    }

    // ✅ 3. Generate secure download URL
    @Operation(summary = "Generate secure download URL for resume", description = "Generates a secure pre-signed URL for downloading the user's resume from S3. Requires authentication and ownership of the resume.")
    @GetMapping("/{resumeId}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable UUID resumeId
    ) {

        User user = getCurrentUser();

        Resume resume = resumeService.getResumeById(resumeId);

        // 🔐 Ownership check
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized");
        }

        String downloadUrl = s3Service.generateDownloadUrl(resume.getFileKey());

        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }

    // ✅ 4. Get user's latest resume (optional helper)
    @Operation(summary = "Get user's latest resume", description = "Retrieves the latest resume submitted by the authenticated user. Requires authentication.")
    @GetMapping("/me")
    public ResponseEntity<List<Resume>> getUserResumes() {
        User user = getCurrentUser();
        List<Resume> resumes = resumeService.getResumeForUser(user);
        return ResponseEntity.ok(resumes);
    }
}