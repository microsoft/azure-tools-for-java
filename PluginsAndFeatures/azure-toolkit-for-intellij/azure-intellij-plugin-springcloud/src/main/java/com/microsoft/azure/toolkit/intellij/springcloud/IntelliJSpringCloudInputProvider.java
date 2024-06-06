/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.config.InputConfig;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInput;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInputProvider;
import com.microsoft.azure.toolkit.intellij.springcloud.input.SpringAppClusterInput;
import com.microsoft.azure.toolkit.intellij.springcloud.input.SpringAppNameInput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IntelliJSpringCloudInputProvider implements GuidanceInputProvider {
    @Nullable
    @Override
    public GuidanceInput<?> createInputComponent(@Nonnull InputConfig config, @Nonnull Context context) {
        final ComponentContext inputContext = new ComponentContext(config, context);
        return switch (config.getName()) {
            case "input.springcloud.cluster" -> new SpringAppClusterInput(config, inputContext);
            case "input.springcloud.name" -> new SpringAppNameInput(config, inputContext);
            default -> null;
        };
    }
}
