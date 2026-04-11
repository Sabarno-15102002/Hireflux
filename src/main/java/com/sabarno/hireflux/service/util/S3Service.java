package com.sabarno.hireflux.service.util;

import java.io.InputStream;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3Service {

    @Autowired
    private S3Presigner presigner;

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    // ✅ 1. Upload (pre-signed PUT)
    public String generateUploadUrl(String fileKey, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(objectRequest)
                        .build();

        return presigner.presignPutObject(presignRequest)
                .url()
                .toString();
    }

    // ✅ 2. Download (pre-signed GET)
    public String generateDownloadUrl(String fileKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(getObjectRequest)
                        .build();

        return presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    // ✅ 3. Internal processing (IMPORTANT for async parsing)
    public InputStream getObject(String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        return s3Client.getObject(request);
    }
}