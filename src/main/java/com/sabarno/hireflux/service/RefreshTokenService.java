package com.sabarno.hireflux.service;

import com.sabarno.hireflux.entity.RefreshToken;
import com.sabarno.hireflux.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyRefreshToken(String token);

    void revokeUserTokens(User user);

}
