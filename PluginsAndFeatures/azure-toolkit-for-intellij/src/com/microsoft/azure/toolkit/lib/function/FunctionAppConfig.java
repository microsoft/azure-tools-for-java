/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.function;

import com.microsoft.azure.toolkit.lib.appservice.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
public class FunctionAppConfig extends AppServiceConfig {
    public static final Platform DEFAULT_PLATFORM = Platform.AzureFunction.Windows_Java8;
    @Builder.Default
    protected Platform platform = DEFAULT_PLATFORM;

    public static FunctionAppConfig getFunctionAppDefaultConfig() {
        return FunctionAppConfig.builder()
                                .platform(FunctionAppConfig.DEFAULT_PLATFORM)
                                .pricingTier(AzureFunctionMvpModel.CONSUMPTION_PRICING_TIER)
                                .region(AppServiceConfig.DEFAULT_REGION).build();
    }
}
