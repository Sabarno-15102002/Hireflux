package com.sabarno.hireflux.exception.impl;

import com.sabarno.hireflux.exception.AppException;

public class ConflictException extends AppException {
    public ConflictException(String message) {
        super(message);
    }
}