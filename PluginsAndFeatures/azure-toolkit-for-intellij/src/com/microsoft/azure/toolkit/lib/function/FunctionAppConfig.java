/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.function;

import com.microsoft.azure.toolkit.lib.appservice.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
public class FunctionAppConfig extends AppServiceConfig {
    public static final Runtime DEFAULT_RUNTIME = Runtime.FUNCTION_WINDOWS_JAVA8;
    @Builder.Default
    protected Runtime runtime = DEFAULT_RUNTIME;

    public static FunctionAppConfig getFunctionAppDefaultConfig() {
        return FunctionAppConfig.builder()
                                .runtime(FunctionAppConfig.DEFAULT_RUNTIME)
                                .pricingTier(AzureFunctionMvpModel.CONSUMPTION_PRICING_TIER)
                                .region(AppServiceConfig.DEFAULT_REGION).build();
    }
}
