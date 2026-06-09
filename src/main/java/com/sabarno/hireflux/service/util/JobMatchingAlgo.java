package com.sabarno.hireflux.service.util;

import org.apache.coyote.BadRequestException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.repository.JobApplicationRepository;
import com.sabarno.hireflux.service.matching.JobMatchingEngine;
import com.sabarno.hireflux.service.matching.MatchContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobMatchingAlgo {

    private final JobApplicationRepository jobApplicationRepository;

    private final JobMatchingEngine jobMatchingEngine;

    @Async
    public void calculateScore(Resume resume, Job job) throws BadRequestException{
        try {
            JobApplication application = jobApplicationRepository.findByApplicantIdAndJobId(resume.getUser().getId(), job.getId()).orElseThrow(() -> new BadRequestException("No application found"));

            double score = jobMatchingEngine.calculate(new MatchContext(resume, job));
            application.setMatchScore(score);
            log.info("event=calculate_score, application_id={}, score={}", application.getId(), score);
            jobApplicationRepository.save(application);
            
        } catch (BadRequestException e) {
            throw new BadRequestException("Couldn't calculate matching score");
        }
    }
}
