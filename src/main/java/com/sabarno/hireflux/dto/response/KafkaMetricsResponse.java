package com.sabarno.hireflux.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KafkaMetricsResponse {
    private long successfulProcesses;
    private long failedProcesses;
    private long retryCount;
    private long dlqCount;
}
