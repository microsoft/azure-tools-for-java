/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;

@Slf4j
public class WhatsNewStartupActivity implements ProjectActivity, DumbAware {
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        Mono.delay(Duration.ofSeconds(5)).subscribe(next -> {
            if (project.isDisposed()) {
                return;
            }
            final AnAction action = ActionManager.getInstance().getAction(WhatsNewAction.ID);
            final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
            AzureTaskManager.getInstance().runLater(() -> ActionUtil.invokeAction(action, context, "AzurePluginStartupActivity", null, null));
        }, error -> log.warn("error occurs when opening what's new.", error));
        return null;
    }
}
