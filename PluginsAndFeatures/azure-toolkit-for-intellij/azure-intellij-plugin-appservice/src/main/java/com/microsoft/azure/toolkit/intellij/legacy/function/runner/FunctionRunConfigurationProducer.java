/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner;

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
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun.FunctionRunConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun.FunctionRunConfigurationFactory;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.actions.RunConfigurationUtils;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;

public class FunctionRunConfigurationProducer extends LazyRunConfigurationProducer<AzureRunConfigurationBase> {
    @NotNull
    @Override
    @ExceptionNotification
    public ConfigurationFactory getConfigurationFactory() {
        return Arrays.stream(AzureFunctionSupportConfigurationType.getInstance().getConfigurationFactories())
                    .filter(configurationFactory -> configurationFactory instanceof FunctionRunConfigurationFactory)
                    .findFirst()
                    .get();
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "function.setup_run_configuration", type = AzureOperation.Type.ACTION)
    protected boolean setupConfigurationFromContext(AzureRunConfigurationBase runConfigurationBase, ConfigurationContext context, Ref ref) {
        if (!(runConfigurationBase instanceof FunctionRunConfiguration || runConfigurationBase instanceof FunctionDeployConfiguration)) {
            return false;
        }
        final Location contextLocation = context.getLocation();
        assert contextLocation != null;
        Location<PsiMethod> methodLocation = getAzureFunctionMethods(contextLocation);
        if (methodLocation == null) {
            return false;
        }
        AzureRunConfigurationBase configuration = runConfigurationBase;
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
            } else {
                return false;
            }
        }
        if (StringUtils.isBlank(configuration.getName())) {
            configuration.setName("Run Functions - " + runConfiguration.getModule().getName());
        }
        return true;
    }

    @Override
    @ExceptionNotification
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
