package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.config.JwtProvider;
import com.sabarno.hireflux.entity.RefreshToken;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.repository.RefreshTokenRepository;
import com.sabarno.hireflux.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService{

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Override
    public RefreshToken createRefreshToken(User user) {

        String token = jwtProvider.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(
                LocalDateTime.now().plusDays(7)
        );
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyRefreshToken(String token) {

        RefreshToken refreshToken =
                refreshTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid refresh token"
                                ));

        if (refreshToken.isRevoked()) {
            throw new BadRequestException("Refresh token revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token expired");
        }

        return refreshToken;
    }

    @Override
    public void revokeUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}