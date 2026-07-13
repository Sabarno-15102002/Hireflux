package com.sabarno.hireflux.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import com.sabarno.hireflux.service.impl.AdminServiceImpl;
import com.sabarno.hireflux.service.util.EmailService;
import com.sabarno.hireflux.utility.enums.AuthProvider;
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.SkillAnalyticsProjection;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @InjectMocks
    private AdminServiceImpl adminService;

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobApplicationRepository applicationRepository;

    @Test
    void testInviteUser_Success_ForAdmin() {
        AdminInviteRequest request = new AdminInviteRequest();
        request.setEmail("recruiter@test.com");
        request.setRole(UserRole.ADMIN);

        User admin = new User();
        admin.setRole(UserRole.ADMIN);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        adminService.inviteUser(request, admin);

        ArgumentCaptor<Invite> inviteCaptor =
                ArgumentCaptor.forClass(Invite.class);

        verify(inviteRepository).save(inviteCaptor.capture());

        Invite savedInvite = inviteCaptor.getValue();

        assertEquals("recruiter@test.com", savedInvite.getEmail());
        assertEquals(UserRole.ADMIN, savedInvite.getRole());
        assertFalse(savedInvite.isAccepted());

        assertNotNull(savedInvite.getToken());
        assertNotNull(savedInvite.getCreatedAt());
        assertNotNull(savedInvite.getExpiresAt());

        verify(emailService).sendInviteEmail(
                eq("recruiter@test.com"),
                contains(savedInvite.getToken())
        );
    }
    @Test
    void testInviteUser_Success_ForRecruiter() {
        AdminInviteRequest request = new AdminInviteRequest();
        request.setEmail("recruiter@test.com");
        request.setRole(UserRole.RECRUITER);

        User admin = new User();
        admin.setRole(UserRole.ADMIN);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        adminService.inviteUser(request, admin);

        ArgumentCaptor<Invite> inviteCaptor =
                ArgumentCaptor.forClass(Invite.class);

        verify(inviteRepository).save(inviteCaptor.capture());

        Invite savedInvite = inviteCaptor.getValue();

        assertEquals("recruiter@test.com", savedInvite.getEmail());
        assertEquals(UserRole.RECRUITER, savedInvite.getRole());
        assertFalse(savedInvite.isAccepted());

        assertNotNull(savedInvite.getToken());
        assertNotNull(savedInvite.getCreatedAt());
        assertNotNull(savedInvite.getExpiresAt());

        verify(emailService).sendInviteEmail(
                eq("recruiter@test.com"),
                contains(savedInvite.getToken())
        );
    }

    @Test
    void testInviteUser_InvokerNotAdmin() {
        // Arrange
        AdminInviteRequest request = new AdminInviteRequest();
        request.setEmail("user@test.com");
        request.setRole(UserRole.RECRUITER);

        User recruiter = new User();
        recruiter.setRole(UserRole.RECRUITER);

        // Act + Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> adminService.inviteUser(request, recruiter)
        );

        assertEquals(
                "Only admins can invite users",
                exception.getMessage()
        );

        verifyNoInteractions(inviteRepository, emailService);
    }

    @Test
    void testInviteUser_InvalidRole() {
        // Arrange
        AdminInviteRequest request = new AdminInviteRequest();
        request.setEmail("user@test.com");
        request.setRole(UserRole.CANDIDATE); // Invalid role for invitation

        User admin = new User();
        admin.setRole(UserRole.ADMIN);

        // Act + Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> adminService.inviteUser(request, admin)
        );

        assertEquals(
                "Invalid invite role",
                exception.getMessage()
        );

        verifyNoInteractions(inviteRepository, emailService);
    }

    @Test
    void testInviteUser_UserAlreadyExists() {
        AdminInviteRequest request = new AdminInviteRequest();
        request.setEmail("recruiter@test.com");
        request.setRole(UserRole.RECRUITER);

        User admin = new User();
        admin.setRole(UserRole.ADMIN);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(new User()));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> adminService.inviteUser(request, admin)
        );

        assertEquals(
                "User already exists",
                exception.getMessage()
        );

        verifyNoInteractions(inviteRepository, emailService);
    }

    @Test
    void testCompleteInvite_Success() {
        CompleteInviteRequest request = new CompleteInviteRequest();
        request.setToken("invite-token");
        request.setName("John Doe");
        request.setPassword("password123");

        Invite invite = new Invite();
        invite.setToken("invite-token");
        invite.setEmail("john@example.com");
        invite.setRole(UserRole.RECRUITER);
        invite.setAccepted(false);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(inviteRepository.findByToken("invite-token"))
                .thenReturn(Optional.of(invite));

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encodedPassword");

        adminService.completeInvite(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
 
        assertEquals(invite.getEmail(), savedUser.getEmail());
        assertEquals(request.getName(), savedUser.getName());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(invite.getRole(), savedUser.getRole());
        assertEquals(AuthProvider.LOCAL, savedUser.getAuthProvider());
        assertNotNull(savedUser.getCreatedAt());
 
        // Verify invite updated and saved
        assertTrue(invite.isAccepted());
        verify(inviteRepository).save(invite);
        verify(passwordEncoder).encode(request.getPassword());
    }

    @Test
    void testCompleteInvite_InvalidToken() {
        CompleteInviteRequest request = new CompleteInviteRequest();
        request.setToken("invalid-token");
        request.setName("John Doe");
        request.setPassword("password123");

        when(inviteRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> adminService.completeInvite(request)
        );

        assertEquals(
                "Invalid invite token",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void testCompleteInvite_AlreadyAccepted() {
        CompleteInviteRequest request = new CompleteInviteRequest();
        request.setToken("invite-token");
        request.setName("John Doe");
        request.setPassword("password123");

        Invite invite = new Invite();
        invite.setToken("invite-token");
        invite.setEmail("john@example.com");
        invite.setRole(UserRole.RECRUITER);
        invite.setAccepted(true);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(inviteRepository.findByToken("invite-token"))
                .thenReturn(Optional.of(invite));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> adminService.completeInvite(request)
        );

        assertEquals(
                "Invite already used",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void testCompleteInvite_ExpiredInvite() {
        CompleteInviteRequest request = new CompleteInviteRequest();
        request.setToken("invite-token");
        request.setName("John Doe");
        request.setPassword("password123");

        Invite invite = new Invite();
        invite.setToken("invite-token");
        invite.setEmail("john@example.com");
        invite.setRole(UserRole.RECRUITER);
        invite.setAccepted(false);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setExpiresAt(LocalDateTime.now().minusDays(1)); // Set expiration in the past

        when(inviteRepository.findByToken("invite-token"))
                .thenReturn(Optional.of(invite));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> adminService.completeInvite(request)
        );

        assertEquals(
                "Invite expired",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void testUpdateUserRole_Success_RoleUpdateToAdmin() {
        User user = new User();
        UUID uuid = UUID.randomUUID();
        user.setId(uuid);
        user.setEmail("john@example.com");
        user.setRole(UserRole.RECRUITER);

        when(userRepository.findById(uuid)).thenReturn(Optional.of(user));

        adminService.updateUserRole(uuid, UserRole.ADMIN.toString());
        verify(userRepository).save(user);
        assertEquals(UserRole.ADMIN, user.getRole());
    }

    @Test
    void testUpdateUserRole_Success_RoleUpdateToCandidate() {
        User user = new User();
        UUID uuid = UUID.randomUUID();
        user.setId(uuid);
        user.setEmail("john@example.com");
        user.setRole(UserRole.ADMIN);

        when(userRepository.findById(uuid)).thenReturn(Optional.of(user));

        adminService.updateUserRole(uuid, UserRole.CANDIDATE.toString());
        verify(userRepository).save(user);
        assertEquals(UserRole.CANDIDATE, user.getRole());
    }

    @Test
    void testUpdateUserRole_Success_RoleUpdateToRecruiter() {
        User user = new User();
        UUID uuid = UUID.randomUUID();
        user.setId(uuid);
        user.setEmail("john@example.com");
        user.setRole(UserRole.CANDIDATE);

        when(userRepository.findById(uuid)).thenReturn(Optional.of(user));

        adminService.updateUserRole(uuid, UserRole.RECRUITER.toString());
        verify(userRepository).save(user);
        assertEquals(UserRole.RECRUITER, user.getRole());
    }

    @Test
    void testUpdateUserRole_UserNotFound() {
        UUID uuid = UUID.randomUUID();

        when(userRepository.findById(uuid)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> adminService.updateUserRole(uuid, UserRole.ADMIN.toString())
        );

        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }

    @Test
    void testGetDashboardStats() {
        // Arrange
        when(userRepository.count()).thenReturn(150L);
        when(jobRepository.count()).thenReturn(42L);
        when(applicationRepository.count()).thenReturn(378L);
        when(userRepository.countRecruiters()).thenReturn(12L);
 
        // Act
        DashboardAnalyticsResponse result = adminService.getDashboardStats();
 
        // Assert
        assertNotNull(result);
        assertEquals(150L, result.getTotalUsers());
        assertEquals(42L, result.getTotalJobs());
        assertEquals(378L, result.getTotalApplications());
        assertEquals(12L, result.getActiveRecruiters());
 
        verify(userRepository, times(1)).count();
        verify(jobRepository, times(1)).count();
        verify(applicationRepository, times(1)).count();
        verify(userRepository, times(1)).countRecruiters();
        verifyNoMoreInteractions(userRepository, jobRepository, applicationRepository);
    }

    @Test
    void testGetTopSkills() {
        // Arrange
        SkillAnalyticsProjection skill1 = new SkillAnalyticsProjection() {
            @Override
            public String getSkill() {
                return "Java";
            }

            @Override
            public Long getCount() {
                return 50L;
            }
        };

        SkillAnalyticsProjection skill2 = new SkillAnalyticsProjection() {
            @Override
            public String getSkill() {
                return "Python";
            }

            @Override
            public Long getCount() {
                return 30L;
            }
        };

        when(userRepository.getTopSkills()).thenReturn(List.of(skill1, skill2));

        // Act
        List<SkillAnalyticsProjection> result = adminService.getTopSkills();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Java", result.get(0).getSkill());
        assertEquals(50L, result.get(0).getCount());
        assertEquals("Python", result.get(1).getSkill());
        assertEquals(30L, result.get(1).getCount());

        verify(userRepository, times(1)).getTopSkills();
        verifyNoMoreInteractions(userRepository);
    }
}