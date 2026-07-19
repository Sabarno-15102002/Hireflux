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
class LocationMatchingStrategyTest {
 
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
 
    private LocationMatchingStrategy strategy;
 
    @BeforeEach
    void setUp() {
        strategy = new LocationMatchingStrategy(dataExtraction);

        lenient().when(context.getResume()).thenReturn(resume);
        lenient().when(context.getJob()).thenReturn(job);
        lenient().when(dataExtraction.getParsedData(resume)).thenReturn(parsedData);
    }
 
    @Test
    void testCalculate_shouldReturnOne_whenJobLocationIsNull() {
        when(job.getLocation()).thenReturn(null);
        when(dataExtraction.extractLocation(parsedData.getExperience())).thenReturn("Bengaluru");
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnOne_whenLocationsMatchExactly() {
        when(job.getLocation()).thenReturn("Bengaluru");
        when(dataExtraction.extractLocation(parsedData.getExperience())).thenReturn("Bengaluru");
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnOne_whenLocationsMatchIgnoringCase() {
        when(job.getLocation()).thenReturn("BENGALURU");
        when(dataExtraction.extractLocation(parsedData.getExperience())).thenReturn("bengaluru");
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnPointSeven_whenLocationsDoNotMatch() {
        when(job.getLocation()).thenReturn("Bengaluru");
        when(dataExtraction.extractLocation(parsedData.getExperience())).thenReturn("Mumbai");
 
        double result = strategy.calculate(context);
 
        assertEquals(0.7, result);
    }
 
    @Test
    void testCalculate_shouldReturnPointSeven_whenResumeLocationIsNullButJobLocationIsNot() {
        when(job.getLocation()).thenReturn("Bengaluru");
        when(dataExtraction.extractLocation(parsedData.getExperience())).thenReturn(null);
 
        double result = strategy.calculate(context);
 
        assertEquals(0.7, result);
    }
 
    @Test
    void testCalculate_shouldUseDataExtractionToResolveResumeLocation() {
        when(job.getLocation()).thenReturn("Remote");
        when(dataExtraction.extractLocation(parsedData.getExperience())).thenReturn("Remote");
 
        strategy.calculate(context);
 
        verify(dataExtraction).getParsedData(resume);
        verify(dataExtraction).extractLocation(parsedData.getExperience());
    }
 
    @Test
    void testWeight_shouldReturnPointOne() {
        assertEquals(0.1, strategy.weight());
    }
 
    @Test
    void testName_shouldReturnLocation() {
        assertEquals("location", strategy.name());
    }
}
