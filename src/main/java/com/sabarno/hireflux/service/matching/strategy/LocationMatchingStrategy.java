package com.sabarno.hireflux.service.matching.strategy;

import org.springframework.stereotype.Component;

import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.service.matching.MatchContext;
import com.sabarno.hireflux.service.matching.MatchingStrategy;
import com.sabarno.hireflux.service.util.ResumeParsedDataExtraction;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocationMatchingStrategy implements MatchingStrategy{
    
    private final ResumeParsedDataExtraction dataExtraction;

    private double locationScore(String resumeLocation, String jobLocation) {

        if (jobLocation == null)
            return 1.0;
        if (jobLocation.equalsIgnoreCase(resumeLocation)) {
            return 1.0;
        }
        return 0.7; // not same, but still possible
    }

    @Override
    public double calculate(MatchContext context) {
        ResumeParsedData parsedData = dataExtraction.getParsedData(context.getResume());
        String resumeLocation = dataExtraction.extractLocation(parsedData.getExperience());
        String jobLocation = context.getJob().getLocation();
        return locationScore(resumeLocation, jobLocation);
    }

    @Override
    public double weight() {
        return 0.1;
    }

    @Override
    public String name() {
        return "location";
    }

}
