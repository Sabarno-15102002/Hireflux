package com.sabarno.hireflux.service.matching.strategy;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.service.SkillGraphService;
import com.sabarno.hireflux.service.matching.MatchContext;
import com.sabarno.hireflux.service.matching.MatchingStrategy;
import com.sabarno.hireflux.service.util.ResumeParsedDataExtraction;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SkillMatchingStrategy implements MatchingStrategy {

    private final SkillGraphService skillGraphService;

    private final ResumeParsedDataExtraction dataExtraction;

    @Override
    public double calculate(MatchContext context) {

        ResumeParsedData parsedData = dataExtraction.getParsedData(context.getResume());
        
        List<String> resumeSkills = parsedData.getSkills() != null ? parsedData.getSkills() : new ArrayList<>();

        List<String> jobSkills = context.getJob().getRequiredSkills();

        double bestMatch = 0;

        for (String rs : resumeSkills) {

            for (String js : jobSkills) {

                double sim =
                        skillGraphService
                                .getSimilarity(rs, js);

                bestMatch = Math.max(bestMatch, sim);
            }
        }

        return bestMatch;
    }

    @Override
    public double weight() {
        return 0.2;
    }

    @Override
    public String name() {
        return "skills";
    }
}