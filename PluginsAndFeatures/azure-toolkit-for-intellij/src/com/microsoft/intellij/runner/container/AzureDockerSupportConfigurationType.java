/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.runner.container;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.microsoft.intellij.runner.container.dockerhost.DockerHostRunConfigurationFactory;
import com.microsoft.intellij.runner.container.pushimage.PushImageRunConfigurationFactory;
import com.microsoft.intellij.runner.container.webapponlinux.WebAppOnLinuxDeployConfigurationFactory;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class AzureDockerSupportConfigurationType implements ConfigurationType {
    private static final String ICON_PATH = "/icons/PublishWebAppOnLinux_16.png";

    public static AzureDockerSupportConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(AzureDockerSupportConfigurationType.class);
    }

    @Override
    public String getDisplayName() {
       return "Azure Docker Support";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Azure Docker Support Configuration Type";
    }

    @Override
    public Icon getIcon() {
        return PluginUtil.getIcon(ICON_PATH);
    }

    @NotNull
    @Override
    public String getId() {
        return "AZURE_DOCKER_SUPPORT_CONFIGURATION";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        // CAUTION: the order cannot be changed, referenced by index in other places.
        return new ConfigurationFactory[]{
                new WebAppOnLinuxDeployConfigurationFactory(this),
                new DockerHostRunConfigurationFactory(this),
                new PushImageRunConfigurationFactory(this),
        };
    }

    public WebAppOnLinuxDeployConfigurationFactory getWebAppOnLinuxDeployConfigurationFactory() {
        return new WebAppOnLinuxDeployConfigurationFactory(this);
    }

    public DockerHostRunConfigurationFactory getDockerHostRunConfigurationFactory() {
        return new DockerHostRunConfigurationFactory(this);
    }

    public PushImageRunConfigurationFactory getPushImageRunConfigurationFactory() {
        return new PushImageRunConfigurationFactory(this);
    }
}
