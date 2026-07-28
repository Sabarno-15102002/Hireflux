package com.sabarno.hireflux.service.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.exception.impl.FileProcessingException;
import com.sabarno.hireflux.utility.Experience;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeParsedDataExtractionTest {
 
    @Mock
    private ObjectMapper objectMapper;
 
    @Mock
    private Resume resume;
 
    @InjectMocks
    private ResumeParsedDataExtraction dataExtraction;
 
    @Test
    void testGetParsedData_shouldReturnDeserializedObject_whenJsonIsValid() throws Exception {
        when(resume.getParsedData()).thenReturn("{\"skills\":[\"java\"]}");
        ResumeParsedData parsedData = new ResumeParsedData();
        when(objectMapper.readValue("{\"skills\":[\"java\"]}", ResumeParsedData.class))
                .thenReturn(parsedData);
 
        ResumeParsedData result = dataExtraction.getParsedData(resume);
 
        assertSame(parsedData, result);
    }
 
    @Test
    void testGetParsedData_shouldThrowFileProcessingException_whenJsonIsInvalid() throws Exception {
        when(resume.getParsedData()).thenReturn("not-valid-json");
        RuntimeException jsonError = new RuntimeException("Unexpected token");
        when(objectMapper.readValue("not-valid-json", ResumeParsedData.class)).thenThrow(jsonError);
 
        FileProcessingException exception = assertThrows(
                FileProcessingException.class,
                () -> dataExtraction.getParsedData(resume)
        );
 
        assertEquals("Failed to parse resume data", exception.getMessage());
        assertSame(jsonError, exception.getCause());
    }
 
    @Test
    void testGetParsedData_shouldThrowFileProcessingException_whenParsedDataIsNull() throws Exception {
        when(resume.getParsedData()).thenReturn(null);
        when(objectMapper.readValue((String) isNull(), eq(ResumeParsedData.class)))
                .thenThrow(new IllegalArgumentException("content cannot be null"));
 
        FileProcessingException exception = assertThrows(
                FileProcessingException.class,
                () -> dataExtraction.getParsedData(resume)
        );
 
        assertEquals("Failed to parse resume data", exception.getMessage());
    }
 
    @Test
    void testCalculateTotalExperience_shouldReturnZero_whenExperienceListIsEmpty() {
        int result = dataExtraction.calculateTotalExperience(Collections.emptyList());
 
        assertEquals(0, result);
    }
 
    @Test
    void testCalculateTotalExperience_shouldThrowNullPointerException_whenExperienceListIsNull() {
        // Documents current behavior: no null-guard on the `experiences`
        // parameter, unlike extractLocation which explicitly handles null.
        assertThrows(NullPointerException.class,
                () -> dataExtraction.calculateTotalExperience(null));
    }
 
    @Test
    void testCalculateTotalExperience_shouldCalculateYears_forSingleCompletedExperience() {
        Experience exp = buildExperience(
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2022, 1, 1, 0, 0),
                "Bengaluru"
        );
 
        int result = dataExtraction.calculateTotalExperience(List.of(exp));
 
        assertEquals(2, result); // exactly 24 months -> 2 years
    }
 
    @Test
    void testCalculateTotalExperience_shouldTreatNullEndDate_asOngoingUntilNow() {
        Experience exp = buildExperience(
                LocalDateTime.now().minusYears(3),
                null,
                "Remote"
        );
 
        int result = dataExtraction.calculateTotalExperience(List.of(exp));
 
        assertEquals(3, result);
    }
 
    @Test
    void testCalculateTotalExperience_shouldSumMultipleExperiences() {
        Experience exp1 = buildExperience(
                LocalDateTime.of(2018, 1, 1, 0, 0),
                LocalDateTime.of(2020, 1, 1, 0, 0),
                "Bengaluru"
        ); // 2 years
        Experience exp2 = buildExperience(
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 0, 0),
                "Mumbai"
        ); // 3 years
 
        int result = dataExtraction.calculateTotalExperience(List.of(exp1, exp2));
 
        assertEquals(5, result);
    }
 
    @Test
    void testCalculateTotalExperience_shouldTruncatePartialYears_dueToIntegerDivision() {
        // 18 months total -> 18/12 = 1 (partial year truncated, not rounded)
        Experience exp = buildExperience(
                LocalDateTime.of(2022, 1, 1, 0, 0),
                LocalDateTime.of(2023, 7, 1, 0, 0),
                "Remote"
        );
 
        int result = dataExtraction.calculateTotalExperience(List.of(exp));
 
        assertEquals(1, result);
    }
 
    @Test
    void testCalculateTotalExperience_shouldAccumulateMonthsAcrossEntries_beforeFinalDivision() {
        // Confirms aggregation happens BEFORE the final /12 truncation,
        // not per-entry (which would silently lose more precision):
        // two 6-month stints correctly combine into 12 months -> 1 year,
        // rather than each truncating to 0 years individually.
        Experience exp1 = buildExperience(
                LocalDateTime.of(2021, 1, 1, 0, 0),
                LocalDateTime.of(2021, 7, 1, 0, 0),
                "Remote"
        ); // 6 months
        Experience exp2 = buildExperience(
                LocalDateTime.of(2021, 7, 1, 0, 0),
                LocalDateTime.of(2022, 1, 1, 0, 0),
                "Remote"
        ); // 6 months
 
        int result = dataExtraction.calculateTotalExperience(List.of(exp1, exp2));
 
        assertEquals(1, result); // 12 months combined -> 1 year
    }
 
    @Test
    void testCalculateTotalExperience_shouldReturnNegativeMonthsDivided_whenEndIsBeforeStart() {
        Experience exp = buildExperience(
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2022, 1, 1, 0, 0),
                "Remote"
        );
 
        int result = dataExtraction.calculateTotalExperience(List.of(exp));
 
        assertEquals(-1, result);
    }
 
    private Experience buildExperience(LocalDateTime from, LocalDateTime to, String location) {
        Experience exp = new Experience();
        exp.setFrom(from);
        exp.setTo(to);
        exp.setLocation(location);
        return exp;
    }
 
    @Test
    void testExtractLocation_shouldReturnUnknown_whenExperienceListIsNull() {
        String result = dataExtraction.extractLocation(null);
 
        assertEquals("Unknown", result);
    }
 
    @Test
    void testExtractLocation_shouldReturnUnknown_whenExperienceListIsEmpty() {
        String result = dataExtraction.extractLocation(Collections.emptyList());
 
        assertEquals("Unknown", result);
    }
 
    @Test
    void testExtractLocation_shouldReturnFirstEntrysLocation() {
        Experience recent = buildExperience(
                LocalDateTime.of(2023, 1, 1, 0, 0), null, "Bengaluru");
        Experience older = buildExperience(
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 1, 0, 0), "Mumbai");
 
        String result = dataExtraction.extractLocation(new ArrayList<>(List.of(recent, older)));
 
        assertEquals("Bengaluru", result);
    }
 
    @Test
    void testExtractLocation_shouldReturnNull_whenFirstExperienceHasNullLocation() {
        Experience exp = buildExperience(LocalDateTime.now(), null, null);
 
        String result = dataExtraction.extractLocation(List.of(exp));
 
        assertNull(result);
    }
}