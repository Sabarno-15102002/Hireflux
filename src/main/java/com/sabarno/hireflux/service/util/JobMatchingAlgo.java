package com.sabarno.hireflux.service.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.service.SkillGraphService;
import com.sabarno.hireflux.utility.ResumeUploadStatus;

@Service
public class JobMatchingAlgo {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SkillGraphService skillGraphService;

    private List<Double> fromJson(String json) throws BadRequestException {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {
            });
        } catch (Exception e) {
            throw new BadRequestException("Error parsing embedding");
        }
    }

    public double calculateEmbeddingScore(Resume resume, Job job) throws BadRequestException {

        if (resume.getUploadStatus() != ResumeUploadStatus.PROCESSED) {
            throw new BadRequestException("Resume not processed yet");
        }
        List<Double> resumeVec = fromJson(resume.getEmbedding());
        List<Double> jobVec = fromJson(job.getEmbedding());

        return cosineSimilarity(resumeVec, jobVec);
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

    public double experienceScore(int resumeExp, Integer minExp, Integer maxExp) {

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

    public double skillScore(List<String> resumeSkills, List<String> jobSkills) {

        Set<String> resumeSet = new HashSet<>(resumeSkills);
        Set<String> jobSet = new HashSet<>(jobSkills);
        double bestMatch = 0;

        for (String jobSkill : jobSet) {
            for (String resumeSkill : resumeSet) {

                double sim = skillGraphService.getSimilarity(resumeSkill,jobSkill);
                bestMatch = Math.max(bestMatch, sim);
            }
        }
        return bestMatch;
    }

    public double locationScore(String resumeLocation, String jobLocation) {

        if (jobLocation == null)
            return 1.0;
        if (jobLocation.equalsIgnoreCase(resumeLocation)) {
            return 1.0;
        }
        return 0.7; // not same, but still possible
    }
}
