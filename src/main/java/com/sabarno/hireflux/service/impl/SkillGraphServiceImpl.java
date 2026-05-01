package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.entity.SkillEdge;
import com.sabarno.hireflux.exception.impl.ConflictException;
import com.sabarno.hireflux.repository.SkillEdgeRepository;
import com.sabarno.hireflux.service.SkillGraphService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SkillGraphServiceImpl implements SkillGraphService {

    @Autowired
    private SkillEdgeRepository repository;

    private final Map<String, Double> cache = new ConcurrentHashMap<>();
    private final Map<String, SkillEdge> edgeStore = new ConcurrentHashMap<>();
    private final Set<String> dirtyKeys = ConcurrentHashMap.newKeySet();
    private final Object flushLock = new Object();

    private String normalize(String skill) {
        return skill.toLowerCase().trim();
    }

    // 🔹 Learn relationships
    @Override
    @Async
    public void updateGraph(List<String> skills) {

        if (skills == null || skills.isEmpty()) return;
        Set<String> normalized = skills.stream()
                .map(this::normalize)
                .collect(Collectors.toSet());

        for (String a : normalized) {
            for (String b : normalized) {

                if (a.equals(b))
                    continue;

                updateEdge(a, b);
                updateEdge(b, a);
            }
        }
    }

    private void updateEdge(String edgeA, String edgeB) {

        String key = edgeA + "|" + edgeB;

        SkillEdge edge = edgeStore.computeIfAbsent(key, k -> {
            SkillEdge e = new SkillEdge();
            e.setSkillA(edgeA);
            e.setSkillB(edgeB);
            e.setCoOccurrence(0);
            e.setWeight(0.0);
            return e;
        });

        synchronized (edge) {
            // update counts
            edge.setCoOccurrence(edge.getCoOccurrence() + 1);

            // simple weight formula
            double weight = Math.log(1.0 + edge.getCoOccurrence()) / 5.0;
            edge.setWeight(Math.min(weight, 1.0));

            edge.setUpdatedAt(LocalDateTime.now());
        }
        cache.put(key, edge.getWeight());
        dirtyKeys.add(key);
    }

    private void saveWithRetry(SkillEdge edge) {

        int maxRetries = 3;

        for (int i = 0; i < maxRetries; i++) {
            try {
                repository.save(edge);
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {

                // reload latest state
                SkillEdge latest = repository
                        .findBySkillAAndSkillB(edge.getSkillA(), edge.getSkillB())
                        .orElseThrow();

                // merge changes
                latest.setCoOccurrence(latest.getCoOccurrence() + 1);

                double weight = Math.log(1.0 + latest.getCoOccurrence()) / 5.0;
                latest.setWeight(Math.min(weight, 1.0));
                latest.setUpdatedAt(LocalDateTime.now());

                edge = latest; // retry with fresh version
            }
        }

        throw new ConflictException("Failed after retries");
    }

    // 🔹 Get similarity
    @Override
    public double getSimilarity(String a, String b) {

        a = normalize(a);
        b = normalize(b);

        if (a.equals(b))
            return 1.0;

        String key = a + "|" + b;

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        double value = repository
                .findBySkillAAndSkillB(a, b)
                .map(SkillEdge::getWeight)
                .orElse(0.0);

        cache.put(key, value);
        return value;
    }

    @PostConstruct
    public void preload() {
        List<SkillEdge> edges = repository.findAll();

        for (SkillEdge e : edges) {
            String key = e.getSkillA() + "|" + e.getSkillB();

            edgeStore.put(key, e);
            cache.put(key, e.getWeight());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void flushToDatabase() {

        synchronized (flushLock) {
            if (dirtyKeys.isEmpty())
                return;

            List<SkillEdge> batch = new ArrayList<>();

            for (String key : dirtyKeys) {
                SkillEdge edge = edgeStore.get(key);
                if (edge != null) {
                    batch.add(edge);
                }
            }

            for (SkillEdge edge : batch) {
                saveWithRetry(edge);
            }
            dirtyKeys.clear();
        }
    }

    @PreDestroy
    public void flushBeforeShutdown() {

        synchronized (flushLock) {
            flushToDatabase();
        }
    }

}