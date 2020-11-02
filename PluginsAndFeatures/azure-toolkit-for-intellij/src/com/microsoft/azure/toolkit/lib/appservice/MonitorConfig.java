/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
    Integer webServerRetentionPeriod = 0;
    @Builder.Default
    boolean enableDetailedErrorMessage = false;
    @Builder.Default
    boolean enableFailedRequestTracing = false;
    // application log
    @Builder.Default
    boolean enableApplicationLog = true;
    @Builder.Default
    LogLevel applicationLogLevel = LogLevel.ERROR;
}
