package com.sabarno.hireflux.service;

import com.sabarno.hireflux.dto.response.KafkaMetricsResponse;

public interface MetricsService {

    void incrementResumeSuccess();
    void incrementResumeFailure();
    void incrementResumeRetry();
    void incrementResumeDlq();
    void recordResumeProcessingTime(long ms);
    KafkaMetricsResponse getKafkaMetrics();
}