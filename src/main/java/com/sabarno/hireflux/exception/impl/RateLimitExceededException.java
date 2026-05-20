package com.sabarno.hireflux.exception.impl;

import com.sabarno.hireflux.exception.AppException;

public class RateLimitExceededException extends AppException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
