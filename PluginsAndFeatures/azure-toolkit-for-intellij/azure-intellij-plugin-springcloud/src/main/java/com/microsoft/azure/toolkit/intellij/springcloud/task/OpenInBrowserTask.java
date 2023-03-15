/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.task;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;

import javax.annotation.Nonnull;

import java.util.Objects;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;

public class OpenInBrowserTask implements Task {
    public static final String SPRING_APP = "springApp";

    private final ComponentContext context;

    public OpenInBrowserTask(@Nonnull final ComponentContext context) {
        this.context = context;
    }

    @Override
    public void execute() {
        final SpringCloudApp app = Objects.requireNonNull((SpringCloudApp) context.getParameter(SPRING_APP),
                "`springApp` is required to open test endpoint");
        AzureActionManager.getInstance().getAction(OPEN_URL).handle(app.getApplicationUrl());
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.springcloud.open_in_browser";
    }
}
