/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.webapp;

import com.microsoft.azure.toolkit.lib.appservice.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
public class WebAppConfig extends AppServiceConfig {
    public static final Runtime DEFAULT_RUNTIME = Runtime.LINUX_JAVA8_TOMCAT9;
    public static final PricingTier DEFAULT_PRICING_TIER = PricingTier.BASIC_B2;
    @Builder.Default
    protected Runtime runtime = DEFAULT_RUNTIME;

    public static WebAppConfig getWebAppDefaultConfig() {
        return WebAppConfig.builder()
                           .runtime(WebAppConfig.DEFAULT_RUNTIME)
                           .pricingTier(WebAppConfig.DEFAULT_PRICING_TIER)
                           .region(AppServiceConfig.DEFAULT_REGION).build();
    }
}
