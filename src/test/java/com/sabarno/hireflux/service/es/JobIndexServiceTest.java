package com.sabarno.hireflux.service.es;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sabarno.hireflux.entity.Company;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.es.JobDocument;
import com.sabarno.hireflux.repository.es.JobSearchRepository;
import com.sabarno.hireflux.service.impl.es.JobIndexService;
import com.sabarno.hireflux.utility.enums.JobType;

@ExtendWith(MockitoExtension.class)
class JobIndexServiceTest {

    @Mock
    private JobSearchRepository jobSearchRepository;
 
    @InjectMocks
    private JobIndexService jobIndexService;
 
    private Job job;
 
    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setName("Acme Corp");
 
        job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Backend Engineer");
        job.setDescription("Build APIs");
        job.setCompany(company);
        job.setLocation("Remote");
        job.setJobType(JobType.FULL_TIME);
        job.setRequiredSkills(List.of("Java", "Spring"));
        job.setMinExperienceRequired(2);
        job.setMaxExperienceRequired(5);
        job.setEmbedding(List.of(0.1, 0.2, 0.3).toString());
        job.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testIndexJob_shouldMapAllFieldsAndSaveDocument() {
        jobIndexService.indexJob(job);
 
        ArgumentCaptor<JobDocument> captor = ArgumentCaptor.forClass(JobDocument.class);
        verify(jobSearchRepository).save(captor.capture());
        JobDocument savedDoc = captor.getValue();
 
        assertEquals(job.getId(), savedDoc.getId());
        assertEquals(job.getTitle(), savedDoc.getTitle());
        assertEquals(job.getDescription(), savedDoc.getDescription());
        assertEquals(job.getCompany().getName(), savedDoc.getCompanyName());
        assertEquals(job.getLocation(), savedDoc.getLocation());
        assertEquals(job.getJobType().name(), savedDoc.getJobType());
        assertEquals(job.getRequiredSkills(), savedDoc.getRequiredSkills());
        assertEquals(job.getMinExperienceRequired(), savedDoc.getMinExperienceRequired());
        assertEquals(job.getMaxExperienceRequired(), savedDoc.getMaxExperienceRequired());
        assertEquals(job.getEmbedding(), savedDoc.getEmbedding());
        assertEquals(job.getCreatedAt(), savedDoc.getCreatedAt());
    }
 
    @Test
    void testIndexJob_shouldCallSaveExactlyOnce() {
        jobIndexService.indexJob(job);
 
        verify(jobSearchRepository, times(1)).save(any(JobDocument.class));
        verifyNoMoreInteractions(jobSearchRepository);
    }
 
    @Test
    void testIndexJob_shouldThrowNullPointerException_whenJobHasNoCompany() {
        job.setCompany(null);
 
        assertThrows(NullPointerException.class, () -> jobIndexService.indexJob(job));
    }
 
    @Test
    void testIndexJob_shouldThrowNullPointerException_whenJobHasNoJobType() {
        job.setJobType(null);
 
        assertThrows(NullPointerException.class, () -> jobIndexService.indexJob(job));
    }
 
    @Test
    void testIndexJob_shouldHandleNullRequiredSkills() {
        job.setRequiredSkills(null);
 
        jobIndexService.indexJob(job);
 
        ArgumentCaptor<JobDocument> captor = ArgumentCaptor.forClass(JobDocument.class);
        verify(jobSearchRepository).save(captor.capture());
        assertNull(captor.getValue().getRequiredSkills());
    }
 
    @Test
    void testIndexJob_shouldPropagateException_whenRepositoryThrows() {
        doThrow(new RuntimeException("Search index unavailable"))
                .when(jobSearchRepository).save(any(JobDocument.class));
 
        assertThrows(RuntimeException.class, () -> jobIndexService.indexJob(job));
    }

    @Test
    void testDeleteJob_shouldCallDeleteByIdWithGivenId() {
        UUID jobId = UUID.randomUUID();
 
        jobIndexService.deleteJob(jobId);
 
        verify(jobSearchRepository, times(1)).deleteById(jobId);
        verifyNoMoreInteractions(jobSearchRepository);
    }
 
    @Test
    void testDeleteJob_shouldPropagateException_whenRepositoryThrows() {
        UUID jobId = UUID.randomUUID();
        doThrow(new RuntimeException("Search index unavailable"))
                .when(jobSearchRepository).deleteById(jobId);
 
        assertThrows(RuntimeException.class, () -> jobIndexService.deleteJob(jobId));
    }
}
