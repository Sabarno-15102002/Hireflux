package com.sabarno.hireflux.service.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.repository.UserRepository;
import com.sabarno.hireflux.utility.enums.AuthProvider;
import com.sabarno.hireflux.utility.enums.UserRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        User admin = new User();

        admin.setName("System Admin");
        admin.setEmail(adminEmail);

        admin.setPassword(
                passwordEncoder.encode(adminPassword));

        admin.setRole(UserRole.ADMIN);

        admin.setAuthProvider(AuthProvider.LOCAL);

        userRepository.save(admin);
    }
}