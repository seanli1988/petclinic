package com.example.demo.configuration;

import java.time.Duration;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshEndpointAutoConfiguration;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@AutoConfigureAfter({RefreshAutoConfiguration.class, RefreshEndpointAutoConfiguration.class})
@EnableScheduling
public class AutoRefreshConfiguration implements SchedulingConfigurer {
    @Value("${spring.cloud.config.refresh-interval:60}")
    private long refreshInterval;
    @Value("${spring.cloud.config.auto-refresh:false}")
    private boolean autoRefresh;
    private final RefreshEndpoint refreshEndpoint;

    public AutoRefreshConfiguration(RefreshEndpoint refreshEndpoint) {
        this.refreshEndpoint = refreshEndpoint;
    }

    @Override
    public void configureTasks(@NotNull ScheduledTaskRegistrar scheduledTaskRegistrar) {
        if (autoRefresh) {
            // set minimal refresh interval to 5 seconds
            refreshInterval = Math.max(refreshInterval, 5);
            scheduledTaskRegistrar.addFixedRateTask(refreshEndpoint::refresh,  Duration.ofSeconds(refreshInterval));
        }
    }
}
