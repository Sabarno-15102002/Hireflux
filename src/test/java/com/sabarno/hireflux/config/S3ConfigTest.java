package com.sabarno.hireflux.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3ConfigTest {

        @Test
        void tests3Client_shouldCreateClient_whenCredentialsProviderProvided() {

                S3Config config = new S3Config();

                AwsCredentialsProvider credentialsProvider = mock(AwsCredentialsProvider.class);

                S3Client s3Client = config.s3Client(credentialsProvider);

                assertNotNull(s3Client);

                s3Client.close();
        }

        @Test
        void tests3Presigner_shouldCreatePresigner_whenCredentialsProviderProvided() {

                S3Config config = new S3Config();

                AwsCredentialsProvider credentialsProvider = mock(AwsCredentialsProvider.class);

                S3Presigner s3Presigner = config.s3Presigner(credentialsProvider);

                assertNotNull(s3Presigner);

                s3Presigner.close();
        }

        @Test
        void testcredentialsProvider_shouldCreateProvider() {

                S3Config config = new S3Config();

                AwsCredentialsProvider provider = config.credentialsProvider();

                assertNotNull(provider);
        }
}