/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.runner;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.icons.AllIcons;
import com.microsoft.azure.toolkit.intellij.springcloud.runner.deploy.SpringCloudDeploymentConfigurationFactory;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;

import static com.microsoft.azure.toolkit.intellij.springcloud.runner.SpringCloudConstants.AZURE_SPRING_CLOUD_ICON;

public class SpringCloudConfigurationType extends ConfigurationTypeBase implements ConfigurationType {

    public static final String ICON_PATH = "/icons/" + AZURE_SPRING_CLOUD_ICON;

    protected SpringCloudConfigurationType() {
        super("AZURE_SPRING_CLOUD_SUPPORT", SpringCloudConstants.DISPLAY_NAME, "Execute the Azure Spring Cloud Services", AllIcons.Actions.Execute);
        addFactory(new SpringCloudDeploymentConfigurationFactory(this));
    }

    public static SpringCloudConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(SpringCloudConfigurationType.class);
    }

    @Override
    public String getDisplayName() {
        return SpringCloudConstants.DISPLAY_NAME;
    }

    @Override
    public Icon getIcon() {
        return PluginUtil.getIcon(ICON_PATH);
    }
}
