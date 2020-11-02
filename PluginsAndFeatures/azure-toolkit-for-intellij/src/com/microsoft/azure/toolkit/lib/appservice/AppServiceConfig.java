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

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Data
@SuperBuilder
public class AppServiceConfig {
    public static final PricingTier DEFAULT_PRICING_TIER = PricingTier.BASIC_B2;
    @Builder.Default
    private MonitorConfig monitorConfig = MonitorConfig.builder().build();
    private String name;
    private Path application;
    @Builder.Default
    private Platform platform = WebAppConfig.DEFAULT_PLATFORM;

    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private AppServicePlan servicePlan;
    private Region region;
    @Builder.Default
    private Map<String, String> appSettings = new HashMap<>();
}
