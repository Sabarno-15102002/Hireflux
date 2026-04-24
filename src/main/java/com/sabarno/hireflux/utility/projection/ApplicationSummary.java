package com.sabarno.hireflux.utility.projection;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sabarno.hireflux.utility.enums.ApplicationStatus;

public interface ApplicationSummary {

    UUID getId();

    ApplicantSummary getApplicant();

    JobMini getJob();

    ResumeMini getResume();

    ApplicationStatus getStatus();

    Double getMatchScore();

    LocalDateTime getAppliedAt();

}