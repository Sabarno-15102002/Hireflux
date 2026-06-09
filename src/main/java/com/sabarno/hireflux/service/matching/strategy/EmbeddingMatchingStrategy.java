package com.sabarno.hireflux.service.matching.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.service.matching.MatchContext;
import com.sabarno.hireflux.service.matching.MatchingStrategy;
import com.sabarno.hireflux.utility.enums.ResumeUploadStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmbeddingMatchingStrategy implements MatchingStrategy {
    
    private final ObjectMapper objectMapper;

    private List<Double> fromJson(String json) throws BadRequestException {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {
            });
        } catch (Exception e) {
            throw new BadRequestException("Error parsing embedding");
        }
    }

    private double cosineSimilarity(List<Double> resumeVec, List<Double> jobVec) {
        if (resumeVec.size() != jobVec.size()) {
            throw new IllegalArgumentException("Embedding vectors must be of same length");
        }

        double dotProduct = 0.0;
        double resumeNorm = 0.0;
        double jobNorm = 0.0;

        for (int i = 0; i < resumeVec.size(); i++) {
            dotProduct += resumeVec.get(i) * jobVec.get(i);
            resumeNorm += Math.pow(resumeVec.get(i), 2);
            jobNorm += Math.pow(jobVec.get(i), 2);
        }

        resumeNorm = Math.sqrt(resumeNorm);
        jobNorm = Math.sqrt(jobNorm);

        if (resumeNorm == 0 || jobNorm == 0) {
            return 0.0;
        }

        return dotProduct / (resumeNorm * jobNorm);
    }
    
    @Override
    public double calculate(MatchContext context) {
        Resume resume = context.getResume();
        Job job = context.getJob();

        try {
            if (resume.getUploadStatus() != ResumeUploadStatus.PROCESSED) {
                throw new BadRequestException("Resume not processed yet");
            }
            List<Double> resumeVec = fromJson(resume.getEmbedding());
            List<Double> jobVec = fromJson(job.getEmbedding());

            return cosineSimilarity(resumeVec, jobVec);
        } catch (Exception e) {
            return 0.0; // If any error occurs, return a score of 0
        }
    }

    @Override
    public double weight() {
        return 0.5;
    }

    @Override
    public String name() {
        return "embedding";
    }

}
