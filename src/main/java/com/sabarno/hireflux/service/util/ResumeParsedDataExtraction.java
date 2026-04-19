package com.sabarno.hireflux.service.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.dto.Experience;
import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.exception.impl.FileProcessingException;

@Service
public class ResumeParsedDataExtraction {

    @Autowired
    private ObjectMapper objectMapper;

    public ResumeParsedData getParsedData(Resume resume) {
        try {
            return objectMapper.readValue(
                    resume.getParsedData(),
                    ResumeParsedData.class);
        } catch (Exception e) {
            throw new FileProcessingException("Failed to parse resume data", e);
        }
    }

    public int calculateTotalExperience(List<Experience> experiences) {

        int totalMonths = 0;
        for (Experience exp : experiences) {
            LocalDateTime start = exp.getFrom();
            LocalDateTime end = exp.getTo() != null ? exp.getTo() : LocalDateTime.now();

            long months = ChronoUnit.MONTHS.between(start, end);
            totalMonths += months;
        }
        return totalMonths / 12;
    }

    public String extractLocation(List<Experience> experiences) {
        if (experiences == null || experiences.isEmpty()) {
            return "Unknown";
        }
        // Assuming the most recent experience location is the current location
        return experiences.get(0).getLocation();
    }
        
}
