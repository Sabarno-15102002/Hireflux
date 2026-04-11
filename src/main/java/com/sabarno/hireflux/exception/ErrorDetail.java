package com.sabarno.hireflux.exception;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ErrorDetail {
    private String error;
    private String message;
    private LocalDateTime timestamp;
}