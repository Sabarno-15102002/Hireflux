package com.sabarno.hireflux.service.util;

import java.net.URL;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {
 
    @Mock
    private S3Presigner presigner;
 
    @Mock
    private S3Client s3Client;
 
    private S3Service s3Service;
 
    private static final String BUCKET_NAME = "hireflux-resumes";
 
    @BeforeEach
    void setUp() {
        s3Service = new S3Service(presigner, s3Client);
        ReflectionTestUtils.setField(s3Service, "bucketName", BUCKET_NAME);
    }
 
    @Test
    void testGenerateUploadUrl_shouldReturnPresignedUrlString() throws Exception {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        URL expectedUrl = new URL("https://hireflux-resumes.s3.amazonaws.com/uploads/resume.pdf?X-Amz-Signature=abc");
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);
 
        String result = s3Service.generateUploadUrl("uploads/resume.pdf", "application/pdf");
 
        assertEquals(expectedUrl.toString(), result);
    }
 
    @Test
    void testGenerateUploadUrl_shouldBuildRequestWithCorrectBucketKeyAndContentType() throws Exception {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/signed"));
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);
 
        s3Service.generateUploadUrl("uploads/resume.pdf", "application/pdf");
 
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(captor.capture());
 
        PutObjectRequest putRequest = captor.getValue().putObjectRequest();
        assertEquals(BUCKET_NAME, putRequest.bucket());
        assertEquals("uploads/resume.pdf", putRequest.key());
        assertEquals("application/pdf", putRequest.contentType());
    }
 
    @Test
    void testGenerateUploadUrl_shouldUseTenMinuteSignatureDuration() throws Exception {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/signed"));
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);
 
        s3Service.generateUploadUrl("uploads/resume.pdf", "application/pdf");
 
        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(captor.capture());
 
        assertEquals(Duration.ofMinutes(10), captor.getValue().signatureDuration());
    }
 
    @Test
    void testGenerateUploadUrl_shouldPropagateException_whenPresignerFails() {
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenThrow(SdkClientException.builder().message("AWS credentials invalid").build());
 
        assertThrows(SdkClientException.class,
                () -> s3Service.generateUploadUrl("uploads/resume.pdf", "application/pdf"));
    }
 
    @Test
    void testGenerateDownloadUrl_shouldReturnPresignedUrlString() throws Exception {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        URL expectedUrl = new URL("https://hireflux-resumes.s3.amazonaws.com/uploads/resume.pdf?X-Amz-Signature=xyz");
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
 
        String result = s3Service.generateDownloadUrl("uploads/resume.pdf");
 
        assertEquals(expectedUrl.toString(), result);
    }
 
    @Test
    void testGenerateDownloadUrl_shouldBuildRequestWithCorrectBucketAndKey() throws Exception {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/signed"));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
 
        s3Service.generateDownloadUrl("uploads/resume.pdf");
 
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(captor.capture());
 
        GetObjectRequest getRequest = captor.getValue().getObjectRequest();
        assertEquals(BUCKET_NAME, getRequest.bucket());
        assertEquals("uploads/resume.pdf", getRequest.key());
    }
 
    @Test
    void testGenerateDownloadUrl_shouldUseFiveMinuteSignatureDuration() throws Exception {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/signed"));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
 
        s3Service.generateDownloadUrl("uploads/resume.pdf");
 
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(captor.capture());
 
        assertEquals(Duration.ofMinutes(5), captor.getValue().signatureDuration());
    }
 
    @Test
    void testGenerateDownloadUrl_shouldPropagateException_whenPresignerFails() {
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(SdkClientException.builder().message("AWS credentials invalid").build());
 
        assertThrows(SdkClientException.class, () -> s3Service.generateDownloadUrl("uploads/resume.pdf"));
    }
 
    @Test
    @SuppressWarnings("unchecked")
    void testGetObject_shouldReturnInputStream_fromS3Client() {
        ResponseInputStream<GetObjectResponse> responseStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);
 
        var result = s3Service.getObject("uploads/resume.pdf");
 
        assertSame(responseStream, result);
    }
 
    @Test
    void testGetObject_shouldBuildRequestWithCorrectBucketAndKey() {
        ResponseInputStream<GetObjectResponse> responseStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);
 
        s3Service.getObject("uploads/resume.pdf");
 
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());
 
        assertEquals(BUCKET_NAME, captor.getValue().bucket());
        assertEquals("uploads/resume.pdf", captor.getValue().key());
    }
 
    @Test
    void testGetObject_shouldPropagateException_whenS3ClientFails() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(SdkClientException.builder().message("S3 unreachable").build());
 
        assertThrows(SdkClientException.class, () -> s3Service.getObject("uploads/resume.pdf"));
    }
}