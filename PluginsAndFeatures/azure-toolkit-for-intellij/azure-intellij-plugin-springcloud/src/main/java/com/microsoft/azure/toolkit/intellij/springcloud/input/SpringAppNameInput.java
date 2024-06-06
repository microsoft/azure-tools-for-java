/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.input;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.config.InputConfig;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInput;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;

import javax.annotation.Nonnull;

public class SpringAppNameInput implements GuidanceInput<String> {
    public static final String SPRING_APP_NAME = "springAppName";
    public static final String SPRING_APP_CLUSTER = "springAppCluster";
    private final InputConfig config;
    private final ComponentContext context;

    private final SpringAppNameInputPanel inputPanel;

    public SpringAppNameInput(@Nonnull InputConfig config, @Nonnull ComponentContext context) {
        this.config = config;
        this.context = context;
        this.inputPanel = new SpringAppNameInputPanel();

        this.inputPanel.setValue((String) context.getParameter(SPRING_APP_NAME));
        this.inputPanel.setCluster((SpringCloudCluster) context.getParameter(SPRING_APP_CLUSTER));
        context.addPropertyListener(SPRING_APP_CLUSTER, cluster -> inputPanel.setCluster((SpringCloudCluster) cluster));
        context.addPropertyListener(SPRING_APP_NAME, name -> inputPanel.setValue((String) name));
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Override
    public SpringAppNameInputPanel getComponent() {
        return inputPanel;
    }

    @Override
    public void applyResult() {
        context.applyResult(SPRING_APP_NAME, inputPanel.getValue());
    }
}
