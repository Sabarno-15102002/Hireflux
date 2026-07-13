package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.dto.request.AdminInviteRequest;
import com.sabarno.hireflux.dto.request.CompleteInviteRequest;
import com.sabarno.hireflux.dto.response.DashboardAnalyticsResponse;
import com.sabarno.hireflux.entity.Invite;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.ConflictException;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.repository.InviteRepository;
import com.sabarno.hireflux.repository.JobApplicationRepository;
import com.sabarno.hireflux.repository.JobRepository;
import com.sabarno.hireflux.repository.UserRepository;
import com.sabarno.hireflux.service.AdminService;
import com.sabarno.hireflux.service.util.EmailService;
import com.sabarno.hireflux.utility.enums.AuthProvider;
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.SkillAnalyticsProjection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService{

    private final InviteRepository inviteRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final JobRepository jobRepository;

    private final JobApplicationRepository applicationRepository;

    @Override
    public void inviteUser(AdminInviteRequest request, User admin) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new BadRequestException(
                    "Only admins can invite users"
            );
        }

        if (
                request.getRole() != UserRole.ADMIN &&
                request.getRole() != UserRole.RECRUITER
        ) {
            throw new BadRequestException(
                    "Invalid invite role"
            );
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException(
                    "User already exists"
            );
        }

        String token = UUID.randomUUID().toString();

        Invite invite = new Invite();

        invite.setEmail(request.getEmail());
        invite.setRole(request.getRole());
        invite.setToken(token);
        invite.setAccepted(false);

        invite.setCreatedAt(LocalDateTime.now());

        invite.setExpiresAt(
                LocalDateTime.now().plusDays(3)
        );

        inviteRepository.save(invite);

        String inviteLink =
                "http://localhost:3000/invite/accept?token=" + token;

        emailService.sendInviteEmail(
                request.getEmail(),
                inviteLink
        );
    }

    @Override
    public void completeInvite(CompleteInviteRequest request) {
        Invite invite = inviteRepository
                .findByToken(request.getToken())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invalid invite token"
                        )
                );

        if (invite.isAccepted()) {
            throw new BadRequestException(
                    "Invite already used"
            );
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(
                    "Invite expired"
            );
        }

        User user = new User();

        user.setEmail(invite.getEmail());
        user.setName(request.getName());

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(invite.getRole());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        invite.setAccepted(true);
        inviteRepository.save(invite);
    }

    @Override
    public void updateUserRole(UUID userId, String newRole) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        UserRole role = UserRole.valueOf(newRole);

        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    public DashboardAnalyticsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalJobs = jobRepository.count();
        long totalApplications = applicationRepository.count();
        long activeRecruiters = userRepository.countRecruiters();

        return new DashboardAnalyticsResponse(
                totalUsers,
                totalJobs,
                totalApplications,
                activeRecruiters
        );

    }

    @Override
    public List<SkillAnalyticsProjection> getTopSkills() {
        return userRepository.getTopSkills();
    }

}
