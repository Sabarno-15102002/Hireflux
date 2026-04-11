package com.sabarno.hireflux.exception.impl;

import com.sabarno.hireflux.exception.AppException;

public class BadRequestException extends AppException {
    public BadRequestException(String message) {
        super(message);
    }
}
