/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.runner;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfigurationFactory;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class WebAppConfigurationType implements ConfigurationType {

    private static final String ID = "com.microsoft.intellij.run.configuration.WebAppConfigurationType";
    private static final String DISPLAY_NAME = "Azure Web App";

    public static WebAppConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(WebAppConfigurationType.class);
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getConfigurationTypeDescription() {
        return DISPLAY_NAME;
    }

    @Override
    public Icon getIcon() {
        return AzureIconLoader.loadIcon(AzureIconSymbol.WebApp.DEPLOY);
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new WebAppConfigurationFactory(this)};
    }

    public WebAppConfigurationFactory getWebAppConfigurationFactory() {
        return new WebAppConfigurationFactory(this);
    }
}
