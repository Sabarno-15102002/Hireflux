package com.sabarno.hireflux.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sabarno.hireflux.config.JwtProvider;
import com.sabarno.hireflux.entity.RefreshToken;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.repository.RefreshTokenRepository;
import com.sabarno.hireflux.service.impl.RefreshTokenServiceImpl;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Test
    void testCreateRefreshToken() {
        
        User user = new User();
        String token = "sample-refresh-token";

        when(jwtProvider.generateRefreshToken(user)).thenReturn(token);

        LocalDateTime before = LocalDateTime.now();
        refreshTokenService.createRefreshToken(user);
        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken savedRefreshToken = refreshTokenCaptor.getValue();
        assertEquals(token, savedRefreshToken.getToken());
        assertEquals(user, savedRefreshToken.getUser());
        assertEquals(false, savedRefreshToken.isRevoked());
        assertTrue(savedRefreshToken.getExpiryDate().isAfter(before.plusDays(7).minusSeconds(1)));
        assertTrue(savedRefreshToken.getExpiryDate().isBefore(after.plusDays(7).plusSeconds(1)));
    }

    @Test
    void testVerifyRefreshToken_Success() {
        String token = "valid-refresh-token";

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setRevoked(false);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.verifyRefreshToken(token);
        assertEquals(refreshToken, result);
    }

    @Test
    void testVerifyRefreshToken_InvalidToken() {
        String token = "invalid-refresh-token";

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            refreshTokenService.verifyRefreshToken(token);
        });
        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void testVerifyRefreshToken_RevokedToken() {
        String token = "revoked-refresh-token";

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setRevoked(true);

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            refreshTokenService.verifyRefreshToken(token);
        });
        assertEquals("Refresh token revoked", exception.getMessage());
    }

    @Test
    void testVerifyRefreshToken_ExpiredToken() {
        String token = "expired-refresh-token";

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setRevoked(false);
        refreshToken.setExpiryDate(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            refreshTokenService.verifyRefreshToken(token);
        });
        assertEquals("Refresh token expired", exception.getMessage());
    }

    @Test
    void testRevokeUserTokens() {
        User user = new User();
        refreshTokenService.revokeUserTokens(user);
        verify(refreshTokenRepository).deleteByUser(user);
    }
}
