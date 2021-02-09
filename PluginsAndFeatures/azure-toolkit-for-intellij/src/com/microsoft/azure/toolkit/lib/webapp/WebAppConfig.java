/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.webapp;

import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
public class WebAppConfig extends AppServiceConfig {
    public static final Platform DEFAULT_PLATFORM = Platform.Linux.JAVA8_TOMCAT9;
    public static final PricingTier DEFAULT_PRICING_TIER = PricingTier.BASIC_B2;
    @Builder.Default
    protected Platform platform = DEFAULT_PLATFORM;

    public static WebAppConfig getWebAppDefaultConfig() {
        return WebAppConfig.builder()
                           .platform(WebAppConfig.DEFAULT_PLATFORM)
                           .pricingTier(WebAppConfig.DEFAULT_PRICING_TIER)
                           .region(AppServiceConfig.DEFAULT_REGION).build();
    }
}
