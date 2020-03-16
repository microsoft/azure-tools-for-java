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
package com.microsoft.intellij.runner.functions;

import com.intellij.execution.Location;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiMethod;
import com.microsoft.intellij.actions.RunConfigurationUtils;
import com.microsoft.intellij.runner.AzureRunConfigurationBase;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeployConfiguration;
import com.microsoft.intellij.runner.functions.localrun.FunctionRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jsoup.helper.StringUtil;

import java.util.Iterator;

public class FunctionRunConfigurationProducer extends LazyRunConfigurationProducer<AzureRunConfigurationBase> {
    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return AzureFunctionSupportConfigurationType.getInstance().getConfigurationFactories()[0];
    }

    @Override
    protected boolean setupConfigurationFromContext(AzureRunConfigurationBase configuration, ConfigurationContext context, Ref ref) {
        if (!(configuration instanceof FunctionRunConfiguration || configuration instanceof FunctionDeployConfiguration)) {
            return false;
        }

        final Location contextLocation = context.getLocation();
        assert contextLocation != null;
        Location<PsiMethod> methodLocation = getAzureFunctionMethods(contextLocation);
        if (methodLocation == null) {
            return false;
        }
        if (configuration instanceof FunctionDeployConfiguration) {
            final RunManagerEx manager = RunManagerEx.getInstanceEx(context.getProject());
            // since deploy configuration doesn't support, we need to create a FunctionRunConfiguration
            final RunnerAndConfigurationSettings settings = RunConfigurationUtils.getOrCreateRunConfigurationSettings(
                    context.getModule(), manager, getConfigurationFactory());
            configuration = (AzureRunConfigurationBase) settings.getConfiguration();
        }

        FunctionRunConfiguration runConfiguration = (FunctionRunConfiguration) configuration;
        final RunnerAndConfigurationSettings template = context.getRunManager().getConfigurationTemplate(getConfigurationFactory());

        final Module contextModule = context.getModule();
        final Module predefinedModule = ((FunctionRunConfiguration) template.getConfiguration()).getModule();
        if (predefinedModule != null) {
            runConfiguration.initializeDefaults(predefinedModule);
        } else {
            final Module module = findModule(runConfiguration, contextModule);
            if (module != null) {
                runConfiguration.initializeDefaults(module);
            }
        }
        if (StringUtil.isBlank(configuration.getName())) {
            configuration.setName("Run Functions - " + runConfiguration.getModule().getName());
        }
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(AzureRunConfigurationBase appConfiguration, ConfigurationContext context) {
        if (!(appConfiguration instanceof FunctionRunConfiguration)) {
            return false;
        }
        Location<PsiMethod> methodLocation = getAzureFunctionMethods(context.getLocation());
        if (methodLocation == null) {
            return false;
        }

        final Module configurationModule = ((FunctionRunConfiguration) appConfiguration).getModule();
        if (Comparing.equal(context.getModule(), configurationModule)) {
            return true;
        }
        return false;
    }

    private static Location<PsiMethod> getAzureFunctionMethods(final Location<?> location) {
        for (Iterator<Location<PsiMethod>> iterator = location.getAncestors(PsiMethod.class, false); iterator.hasNext();) {
            final Location<PsiMethod> methodLocation = iterator.next();
            if (FunctionUtils.isFunctionClassAnnotated(methodLocation.getPsiElement())) {
                return methodLocation;
            }
        }
        return null;
    }

    private Module findModule(FunctionRunConfiguration configuration, Module contextModule) {
        if (configuration.getModule() == null && contextModule != null) {
            return contextModule;
        }
        return null;
    }
}
