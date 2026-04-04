package com.sabarno.hireflux.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sabarno.hireflux.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    User findByEmail(String email);
}
