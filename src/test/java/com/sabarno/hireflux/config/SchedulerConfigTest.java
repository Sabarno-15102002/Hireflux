package com.sabarno.hireflux.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ScheduledThreadPoolExecutor;

class SchedulerConfigTest {

        @Test
        void testTaskScheduler_shouldCreateSchedulerWithCorrectConfiguration() {

                SchedulerConfig config = new SchedulerConfig();

                ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) config.taskScheduler();

                assertNotNull(scheduler);

                assertEquals(
                                "kafka-retry-",
                                scheduler.getThreadNamePrefix());

                ScheduledThreadPoolExecutor executor = scheduler.getScheduledThreadPoolExecutor();

                assertEquals(
                                2,
                                executor.getCorePoolSize());

                scheduler.shutdown();
        }
}