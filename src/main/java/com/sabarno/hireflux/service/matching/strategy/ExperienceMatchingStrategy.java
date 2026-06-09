package com.sabarno.hireflux.service.matching.strategy;

import org.springframework.stereotype.Component;

import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.service.matching.MatchContext;
import com.sabarno.hireflux.service.matching.MatchingStrategy;
import com.sabarno.hireflux.service.util.ResumeParsedDataExtraction;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExperienceMatchingStrategy implements MatchingStrategy{

    private final ResumeParsedDataExtraction dataExtraction;

    private double experienceScore(Integer resumeExp, Integer minExp, Integer maxExp) {

        if (minExp == null)
            return 1.0;

        if (resumeExp < minExp) {
            // penalize but don't reject
            return (double) resumeExp / minExp;
        }

        if (maxExp != null && resumeExp > maxExp) {
            return 0.9; // slightly overqualified
        }

        return 1.0;
    }

    @Override
    public double calculate(MatchContext context) {
        ResumeParsedData parsedData = dataExtraction.getParsedData(context.getResume());
        Integer experience = dataExtraction.calculateTotalExperience(parsedData.getExperience());
        return experienceScore(experience, context.getJob().getMinExperienceRequired(), context.getJob().getMaxExperienceRequired());
    }

    @Override
    public double weight() {
        return 0.2;
    }

    @Override
    public String name() {
        return "Experience";
    }

}
