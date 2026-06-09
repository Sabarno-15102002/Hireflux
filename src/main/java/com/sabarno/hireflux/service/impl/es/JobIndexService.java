package com.sabarno.hireflux.service.impl.es;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.es.JobDocument;
import com.sabarno.hireflux.repository.es.JobSearchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobIndexService {

    private final JobSearchRepository jobSearchRepository;

    public void indexJob(Job job) {

        JobDocument doc = new JobDocument();

        doc.setId(job.getId());
        doc.setTitle(job.getTitle());
        doc.setDescription(job.getDescription());
        doc.setCompanyName(job.getCompany().getName());
        doc.setLocation(job.getLocation());
        doc.setJobType(job.getJobType().name());
        doc.setRequiredSkills(job.getRequiredSkills());
        doc.setMinExperienceRequired(job.getMinExperienceRequired());
        doc.setMaxExperienceRequired(job.getMaxExperienceRequired());
        doc.setEmbedding(job.getEmbedding());
        doc.setCreatedAt(job.getCreatedAt());

        jobSearchRepository.save(doc);
    }

    public void deleteJob(UUID jobId) {
        jobSearchRepository.deleteById(jobId);
    }
}