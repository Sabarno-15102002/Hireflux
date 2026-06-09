package com.sabarno.hireflux.service.matching;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobMatchingEngine {

    private final List<MatchingStrategy> strategies;

    public double calculate(MatchContext context) {

        return strategies.stream()
                .mapToDouble(strategy -> {

                    double score =
                            strategy.calculate(context);

                    return score * strategy.weight();
                })
                .sum();
    }
}