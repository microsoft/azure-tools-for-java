/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.launch.local;

import com.microsoft.azure.toolkit.eclipse.function.launch.LaunchConfigurationUtils;
import com.microsoft.azure.toolkit.eclipse.function.model.FunctionLocalRunConfiguration;
import com.microsoft.azure.toolkit.eclipse.function.utils.FunctionUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azuretools.core.actions.MavenExecuteAction;
import com.microsoft.azuretools.core.utils.MavenUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedJavaLaunchDelegate;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

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
        // run `mvn compile` first to generate .class files which are required before generating function staging folder
        final MavenExecuteAction action = new MavenExecuteAction("compile");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        action.launch(MavenUtils.getPomFile(project.getProject()).getParent(), () -> {
            countDownLatch.countDown();
            return "ignore";
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new CoreException(Status.CANCEL_STATUS);
        }
        File tempFolder = FunctionUtils.getTempStagingFolder();
        try {
            FileUtils.cleanDirectory(tempFolder);
            IFile file = project.getProject().getFile("host.json");
            FunctionUtils.prepareStagingFolder(project, tempFolder.toPath(),
                    file.exists() ? Paths.get(file.getLocation().toOSString()) : null,
                    Paths.get(config.getLocalSettingsJsonPath())
            );
        } catch (Throwable e) {
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
            return new AzureFunctionRunner(vm, func, stagingFolder);
        } else if (ILaunchManager.DEBUG_MODE.equals(mode)) {
            return new AzureFunctionDebugger(vm, func, stagingFolder);
        }
        return null;
    }
}
