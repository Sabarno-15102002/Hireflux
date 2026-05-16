package com.sabarno.hireflux.exception;

public class RetryableProcessingException extends RuntimeException {

    public RetryableProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}