package com.sabarno.hireflux.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.sabarno.hireflux.entity.SkillEdge;
import com.sabarno.hireflux.exception.impl.ConflictException;
import com.sabarno.hireflux.repository.SkillEdgeRepository;
import com.sabarno.hireflux.service.impl.SkillGraphServiceImpl;

@ExtendWith(MockitoExtension.class)
class SkillGraphServiceImplTest {

    @InjectMocks
    private SkillGraphServiceImpl skillGraphService;

    @Mock
    private SkillEdgeRepository skillEdgeRepository;

    @BeforeEach
    void setUp() {
        skillGraphService = new SkillGraphServiceImpl(skillEdgeRepository);
        lenient().when(skillEdgeRepository.save(any(SkillEdge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testUpdateGraph_SkillIsNull(){
        skillGraphService.updateGraph(null);
        verifyNoInteractions(skillEdgeRepository);
    }

    @Test
    void testUpdateGraph_SkillIsEmpty(){
        List<String> skills = new ArrayList<>();
        skillGraphService.updateGraph(skills);
        verifyNoInteractions(skillEdgeRepository);
    }

    @Test
    void testUpdateGraph_SingleSkill(){
        skillGraphService.updateGraph(List.of("Java"));
        skillGraphService.flushToDatabase();
 
        verifyNoInteractions(skillEdgeRepository);
    }

    @Test
    void testUpdateGraph_Success(){
        skillGraphService.updateGraph(List.of("Java", "Python"));
        skillGraphService.flushToDatabase();
 
        ArgumentCaptor<SkillEdge> captor = ArgumentCaptor.forClass(SkillEdge.class);
        verify(skillEdgeRepository, times(2)).save(captor.capture());
 
        List<SkillEdge> savedEdges = captor.getAllValues();
        assertTrue(savedEdges.stream().anyMatch(e ->
                e.getSkillA().equals("java") && e.getSkillB().equals("python")));
        assertTrue(savedEdges.stream().anyMatch(e ->
                e.getSkillA().equals("python") && e.getSkillB().equals("java")));
 
        double expectedWeight = Math.log(3.0) / 5.0; // log(1 + 1 co-occurrence) / 5
        for (SkillEdge edge : savedEdges) {
            assertEquals(2, edge.getCoOccurrence());
            assertEquals(expectedWeight, edge.getWeight(), 0.0001);
        }
    }

    @Test
    void testUpdateGraph_Success_WithDuplicateSkill(){
        skillGraphService.updateGraph(List.of("Java", "JAVA ", "python", " Python"));
        skillGraphService.flushToDatabase();

        verify(skillEdgeRepository, times(2)).save(any(SkillEdge.class));
    }

    @Test
    void testUpdateGraph_Success_WithRepeatedCall(){
        skillGraphService.updateGraph(List.of("Java", "Python"));
        skillGraphService.updateGraph(List.of("Java", "Python"));
        skillGraphService.flushToDatabase();
 
        ArgumentCaptor<SkillEdge> captor = ArgumentCaptor.forClass(SkillEdge.class);
        verify(skillEdgeRepository, times(2)).save(captor.capture());
 
        double expectedWeight = Math.log(5.0) / 5.0; // log(1 + 2 co-occurrences) / 5
        for (SkillEdge edge : captor.getAllValues()) {
            assertEquals(4, edge.getCoOccurrence());
            assertEquals(expectedWeight, edge.getWeight(), 0.0001);
        }
    }

    @Test
    void testUpdateGraph_WithMoreThan2Skills(){
        skillGraphService.updateGraph(List.of("Java", "Python", "SQL"));
        skillGraphService.flushToDatabase();
 
        // 3 distinct skills -> 3 * 2 = 6 directed edges (a->b for every ordered pair)
        verify(skillEdgeRepository, times(6)).save(any(SkillEdge.class));
    }

    @Test
    void testGetSimilarity_shouldReturnOne_whenSkillsAreIdentical() {
        double similarity = skillGraphService.getSimilarity("Java", "JAVA");
 
        assertEquals(1.0, similarity);
        verifyNoInteractions(skillEdgeRepository);
    }
 
    @Test
    void testGetSimilarity_shouldUseCachedValue_withoutHittingRepository() {
        skillGraphService.updateGraph(List.of("Java", "Python"));
        reset(skillEdgeRepository); // clear any incidental stubbing interactions
 
        double similarity = skillGraphService.getSimilarity("Java", "Python");
 
        assertTrue(similarity > 0);
        verifyNoInteractions(skillEdgeRepository);
    }
 
    @Test
    void testGetSimilarity_shouldFallBackToRepository_onCacheMiss() {
        SkillEdge edge = new SkillEdge();
        edge.setSkillA("java");
        edge.setSkillB("python");
        edge.setWeight(0.42);
 
        when(skillEdgeRepository.findBySkillAAndSkillB("java", "python")).thenReturn(Optional.of(edge));
 
        double similarity = skillGraphService.getSimilarity("Java", "Python");
 
        assertEquals(0.42, similarity);
        verify(skillEdgeRepository, times(1)).findBySkillAAndSkillB("java", "python");
    }
 
    @Test
    void testGetSimilarity_shouldCacheRepositoryResult_soSecondCallDoesNotHitRepository() {
        SkillEdge edge = new SkillEdge();
        edge.setSkillA("java");
        edge.setSkillB("python");
        edge.setWeight(0.42);
 
        when(skillEdgeRepository.findBySkillAAndSkillB("java", "python")).thenReturn(Optional.of(edge));
 
        skillGraphService.getSimilarity("Java", "Python");
        skillGraphService.getSimilarity("Java", "Python");
 
        verify(skillEdgeRepository, times(1)).findBySkillAAndSkillB("java", "python");
    }
 
    @Test
    void testGetSimilarity_shouldReturnAndCacheZero_whenNoEdgeExists() {
        when(skillEdgeRepository.findBySkillAAndSkillB("java", "rust")).thenReturn(Optional.empty());
 
        double first = skillGraphService.getSimilarity("Java", "Rust");
        double second = skillGraphService.getSimilarity("Java", "Rust");
 
        assertEquals(0.0, first);
        assertEquals(0.0, second);
        // Zero result should still be cached, so skillEdgeRepository only hit once.
        verify(skillEdgeRepository, times(1)).findBySkillAAndSkillB("java", "rust");
    }

    @Test
    void testFlushToDatabase_shouldDoNothing_whenNoDirtyKeys() {
        skillGraphService.flushToDatabase();
 
        verifyNoInteractions(skillEdgeRepository);
    }
 
    @Test
    void testFlushToDatabase_shouldClearDirtyKeys_soSecondFlushSavesNothingNew() {
        skillGraphService.updateGraph(List.of("Java", "Python"));
 
        skillGraphService.flushToDatabase();
        verify(skillEdgeRepository, times(2)).save(any(SkillEdge.class));
 
        reset(skillEdgeRepository);
        skillGraphService.flushToDatabase(); // nothing new dirty
        verifyNoInteractions(skillEdgeRepository);
    }
 
    @Test
    void testFlushToDatabase_shouldRetryAndMerge_onOptimisticLockingFailure() {
        skillGraphService.updateGraph(List.of("Java", "Python"));
 
        SkillEdge staleEdge = new SkillEdge();
        staleEdge.setSkillA("java");
        staleEdge.setSkillB("python");
        staleEdge.setCoOccurrence(5);
        staleEdge.setWeight(0.5);
 
        when(skillEdgeRepository.save(any(SkillEdge.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(SkillEdge.class, "id"))
                .thenAnswer(invocation -> invocation.getArgument(0));
 
        when(skillEdgeRepository.findBySkillAAndSkillB(anyString(), anyString()))
                .thenReturn(Optional.of(staleEdge));
 
        skillGraphService.flushToDatabase();

        verify(skillEdgeRepository, atLeast(2)).save(any(SkillEdge.class));
    }
 
    @Test
    void testFlushToDatabase_shouldThrowConflictException_whenAllRetriesFail() {
        skillGraphService.updateGraph(List.of("Java", "Python"));
 
        when(skillEdgeRepository.save(any(SkillEdge.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(SkillEdge.class, "id"));
 
        SkillEdge staleEdge = new SkillEdge();
        staleEdge.setSkillA("java");
        staleEdge.setSkillB("python");
        staleEdge.setCoOccurrence(1);
        staleEdge.setWeight(0.1);
        when(skillEdgeRepository.findBySkillAAndSkillB(anyString(), anyString()))
                .thenReturn(Optional.of(staleEdge));
 
        ConflictException exception = assertThrows(ConflictException.class, () -> skillGraphService.flushToDatabase());

        assertEquals( "Failed after retries",exception.getMessage());
    }

    @Test
    void testPreload_shouldPopulateCache_soGetSimilarityAvoidsRepositoryLookup() {
        SkillEdge edge = new SkillEdge();
        edge.setSkillA("java");
        edge.setSkillB("python");
        edge.setWeight(0.75);
 
        when(skillEdgeRepository.findAll()).thenReturn(List.of(edge));
 
        skillGraphService.preload();
        reset(skillEdgeRepository); // clear findAll() interaction so we can isolate getSimilarity
 
        double similarity = skillGraphService.getSimilarity("Java", "Python");
 
        assertEquals(0.75, similarity);
        verifyNoInteractions(skillEdgeRepository);
    }
 
    @Test
    void testPreload_shouldHandleEmptyRepository_gracefully() {
        when(skillEdgeRepository.findAll()).thenReturn(Collections.emptyList());
 
        assertDoesNotThrow(() -> skillGraphService.preload());
    }

    @Test
    void testFlushBeforeShutdown_shouldDelegateToFlushToDatabase() {
        skillGraphService.updateGraph(List.of("Java", "Python"));
 
        skillGraphService.flushBeforeShutdown();
 
        verify(skillEdgeRepository, times(2)).save(any(SkillEdge.class));
    }
 
    @Test
    void testFlushBeforeShutdown_shouldDoNothing_whenNoDirtyKeys() {
        skillGraphService.flushBeforeShutdown();
 
        verifyNoInteractions(skillEdgeRepository);
    }
}
