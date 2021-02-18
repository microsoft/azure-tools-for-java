/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.icons.AllIcons;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;

public class SpringCloudDeploymentConfigurationType extends ConfigurationTypeBase implements ConfigurationType {
    private static final String ID = "AZURE_SPRING_CLOUD_SUPPORT";
    private static final String DISPLAY_NAME = "Azure Spring Cloud Services";
    private static final String ICON_PATH = "/icons/azure-springcloud-small.png";
    private static final String DESCRIPTION = "Execute the Azure Spring Cloud Services";

    protected SpringCloudDeploymentConfigurationType() {
        super(ID, DISPLAY_NAME, DESCRIPTION, AllIcons.Actions.Execute);
        addFactory(new SpringCloudDeploymentConfiguration.Factory(this));
    }

    public static SpringCloudDeploymentConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(SpringCloudDeploymentConfigurationType.class);
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public Icon getIcon() {
        return PluginUtil.getIcon(ICON_PATH);
    }
}
