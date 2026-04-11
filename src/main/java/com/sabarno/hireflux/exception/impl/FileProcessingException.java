package com.sabarno.hireflux.exception.impl;

import com.sabarno.hireflux.exception.AppException;

public class FileProcessingException extends AppException {
    public FileProcessingException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
