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
 *
 */

package com.microsoft.intellij.runner.functions;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeploymentConfigurationFactory;
import com.microsoft.intellij.runner.functions.localrun.FunctionRunConfigurationFactory;
import com.microsoft.intellij.util.PluginUtil;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class AzureFunctionSupportConfigurationType implements ConfigurationType {

    public static final String ICON_PATH = "/icons/azure-functions-small.png";

    public static AzureFunctionSupportConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(AzureFunctionSupportConfigurationType.class);
    }

    @Override
    public String getDisplayName() {
        return AzureFunctionsConstants.DISPLAY_NAME;
    }

    @Override
    public String getConfigurationTypeDescription() {
        return AzureFunctionsConstants.DISPLAY_NAME;
    }

    @Override
    public Icon getIcon() {
        return PluginUtil.getIcon(ICON_PATH);
    }

    @NotNull
    @Override
    public String getId() {
        return "AZURE_FUNCTION_SUPPORT";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        // Todo: update after commit specified configuration factory
        return new ConfigurationFactory[] { new FunctionDeploymentConfigurationFactory(this), new FunctionRunConfigurationFactory(this) };
    }
}
