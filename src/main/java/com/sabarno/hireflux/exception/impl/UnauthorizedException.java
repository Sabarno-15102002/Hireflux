package com.sabarno.hireflux.exception.impl;

import com.sabarno.hireflux.exception.AppException;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String message) {
        super(message);
    }
}