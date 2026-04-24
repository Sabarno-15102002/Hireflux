package com.sabarno.hireflux.utility.projection;

import java.util.UUID;

import com.sabarno.hireflux.utility.enums.ResumeUploadStatus;

public interface ResumeMini {
    UUID getId();
    String getFileName();
    ResumeUploadStatus getUploadStatus();
}