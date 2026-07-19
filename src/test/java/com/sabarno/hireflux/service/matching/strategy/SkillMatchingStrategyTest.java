package com.sabarno.hireflux.service.matching.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.service.SkillGraphService;
import com.sabarno.hireflux.service.matching.MatchContext;
import com.sabarno.hireflux.service.util.ResumeParsedDataExtraction;

@ExtendWith(MockitoExtension.class)
class SkillMatchingStrategyTest {
 
    @Mock
    private SkillGraphService skillGraphService;
 
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
 
    private SkillMatchingStrategy strategy;
 
    @BeforeEach
    void setUp() {
        strategy = new SkillMatchingStrategy(skillGraphService, dataExtraction);
 
        lenient().when(context.getResume()).thenReturn(resume);
        lenient().when(context.getJob()).thenReturn(job);
        lenient().when(dataExtraction.getParsedData(resume)).thenReturn(parsedData);
    }
 
    @Test
    void testCalculate_shouldReturnZero_whenResumeHasNoSkills() {
        when(parsedData.getSkills()).thenReturn(null);
        when(job.getRequiredSkills()).thenReturn(List.of("Java", "Spring"));
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result);
        verifyNoInteractions(skillGraphService);
    }
 
    @Test
    void testCalculate_shouldReturnZero_whenResumeSkillsListIsEmpty() {
        when(parsedData.getSkills()).thenReturn(List.of());
        when(job.getRequiredSkills()).thenReturn(List.of("Java", "Spring"));
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result);
        verifyNoInteractions(skillGraphService);
    }
 
    @Test
    void testCalculate_shouldReturnZero_whenJobRequiredSkillsIsEmpty() {
        when(parsedData.getSkills()).thenReturn(List.of("Java", "Python"));
        when(job.getRequiredSkills()).thenReturn(List.of());
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result);
        verifyNoInteractions(skillGraphService);
    }
 
    @Test
    void testCalculate_shouldThrowNullPointerException_whenJobRequiredSkillsIsNull() {
        // Unlike resumeSkills, jobSkills has no null-guard -- iterating a
        // null List in the inner for-each throws an NPE. Documents current
        // behavior rather than assuming it can't happen.
        when(parsedData.getSkills()).thenReturn(List.of("Java"));
        when(job.getRequiredSkills()).thenReturn(null);
 
        assertThrows(NullPointerException.class, () -> strategy.calculate(context));
    }
 
    @Test
    void testCalculate_shouldReturnDirectSimilarity_forSingleSkillPair() {
        when(parsedData.getSkills()).thenReturn(List.of("Java"));
        when(job.getRequiredSkills()).thenReturn(List.of("Spring"));
        when(skillGraphService.getSimilarity("Java", "Spring")).thenReturn(0.6);
 
        double result = strategy.calculate(context);
 
        assertEquals(0.6, result);
    }
 
    @Test
    void testCalculate_shouldReturnHighestSimilarity_acrossAllPairs() {
        when(parsedData.getSkills()).thenReturn(List.of("Java", "Python"));
        when(job.getRequiredSkills()).thenReturn(List.of("Spring", "Django"));
 
        when(skillGraphService.getSimilarity("Java", "Spring")).thenReturn(0.8);
        when(skillGraphService.getSimilarity("Java", "Django")).thenReturn(0.2);
        when(skillGraphService.getSimilarity("Python", "Spring")).thenReturn(0.3);
        when(skillGraphService.getSimilarity("Python", "Django")).thenReturn(0.9);
 
        double result = strategy.calculate(context);
 
        assertEquals(0.9, result); // best pair is (Python, Django)
    }
 
    @Test
    void testCalculate_shouldEvaluateEveryResumeSkillAgainstEveryJobSkill() {
        when(parsedData.getSkills()).thenReturn(List.of("Java", "Python"));
        when(job.getRequiredSkills()).thenReturn(List.of("Spring", "Django"));
        when(skillGraphService.getSimilarity(anyString(), anyString())).thenReturn(0.1);
 
        strategy.calculate(context);
 
        verify(skillGraphService).getSimilarity("Java", "Spring");
        verify(skillGraphService).getSimilarity("Java", "Django");
        verify(skillGraphService).getSimilarity("Python", "Spring");
        verify(skillGraphService).getSimilarity("Python", "Django");
        verify(skillGraphService, times(4)).getSimilarity(anyString(), anyString());
    }
 
    @Test
    void testCalculate_shouldIgnoreWeakMatches_whenOneStrongMatchExists() {
        // Documents the "best single match" design: even if a resume has
        // 9 completely irrelevant skills, ONE strong match with a required
        // skill is enough to score as if it were a great overall fit.
        when(parsedData.getSkills()).thenReturn(List.of("Cooking", "Painting", "Java"));
        when(job.getRequiredSkills()).thenReturn(List.of("Java"));
 
        when(skillGraphService.getSimilarity("Cooking", "Java")).thenReturn(0.0);
        when(skillGraphService.getSimilarity("Painting", "Java")).thenReturn(0.0);
        when(skillGraphService.getSimilarity("Java", "Java")).thenReturn(1.0);
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result);
    }
 
    @Test
    void testWeight_shouldReturnPointTwo() {
        assertEquals(0.2, strategy.weight());
    }
 
    @Test
    void testName_shouldReturnSkills() {
        assertEquals("skills", strategy.name());
    }
}
