/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.function;

import com.microsoft.azure.toolkit.lib.appservice.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class FunctionAppConfig extends AppServiceConfig {
    public static final Runtime DEFAULT_RUNTIME = Runtime.FUNCTION_WINDOWS_JAVA8;
    @Builder.Default
    protected Runtime runtime = DEFAULT_RUNTIME;

    public static FunctionAppConfig getFunctionAppDefaultConfig() {
        return FunctionAppConfig.builder()
                                .runtime(FunctionAppConfig.DEFAULT_RUNTIME)
                                .pricingTier(PricingTier.CONSUMPTION)
                                .region(AppServiceConfig.getDefaultRegion()).build();
    }

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> result = super.getTelemetryProperties();
        result.put("runtime", Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).map(OperatingSystem::getValue).orElse(StringUtils.EMPTY));
        result.put("functionJavaVersion", Optional.ofNullable(runtime).map(Runtime::getJavaVersion).map(JavaVersion::getValue).orElse(StringUtils.EMPTY));
        return result;
    }
}
