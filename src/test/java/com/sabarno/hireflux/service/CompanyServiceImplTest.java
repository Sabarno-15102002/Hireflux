package com.sabarno.hireflux.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sabarno.hireflux.dto.request.CompanyRequest;
import com.sabarno.hireflux.dto.response.CompanyResponse;
import com.sabarno.hireflux.entity.Company;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.repository.CompanyRepository;
import com.sabarno.hireflux.repository.UserRepository;
import com.sabarno.hireflux.service.impl.CompanyServiceImpl;
import com.sabarno.hireflux.utility.enums.UserRole;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void testCreateCompany_Success() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Acme Corp");
        request.setWebsite("https://acme.example.com");
        request.setDescription("We build widgets.");

        User user = new User();
        user.setRole(UserRole.RECRUITER);
        user.setId(UUID.randomUUID());
        
        LocalDateTime before = LocalDateTime.now();
 
        companyService.createCompany(request, user);
 
        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        Company savedCompany = companyCaptor.getValue();
 
        assertEquals(request.getName(), savedCompany.getName());
        assertEquals(request.getWebsite(), savedCompany.getWebsite());
        assertEquals(request.getDescription(), savedCompany.getDescription());
        assertNotNull(savedCompany.getCreatedAt());
        assertFalse(savedCompany.getCreatedAt().isBefore(before.minusSeconds(2)));
    }

    @Test
    void testCreateCompany_ReturnMappedResponse() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Acme Corp");
        request.setWebsite("https://acme.example.com");
        request.setDescription("We build widgets.");

        User user = new User();
        user.setRole(UserRole.RECRUITER);
        user.setId(UUID.randomUUID());

        CompanyResponse response = companyService.createCompany(request, user);

        assertEquals(request.getName(), response.getName());
        assertEquals(request.getWebsite(), response.getWebsite());
        assertEquals(request.getDescription(), response.getDescription());
    }

    @Test
    void testCreateCompany_UnauthorizedForNonRecruiter() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Acme Corp");
        request.setWebsite("https://acme.example.com");
        request.setDescription("We build widgets.");

        User user = new User();
        user.setRole(UserRole.CANDIDATE);
        user.setId(UUID.randomUUID());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> companyService.createCompany(request, user)
        );
 
        assertEquals("Only recruiters can create companies", exception.getMessage());
        verifyNoInteractions(companyRepository, userRepository);
    }

    @Test
    void testGetCompany_Success() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);
        company.setName("Acme Corp");
        company.setWebsite("https://acme.example.com");
        company.setDescription("We build widgets.");
 
        when(companyRepository.findById(companyId)).thenReturn(java.util.Optional.of(company));
 
        CompanyResponse response = companyService.getCompany(companyId);
 
        assertEquals(company.getName(), response.getName());
        assertEquals(company.getWebsite(), response.getWebsite());
        assertEquals(company.getDescription(), response.getDescription());
    }

    @Test
    void testGetCompany_NotFound() {
        UUID companyId = UUID.randomUUID();
 
        when(companyRepository.findById(companyId)).thenReturn(java.util.Optional.empty());
 
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> companyService.getCompany(companyId)
        );
        assertEquals("Company not found", exception.getMessage());
    }

    @Test
    void testGetCompany_ReturnMappedResponse() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        company.setId(companyId);
        company.setName("Acme Corp");
        company.setWebsite("https://acme.example.com");
        company.setDescription("We build widgets.");
 
        when(companyRepository.findById(companyId)).thenReturn(java.util.Optional.of(company));
 
        CompanyResponse response = companyService.getCompany(companyId);
 
        assertEquals(company.getName(), response.getName());
        assertEquals(company.getWebsite(), response.getWebsite());
        assertEquals(company.getDescription(), response.getDescription());
    }

    @Test
    void testGetAllCompanies_Success() {
        Company company1 = new Company();
        company1.setId(UUID.randomUUID());
        company1.setName("Acme Corp");
        company1.setWebsite("https://acme.example.com");
        company1.setDescription("We build widgets.");

        Company company2 = new Company();
        company2.setId(UUID.randomUUID());
        company2.setName("Globex Inc");
        company2.setWebsite("https://globex.example.com");
        company2.setDescription("We build gadgets.");

        when(companyRepository.findAll()).thenReturn(List.of(company1, company2));

        List<CompanyResponse> responses = companyService.getAllCompanies();

        assertEquals(2, responses.size());
        assertEquals("Acme Corp", responses.get(0).getName());
        assertEquals("Globex Inc", responses.get(1).getName());
        assertEquals(company1.getId(), responses.get(0).getId());
        assertEquals(company2.getId(), responses.get(1).getId());
        verify(companyRepository, times(1)).findAll();
    }
}
