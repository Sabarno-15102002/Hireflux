package com.sabarno.hireflux.service.matching.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.service.matching.MatchContext;
import com.sabarno.hireflux.service.util.ResumeParsedDataExtraction;

@ExtendWith(MockitoExtension.class)
class ExperienceMatchingStrategyTest {
 
    @Mock
    private ResumeParsedDataExtraction dataExtraction;
 
    @Mock
    private MatchContext context;
 
    @Mock
    private Resume resume;
 
    @Mock
    private Job job;
 
    @Mock
    private ResumeParsedData parsedData;
 
    private ExperienceMatchingStrategy strategy;
 
    @BeforeEach
    void setUp() {
        strategy = new ExperienceMatchingStrategy(dataExtraction);
 
        lenient().when(context.getResume()).thenReturn(resume);
        lenient().when(context.getJob()).thenReturn(job);
        lenient().when(dataExtraction.getParsedData(resume)).thenReturn(parsedData);
    }
 
    private void stubExperience(Integer resumeExperience, Integer minExp, Integer maxExp) {
        when(dataExtraction.calculateTotalExperience(parsedData.getExperience()))
                .thenReturn(resumeExperience);
        when(job.getMinExperienceRequired()).thenReturn(minExp);
        when(job.getMaxExperienceRequired()).thenReturn(maxExp);
    }
 
    @Test
    void testCalculate_shouldReturnOne_whenMinExperienceIsNull() {
        stubExperience(1, null, null);
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnPartialScore_whenResumeExperienceIsBelowMinimum() {
        stubExperience(3, 5, 10);
 
        double result = strategy.calculate(context);
 
        assertEquals(0.6, result, 0.0001); // 3.0 / 5
    }
 
    @Test
    void testCalculate_shouldReturnOne_whenResumeExperienceExactlyMeetsMinimum() {
        stubExperience(5, 5, 10);
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnOne_whenResumeExperienceIsWithinRange() {
        stubExperience(7, 5, 10);
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnOne_whenResumeExperienceExactlyMeetsMaximum() {
        stubExperience(10, 5, 10);
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnPointNine_whenResumeExperienceExceedsMaximum() {
        stubExperience(15, 5, 10);
 
        double result = strategy.calculate(context);
 
        assertEquals(0.9, result);
    }
 
    @Test
    void testCalculate_shouldReturnOne_whenMaxExperienceIsNullAndResumeMeetsMinimum() {
        stubExperience(20, 5, null);
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnZero_whenResumeExperienceIsZeroAndBelowMinimum() {
        stubExperience(0, 5, 10);
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result); // 0.0 / 5
    }
 
    @Test
    void testCalculate_shouldUseDataExtractionToResolveExperience() {
        stubExperience(8, 5, 10);
 
        strategy.calculate(context);
 
        verify(dataExtraction).getParsedData(resume);
        verify(dataExtraction).calculateTotalExperience(parsedData.getExperience());
    }
 
    @Test
    void testWeight_shouldReturnPointTwo() {
        assertEquals(0.2, strategy.weight());
    }
 
    @Test
    void testName_shouldReturnExperience() {
        assertEquals("Experience", strategy.name());
    }
}