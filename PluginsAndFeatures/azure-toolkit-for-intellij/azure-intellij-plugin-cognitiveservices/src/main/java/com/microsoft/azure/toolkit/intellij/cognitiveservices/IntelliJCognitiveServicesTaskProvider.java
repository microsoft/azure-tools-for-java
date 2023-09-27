/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.config.TaskConfig;
import com.microsoft.azure.toolkit.ide.guidance.task.GuidanceTaskProvider;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks.CreateCognitiveDeploymentTask;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks.OpenPlaygroundTask;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks.SelectSubscriptionTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IntelliJCognitiveServicesTaskProvider implements GuidanceTaskProvider {
    @Nullable
    @Override
    public Task createTask(@Nonnull TaskConfig config, @Nonnull Context context) {
        final ComponentContext taskContext = new ComponentContext(config, context);
        switch (config.getName()) {
            case "task.openai.select_subscription":
                return new SelectSubscriptionTask(taskContext);
            case "task.openai.create_deployment":
                return new CreateCognitiveDeploymentTask(taskContext);
            case "task.open_ai.open_playground":
                return new OpenPlaygroundTask(taskContext);
            default:
                return null;
        }
    }
}
