package com.sabarno.hireflux.exception.impl;

import com.sabarno.hireflux.exception.AppException;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}