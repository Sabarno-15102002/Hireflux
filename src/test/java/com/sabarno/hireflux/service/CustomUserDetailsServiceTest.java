package com.sabarno.hireflux.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.repository.UserRepository;
import com.sabarno.hireflux.utility.enums.UserRole;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
 
    @Mock
    private UserRepository userRepository;
 
    @InjectMocks
    private CustomUserService userDetailsService;
 
    private User user;
 
    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("encodedPassword123");
        user.setRole(UserRole.RECRUITER);
    }
 
    @Test
    void testLoadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
 
        UserDetails result = userDetailsService.loadUserByUsername("jane@example.com");
 
        assertNotNull(result);
        assertEquals("jane@example.com", result.getUsername());
        assertEquals("encodedPassword123", result.getPassword());
        verify(userRepository, times(1)).findByEmail("jane@example.com");
    }
 
    @Test
    void testLoadUserByUsername_shouldAssignRolePrefixedAuthority_matchingUserRole() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
 
        UserDetails result = userDetailsService.loadUserByUsername("jane@example.com");
 
        assertEquals(1, result.getAuthorities().size());
        assertTrue(result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_RECRUITER")));
    }
 
    @Test
    void testLoadUserByUsername_shouldAssignCorrectAuthority_forEachRole() {
        for (UserRole role : UserRole.values()) {
            user.setRole(role);
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
 
            UserDetails result = userDetailsService.loadUserByUsername("jane@example.com");
 
            assertEquals(1, result.getAuthorities().size());
            assertTrue(result.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equals("ROLE_" + role.name())));
        }
    }
 
    @Test
    void testLoadUserByUsername_shouldThrowUsernameNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
 
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@example.com")
        );
 
        assertEquals("User Not Found with email: missing@example.com", exception.getMessage());
    }
 
    @Test
    void testLoadUserByUsername_shouldQueryRepositoryWithExactUsernameProvided() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
 
        userDetailsService.loadUserByUsername("Jane@Example.com");
        verify(userRepository).findByEmail("Jane@Example.com");
    }
 
    @Test
    void testLoadUserByUsername_shouldReturnAccountFlagsAsEnabled_byDefault() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
 
        UserDetails result = userDetailsService.loadUserByUsername("jane@example.com");
 
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isCredentialsNonExpired());
    }
}