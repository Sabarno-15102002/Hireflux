package com.sabarno.hireflux.service.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobMatchingEngineTest {
 
    @Mock
    private MatchContext context;
 
    @Mock
    private MatchingStrategy skillMatchStrategy;
 
    @Mock
    private MatchingStrategy experienceMatchStrategy;
 
    @Test
    void testCalculate_shouldReturnZero_whenNoStrategiesConfigured() {
        JobMatchingEngine engine = new JobMatchingEngine(Collections.emptyList());
 
        double result = engine.calculate(context);
 
        assertEquals(0.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnWeightedScore_forSingleStrategy() {
        when(skillMatchStrategy.calculate(context)).thenReturn(0.8);
        when(skillMatchStrategy.weight()).thenReturn(0.5);
 
        JobMatchingEngine engine = new JobMatchingEngine(List.of(skillMatchStrategy));
 
        double result = engine.calculate(context);
 
        assertEquals(0.4, result, 0.0001); // 0.8 * 0.5
    }
 
    @Test
    void testCalculate_shouldSumWeightedScores_acrossMultipleStrategies() {
        when(skillMatchStrategy.calculate(context)).thenReturn(0.8);
        when(skillMatchStrategy.weight()).thenReturn(0.6);
 
        when(experienceMatchStrategy.calculate(context)).thenReturn(0.5);
        when(experienceMatchStrategy.weight()).thenReturn(0.4);
 
        JobMatchingEngine engine = new JobMatchingEngine(
                List.of(skillMatchStrategy, experienceMatchStrategy));
 
        double result = engine.calculate(context);
 
        // (0.8 * 0.6) + (0.5 * 0.4) = 0.48 + 0.20 = 0.68
        assertEquals(0.68, result, 0.0001);
    }
 
    @Test
    void testCalculate_shouldInvokeEveryStrategy_withSameContext() {
        when(skillMatchStrategy.calculate(context)).thenReturn(1.0);
        when(skillMatchStrategy.weight()).thenReturn(1.0);
        when(experienceMatchStrategy.calculate(context)).thenReturn(1.0);
        when(experienceMatchStrategy.weight()).thenReturn(1.0);
 
        JobMatchingEngine engine = new JobMatchingEngine(
                List.of(skillMatchStrategy, experienceMatchStrategy));
 
        engine.calculate(context);
 
        verify(skillMatchStrategy, times(1)).calculate(context);
        verify(experienceMatchStrategy, times(1)).calculate(context);
    }
 
    @Test
    void testCalculate_shouldHandleZeroWeight_contributingNothingToTotal() {
        when(skillMatchStrategy.calculate(context)).thenReturn(0.9);
        when(skillMatchStrategy.weight()).thenReturn(0.0);
 
        JobMatchingEngine engine = new JobMatchingEngine(List.of(skillMatchStrategy));
 
        double result = engine.calculate(context);
 
        assertEquals(0.0, result);
    }
 
    @Test
    void testCalculate_shouldHandleNegativeWeight_subtractingFromTotal() {
        when(skillMatchStrategy.calculate(context)).thenReturn(0.5);
        when(skillMatchStrategy.weight()).thenReturn(-1.0);
 
        when(experienceMatchStrategy.calculate(context)).thenReturn(0.5);
        when(experienceMatchStrategy.weight()).thenReturn(1.0);
 
        JobMatchingEngine engine = new JobMatchingEngine(
                List.of(skillMatchStrategy, experienceMatchStrategy));
 
        double result = engine.calculate(context);
 
        // (0.5 * -1.0) + (0.5 * 1.0) = -0.5 + 0.5 = 0.0
        assertEquals(0.0, result, 0.0001);
    }
 
    @Test
    void testCalculate_shouldPropagateException_whenStrategyThrows() {
        when(skillMatchStrategy.calculate(context)).thenThrow(new RuntimeException("Strategy failure"));
 
        JobMatchingEngine engine = new JobMatchingEngine(List.of(skillMatchStrategy));
 
        assertThrows(RuntimeException.class, () -> engine.calculate(context));
    }
}