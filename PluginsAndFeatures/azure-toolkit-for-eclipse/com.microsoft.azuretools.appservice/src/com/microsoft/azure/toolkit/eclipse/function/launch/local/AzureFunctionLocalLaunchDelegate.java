/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.launch.local;

import com.microsoft.azure.toolkit.eclipse.common.launch.AzureLongDurationTaskRunnerWithConsole;
import com.microsoft.azure.toolkit.eclipse.common.launch.LaunchConfigurationUtils;
import com.microsoft.azure.toolkit.eclipse.function.jdt.EclipseFunctionProject;
import com.microsoft.azure.toolkit.eclipse.function.jdt.EclipseFunctionStagingContributor;
import com.microsoft.azure.toolkit.eclipse.function.launch.model.FunctionLocalRunConfiguration;
import com.microsoft.azure.toolkit.eclipse.function.utils.FunctionUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedJavaLaunchDelegate;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AzureFunctionLocalLaunchDelegate extends AdvancedJavaLaunchDelegate {
    @Override
    public String verifyMainTypeName(ILaunchConfiguration configuration) {
        return "ignore";
    }

    @Override
    public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
        FunctionLocalRunConfiguration config = LaunchConfigurationUtils.getFromConfiguration(configuration, FunctionLocalRunConfiguration.class);
        String projectName = config.getProjectName();
        IJavaProject project = Arrays.stream(FunctionUtils.listFunctionProjects()).filter(t -> StringUtils.equals(t.getElementName(), projectName)).findFirst().orElse(null);
        if (project == null) {
            AzureMessager.getMessager().error("Cannot find the specified project:" + projectName);
            throw new AzureToolkitRuntimeException("Cannot find the specified project:" + projectName);
        }

        if (!new File(config.getLocalSettingsJsonPath()).exists()) {
            AzureMessager.getMessager().error("Cannot find the specified local.settings.json :" + config.getLocalSettingsJsonPath());
            throw new AzureToolkitRuntimeException("Cannot find the specified local.settings.json :" + config.getLocalSettingsJsonPath());
        }

        if (!new File(config.getFunctionCliPath()).exists()) {
            AzureMessager.getMessager().error("Cannot find function cli:" + config.getFunctionCliPath());
            throw new AzureToolkitRuntimeException("Cannot find function cli:" + config.getFunctionCliPath());
        }
        File tempFolder = FunctionUtils.getTempStagingFolder().toFile();

        final EclipseFunctionStagingContributor stagingContributor = new EclipseFunctionStagingContributor();
        try {
            Mono.create(monoSink -> AzureLongDurationTaskRunnerWithConsole.getInstance().runTask("Launching function local run", () -> {
                try {
                    FileUtils.cleanDirectory(tempFolder);
                    File file = project.getProject().getFile("host.json").getLocation().toFile();
                    final EclipseFunctionProject eclipseFunctionProject = new EclipseFunctionProject(project, tempFolder);
                    eclipseFunctionProject.setHostJsonFile(file);
                    eclipseFunctionProject.buildJar();
                    stagingContributor.prepareStagingFolder(eclipseFunctionProject, false);
                    monoSink.success(true);
                } catch (Throwable e) {
                    monoSink.error(e);
                }
            }, true)).block();
        } catch (Exception e) {
            AzureMessager.getMessager().error(e);
            throw new CoreException(Status.error("Cannot prepare the staging folder for azure function.", e));
        }

        IVMInstall vm = getVMInstall(configuration);
        IVMRunner runner = getVMRunner(vm, mode, config.getFunctionCliPath(), tempFolder.getAbsolutePath());
        if (runner == null) {
            abort("Local debug Azure Function is not supported by now.", null, IJavaLaunchConfigurationConstants.ERR_VM_RUNNER_DOES_NOT_EXIST);
        }
        return runner;
    }

    private IVMRunner getVMRunner(IVMInstall vm, String mode, String func, String stagingFolder) {
        if (ILaunchManager.RUN_MODE.equals(mode)) {
            return new AzureFunctionVMRunner(vm, func, stagingFolder);
        } else if (ILaunchManager.DEBUG_MODE.equals(mode)) {
            return new AzureFunctionVMDebugger(vm, func, stagingFolder);
        }
        return null;
    }

    public Map<String, Object> getVMSpecificAttributesMap(ILaunchConfiguration configuration) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, "xgf");
        return attributes;
    }

    public String[] getClasspath(ILaunchConfiguration configuration) {
        return new String[0];
    }

    @Override
    protected boolean supportsModule() {
        return false;
    }

}
