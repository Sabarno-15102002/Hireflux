package com.sabarno.hireflux.utility.projection;

import java.util.UUID;

public interface ApplicantSummary {
    UUID getId();
    String getName();
    String getEmail();
}