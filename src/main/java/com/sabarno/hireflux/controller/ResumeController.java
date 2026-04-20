package com.sabarno.hireflux.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.UploadDTO;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.service.ResumeService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.service.util.S3Service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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

    // ✅ 1. Generate pre-signed upload URL
    @Operation(summary = "Generate pre-signed upload URL", description = "Generates a pre-signed URL for uploading a resume file to S3. Requires authentication.")
    @PostMapping("/presign")
    public ResponseEntity<Map<String, String>> generateUploadUrl(
            @RequestHeader("Authorization") String token,
            @RequestParam String fileName,
            @RequestParam String contentType
    ) {

        User user = userService.findUserFromToken(token);

        // Optional: validate file type
        if (!"application/pdf".equals(contentType)) {
            throw new BadRequestException("Only PDF files allowed");
        }

        String fileKey = "resumes"+File.separator + user.getId() + File.separator  + fileName;

        String uploadUrl = s3Service.generateUploadUrl(fileKey, contentType);

        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", uploadUrl);
        response.put("fileKey", fileKey);

        return ResponseEntity.ok(response);
    }

    // ✅ 2. Confirm upload + trigger processing
    @Operation(summary = "Confirm resume upload and trigger processing", description = "Confirms that a resume has been uploaded to S3 and triggers asynchronous processing to extract information. Requires authentication.")
    @PostMapping("/upload")
    public ResponseEntity<Resume> uploadResume(
            @RequestHeader("Authorization") String token,
            @RequestBody UploadDTO uploadDTO
    ) {

        User user = userService.findUserFromToken(token);

        String fileKey = uploadDTO.getFileKey();
        String fileName = uploadDTO.getFileName();

        // Save initial record
        Resume resume = resumeService.saveParsedResume(user, fileKey, fileName);

        // Async processing (from S3)
        resumeService.processResumeAsync(resume.getId(), fileKey);

        return ResponseEntity.ok(resume);
    }

    // ✅ 3. Generate secure download URL
    @Operation(summary = "Generate secure download URL for resume", description = "Generates a secure pre-signed URL for downloading the user's resume from S3. Requires authentication and ownership of the resume.")
    @GetMapping("/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @RequestHeader("Authorization") String token,
            @RequestParam UUID resumeId
    ) {

        User user = userService.findUserFromToken(token);

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
    public ResponseEntity<Resume> getMyResume(
            @RequestHeader("Authorization") String token
    ) {
        User user = userService.findUserFromToken(token);
        Resume resume = resumeService.getResumeForUser(user);
        return ResponseEntity.ok(resume);
    }
}