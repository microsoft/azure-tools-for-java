/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.docker.wizards.createhost;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_DOCKER_HOST;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardDialog;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerPreferredSettings;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class AzureNewDockerWizardDialog extends WizardDialog<AzureNewDockerWizardModel> {
    private static final Logger LOGGER = Logger.getInstance(AzureNewDockerWizardDialog.class);
    private AzureNewDockerWizardModel model;
    private Runnable onCreate;

    public AzureNewDockerWizardDialog(AzureNewDockerWizardModel model) {
        super(model.getProject(), true, model);
        this.model = model;
        model.setNewDockerWizardDialog(this);
        this.onCreate = null;
        if (model.getDockerHost().hostVM.region == null) {
            model.getCurrentNavigationState().NEXT.setEnabled(false);
            model.getCurrentNavigationState().FINISH.setEnabled(false);
        }
    }

    public void DialogShaker(ValidationInfo info) {
        PluginUtil.dialogShaker(info, this);
    }

    @Override
    public void onWizardGoalAchieved() {
        if (model.canClose()) {
            super.onWizardGoalAchieved();
        }
    }

    @Override
    public void onWizardGoalDropped() {
        if (model.canClose()) {
            super.onWizardGoalDropped();
        }
    }

    @Override
    protected Dimension getWindowPreferredSize() {
        return new Dimension(600, 400);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return model.doValidate();
    }

    @Override
    protected void doOKAction() {
        if (isOKActionEnabled()) {
            super.doOKAction();
        }
    }

    public void create() {
        DockerHost dockerHost = model.getDockerHost();

        AzureDockerPreferredSettings dockerPreferredSettings = model.getDockerManager().getDockerPreferredSettings();

        if (dockerPreferredSettings == null) {
            dockerPreferredSettings = new AzureDockerPreferredSettings();
        }
        dockerPreferredSettings.dockerApiName = dockerHost.apiUrl;
        dockerPreferredSettings.region = dockerHost.hostVM.region;
        dockerPreferredSettings.vmSize = dockerHost.hostVM.vmSize;
        dockerPreferredSettings.vmOS = dockerHost.hostOSType.name();
        model.getDockerManager().setDockerPreferredSettings(dockerPreferredSettings);

        ProgressManager.getInstance().run(new Task.Backgroundable(model.getProject(),
                                                                  "Creating Docker Host on Azure...",
                                                                  true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                Operation operation = TelemetryManager.createOperation(WEBAPP, CREATE_DOCKER_HOST);
                operation.start();
                try {
                    progressIndicator.setFraction(.05);
                    progressIndicator.setText2(String.format("Reading subscription details for Docker host %s ...",
                                                             dockerHost.apiUrl));
                    if (progressIndicator.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 1) {
                            return;
                        }
                    }

                    progressIndicator.setFraction(.10);
                    progressIndicator.setText2(String.format("Creating new virtual machine %s ...", dockerHost.name));
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println(
                                "Creating new virtual machine: " + new Date().toString());
                    }

                    AzureDockerHostsManager dockerManager = model.getDockerManager();
                    Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
                    AzureDockerVMOps.createDockerHostVM(azureClient, dockerHost);
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println(
                                "Done creating new virtual machine: " + new Date().toString());
                    }
                    if (progressIndicator.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 1) {
                            return;
                        }
                    }

                    progressIndicator.setFraction(.60);
                    progressIndicator.setIndeterminate(true);
                    progressIndicator.setText2("Getting the new Docker virtual machines details...");
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println(
                                "Getting the new Docker virtual machines details: " + new Date().toString());
                    }
                    // dockerManager.refreshDockerHostDetails();
                    VirtualMachine vm =
                            azureClient.virtualMachines().getByResourceGroup(dockerHost.hostVM.resourceGroupName,
                                                                             dockerHost.hostVM.name);
                    if (vm != null) {
                        DockerHost updatedHost = AzureDockerVMOps.getDockerHost(vm, dockerManager.getDockerVaultsMap());
                        if (updatedHost != null) {
                            dockerHost.hostVM = updatedHost.hostVM;
                            dockerHost.apiUrl = updatedHost.apiUrl;
                        }
                    }
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println(
                                "Done getting the new Docker virtual machines details: " + new Date().toString());
                    }
                    if (progressIndicator.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 1) {
                            return;
                        }
                    }

                    progressIndicator.setFraction(.65);
                    progressIndicator.setText2(String.format("Waiting for virtual machine %s to be up...",
                                                             dockerHost.name));
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println(
                                "Waiting for virtual machine to be up: " + new Date().toString());
                    }
                    AzureDockerVMOps.waitForVirtualMachineStartup(azureClient, dockerHost);
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println(
                                "Done Waiting for virtual machine to be up: " + new Date().toString());
                    }
                    if (progressIndicator.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 1) {
                            return;
                        }
                    }

                    progressIndicator.setFraction(.75);
                    progressIndicator.setText2(String.format("Configuring Docker service for %s ...",
                                                             dockerHost.apiUrl));
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println("Configuring Docker host: " + new Date().toString());
                    }
                    AzureDockerVMOps.installDocker(dockerHost);
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println(
                                "Done configuring Docker host: " + new Date().toString());
                    }
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println("Finished setting up Docker host");
                    }
                    if (progressIndicator.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 1) {
                            return;
                        }
                    }

                    if (dockerHost.certVault != null && dockerHost.certVault.hostName != null) {
                        AzureDockerUIResources.createDockerKeyVault(model.getProject(), dockerHost, dockerManager);
                    }

                    progressIndicator.setFraction(.90);
                    progressIndicator.setIndeterminate(true);
                    progressIndicator.setText2("Refreshing the Docker virtual machines details...");
                    if (AzureDockerUtils.DEBUG) {
                        System.out.println(
                                "Refreshing Docker hosts details: " + new Date().toString());
                    }
                    // dockerManager.refreshDockerHostDetails();
                    vm = azureClient.virtualMachines().getByResourceGroup(dockerHost.hostVM.resourceGroupName,
                                                                          dockerHost.hostVM.name);
                    if (vm != null) {
                        DockerHost updatedHost = AzureDockerVMOps.getDockerHost(vm, dockerManager.getDockerVaultsMap());
                        if (updatedHost != null) {
                            updatedHost.sid = dockerHost.sid;
                            updatedHost.hostVM.sid = dockerHost.hostVM.sid;
                            if (updatedHost.certVault == null) {
                                updatedHost.certVault = dockerHost.certVault;
                                updatedHost.hasPwdLogIn = dockerHost.hasPwdLogIn;
                                updatedHost.hasSSHLogIn = dockerHost.hasSSHLogIn;
                                updatedHost.isTLSSecured = dockerHost.isTLSSecured;
                            }
                            dockerManager.addDockerHostDetails(updatedHost);
                            if (AzureUIRefreshCore.listeners != null) {
                                AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.ADD,
                                                                                   updatedHost));
                            }
                        }
                    }

                    if (AzureDockerUtils.DEBUG) {
                        System.out.println(
                                "Done refreshing Docker hosts details: " + new Date().toString());
                    }
                    if (progressIndicator.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 1) {
                            return;
                        }
                    }
                    progressIndicator.setFraction(1);
                    progressIndicator.setIndeterminate(true);

                } catch (Exception e) {
                    String msg = "An error occurred while attempting to create Docker host." + "\n" + e.getMessage();
                    LOGGER.error("Failed to Create Docker Host", e);
                    EventUtil.logError(operation, ErrorType.systemError, e, null, null);
                    PluginUtil.displayErrorDialogInAWTAndLog("Failed to Create Docker Host", msg, e);
                } finally {
                    operation.complete();
                }
            }
        });

    }

    private int displayWarningOnCreateHostCancelAction() {
        return DefaultLoader.getUIHelper().showConfirmDialog(
                null,
                "This action can leave the Docker virtual machine host in an partial setup state and which can cause "
                        + "publishing to a Docker container to fail!\n\n Are you sure you want this?",
                "Stop Create Docker Host",
                new String[]{"Cancel", "OK"},
                null,
                PluginUtil.getIcon("/icons/logwarn.png"));
    }
}
