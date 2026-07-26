package com.sabarno.hireflux.service.util;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.repository.UserRepository;
import com.sabarno.hireflux.utility.enums.AuthProvider;
import com.sabarno.hireflux.utility.enums.UserRole;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {
 
    @Mock
    private UserRepository userRepository;
 
    @Mock
    private PasswordEncoder passwordEncoder;
 
    private AdminSeeder adminSeeder;
 
    private static final String ADMIN_EMAIL = "admin@hireflux.com";
    private static final String ADMIN_PASSWORD = "rawAdminPassword123";
 
    @BeforeEach
    void setUp() {
        adminSeeder = new AdminSeeder(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(adminSeeder, "adminEmail", ADMIN_EMAIL);
        ReflectionTestUtils.setField(adminSeeder, "adminPassword", ADMIN_PASSWORD);
    }
 
    @Test
    void testRun_shouldDoNothing_whenAdminAlreadyExists() throws Exception {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(new User()));
 
        adminSeeder.run();
 
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
 
    @Test
    void testRun_shouldCreateAdminUser_whenNoAdminExists() throws Exception {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn("encodedPassword");
 
        adminSeeder.run();
 
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedAdmin = userCaptor.getValue();
 
        assertEquals("System Admin", savedAdmin.getName());
        assertEquals(ADMIN_EMAIL, savedAdmin.getEmail());
        assertEquals("encodedPassword", savedAdmin.getPassword());
        assertEquals(UserRole.ADMIN, savedAdmin.getRole());
        assertEquals(AuthProvider.LOCAL, savedAdmin.getAuthProvider());
    }
 
    @Test
    void testRun_shouldEncodeRawPassword_beforeSaving() throws Exception {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn("encodedPassword");
 
        adminSeeder.run();
 
        verify(passwordEncoder, times(1)).encode(ADMIN_PASSWORD);
 
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        // The raw password should never end up stored directly.
        assertNotEquals(ADMIN_PASSWORD, userCaptor.getValue().getPassword());
    }
 
    @Test
    void testRun_shouldCheckForExistingAdmin_byConfiguredEmail() throws Exception {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
 
        adminSeeder.run();
 
        verify(userRepository, times(1)).findByEmail(ADMIN_EMAIL);
    }
 
    @Test
    void testRun_shouldPropagateException_whenSaveFails() throws Exception {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));
 
        assertThrows(RuntimeException.class, () -> adminSeeder.run());
    }
 
    @Test
    void testRun_shouldAcceptVarargsArgs_withoutUsingThem() throws Exception {
        // CommandLineRunner#run(String...) receives application startup
        // args, which this class never reads -- confirms passing args
        // doesn't break anything.
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn("encodedPassword");
 
        assertDoesNotThrow(() -> adminSeeder.run("--spring.profiles.active=test", "--debug"));
 
        verify(userRepository).save(any(User.class));
    }
}