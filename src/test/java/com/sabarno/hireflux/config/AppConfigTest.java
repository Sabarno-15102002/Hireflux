package com.sabarno.hireflux.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppConfigTest {

    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig();
    }

    @Test
    void passwordEncoder_shouldReturnBCryptPasswordEncoder() {

        PasswordEncoder encoder = appConfig.passwordEncoder();

        assertNotNull(encoder);
        assertInstanceOf(BCryptPasswordEncoder.class, encoder);

        String encoded = encoder.encode("password");

        assertTrue(encoder.matches("password", encoded));
    }

    @Test
    void objectMapper_shouldReturnObjectMapper() {

        ObjectMapper objectMapper = appConfig.objectMapper();

        assertNotNull(objectMapper);
    }

    @Test
    void requestLoggingFilter_shouldReturnRequestLoggingFilter() {

        RequestLoggingFilter filter =
                appConfig.requestLoggingFilter();

        assertNotNull(filter);
        assertInstanceOf(RequestLoggingFilter.class, filter);
    }

    @Test
    void jwtTokenValidator_shouldReturnJwtTokenValidator() {

        JwtTokenValidator validator =
                appConfig.jwtTokenValidator();

        assertNotNull(validator);
        assertInstanceOf(JwtTokenValidator.class, validator);
    }

    @Test
    void transactionManager_shouldReturnJpaTransactionManager() {

        EntityManagerFactory entityManagerFactory =
                mock(EntityManagerFactory.class);

        PlatformTransactionManager manager =
                appConfig.transactionManager(entityManagerFactory);

        assertNotNull(manager);
        assertInstanceOf(JpaTransactionManager.class, manager);
    }
}
