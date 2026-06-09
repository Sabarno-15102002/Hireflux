package com.sabarno.hireflux.service.matching;

public interface MatchingStrategy {

    double calculate(MatchContext context);

    double weight();

    String name();
}
