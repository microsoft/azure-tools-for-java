package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.management.appservice.LogLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class MonitorConfig {
    ApplicationInsightsConfig applicationInsightsConfig;
    // web server log
    @Builder.Default
    boolean enableWebServerLogging = true;
    @Builder.Default
    Integer webServerLogQuota = 35;
    @Builder.Default
    Integer webServerRetentionPeriod = null;
    @Builder.Default
    boolean enableDetailedErrorMessage = false;
    @Builder.Default
    boolean enableFailedRequestTracing = false;
    // application log
    @Builder.Default
    boolean enableApplicationLog = true;
    @Builder.Default
    LogLevel applicationLogLevel = LogLevel.ERROR;
    // SSH
    @Builder.Default
    boolean enableSSH = true;
}
