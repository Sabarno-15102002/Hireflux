package com.sabarno.hireflux.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.ConflictException;
import com.sabarno.hireflux.exception.impl.FileProcessingException;
import com.sabarno.hireflux.exception.impl.RateLimitExceededException;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
 
    private MeterRegistry meterRegistry;
    private GlobalExceptionHandler handler;
 
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        handler = new GlobalExceptionHandler(meterRegistry);
    }
 
    @Test
    void testHandleNotFound_shouldReturn404_withMessageAndIncrementCounter() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Job not found");
 
        ResponseEntity<ErrorDetail> response = handler.handleNotFound(ex);
 
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Job not found", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(1.0, meterRegistry.counter("exceptions.not_found").count());
    }
 
    @Test
    void testHandleBadRequest_shouldReturn400_withMessageAndIncrementCounter() {
        BadRequestException ex = new BadRequestException("Invalid invite role");
 
        ResponseEntity<ErrorDetail> response = handler.handleBadRequest(ex);
 
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid invite role", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(1.0, meterRegistry.counter("exceptions.bad_request").count());
    }
 
    @Test
    void testHandleUnauthorized_shouldReturn401_withMessageAndIncrementCounter() {
        UnauthorizedException ex = new UnauthorizedException("Only admins can invite users");
 
        ResponseEntity<ErrorDetail> response = handler.handleUnauthorized(ex);
 
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Only admins can invite users", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(1.0, meterRegistry.counter("exceptions.unauthorized").count());
    }
 
    @Test
    void testHandleConflict_shouldReturn409_withMessageAndIncrementCounter() {
        ConflictException ex = new ConflictException("User already exists");
 
        ResponseEntity<ErrorDetail> response = handler.handleConflict(ex);
 
        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("User already exists", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(1.0, meterRegistry.counter("exceptions.conflict").count());
    }
 
    @Test
    void testHandleFile_shouldReturn500_withMessageAndIncrementCounter() {
        FileProcessingException ex = new FileProcessingException("Failed to save resume", new RuntimeException());
 
        ResponseEntity<ErrorDetail> response = handler.handleFile(ex);
 
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("File Processing Error", response.getBody().getError());
        assertEquals("Failed to save resume", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(1.0, meterRegistry.counter("exceptions.file_processing").count());
    }
 
    @Test
    void testHandleRateLimit_shouldReturn429_withMessageAndIncrementCounter() {
        RateLimitExceededException ex = new RateLimitExceededException("Too many requests");
 
        ResponseEntity<ErrorDetail> response = handler.handleRateLimit(ex);
 
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Rate Limit Exceeded", response.getBody().getError());
        assertEquals("Too many requests", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(1.0, meterRegistry.counter("exceptions.rate_limit_exceeded").count());
    }
 
    @Test
    void testHandleGeneric_shouldReturn500_withGenericMessage_regardlessOfActualExceptionMessage() {

        Exception ex = new RuntimeException("NullPointerException at line 42 in InternalService.java");
 
        ResponseEntity<ErrorDetail> response = handler.handleGeneric(ex);
 
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("Something went wrong", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals(1.0, meterRegistry.counter("exceptions.internal_server_error").count());
    }
 
    @Test
    void testHandleGeneric_shouldCatchAnyThrowableSubtype_notJustRuntimeException() {
        
        Exception ex = new java.io.IOException("Unexpected IO failure");
 
        ResponseEntity<ErrorDetail> response = handler.handleGeneric(ex);
 
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertEquals("Something went wrong", response.getBody().getMessage());
    }
 
    @Test
    void eachHandler_shouldIncrementOnlyItsOwnCounter_notOthers() {
        handler.handleNotFound(new ResourceNotFoundException("x"));
 
        assertEquals(1.0, meterRegistry.counter("exceptions.not_found").count());
        assertEquals(0.0, meterRegistry.counter("exceptions.bad_request").count());
        assertEquals(0.0, meterRegistry.counter("exceptions.unauthorized").count());
        assertEquals(0.0, meterRegistry.counter("exceptions.conflict").count());
        assertEquals(0.0, meterRegistry.counter("exceptions.file_processing").count());
        assertEquals(0.0, meterRegistry.counter("exceptions.rate_limit_exceeded").count());
        assertEquals(0.0, meterRegistry.counter("exceptions.internal_server_error").count());
    }
}