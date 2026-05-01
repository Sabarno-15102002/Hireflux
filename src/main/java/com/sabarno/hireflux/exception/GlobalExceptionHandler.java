package com.sabarno.hireflux.exception;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.ConflictException;
import com.sabarno.hireflux.exception.impl.FileProcessingException;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;

import io.micrometer.core.instrument.MeterRegistry;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private MeterRegistry meterRegistry;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetail> handleNotFound(ResourceNotFoundException ex) {
        ErrorDetail errorDetail = new ErrorDetail();
        errorDetail.setError("Not Found");
        errorDetail.setMessage(ex.getMessage());
        errorDetail.setTimestamp(LocalDateTime.now());
        meterRegistry.counter("exceptions.not_found").increment();
        return ResponseEntity.status(404).body(errorDetail);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorDetail> handleBadRequest(BadRequestException ex) {
        ErrorDetail errorDetail = new ErrorDetail();
        errorDetail.setError("Bad Request");
        errorDetail.setMessage(ex.getMessage());
        errorDetail.setTimestamp(LocalDateTime.now());
        meterRegistry.counter("exceptions.bad_request").increment();
        return ResponseEntity.status(400).body(errorDetail);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorDetail> handleUnauthorized(UnauthorizedException ex) {
        ErrorDetail errorDetail = new ErrorDetail();
        errorDetail.setError("Unauthorized");
        errorDetail.setMessage(ex.getMessage());
        errorDetail.setTimestamp(LocalDateTime.now());
        meterRegistry.counter("exceptions.unauthorized").increment();
        return ResponseEntity.status(403).body(errorDetail);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorDetail> handleConflict(ConflictException ex) {
        ErrorDetail errorDetail = new ErrorDetail();
        errorDetail.setError("Conflict");
        errorDetail.setMessage(ex.getMessage());
        errorDetail.setTimestamp(LocalDateTime.now());
        meterRegistry.counter("exceptions.conflict").increment();
        return ResponseEntity.status(409).body(errorDetail);
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ErrorDetail> handleFile(FileProcessingException ex) {
        ErrorDetail errorDetail = new ErrorDetail();
        errorDetail.setError("File Processing Error");
        errorDetail.setMessage(ex.getMessage());
        errorDetail.setTimestamp(LocalDateTime.now());
        meterRegistry.counter("exceptions.file_processing").increment();
        return ResponseEntity.status(500).body(errorDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetail> handleGeneric(Exception ex) {
        ErrorDetail errorDetail = new ErrorDetail();
        errorDetail.setError("Internal Server Error");
        errorDetail.setMessage("Something went wrong");
        errorDetail.setTimestamp(LocalDateTime.now());
        meterRegistry.counter("exceptions.internal_server_error").increment();
        return ResponseEntity.status(500).body(errorDetail);
    }
}
