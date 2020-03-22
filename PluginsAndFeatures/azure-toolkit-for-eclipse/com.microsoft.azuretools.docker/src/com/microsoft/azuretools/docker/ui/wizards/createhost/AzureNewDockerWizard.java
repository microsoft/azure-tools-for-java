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
package com.microsoft.azuretools.docker.ui.wizards.createhost;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_DOCKER_HOST;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerPreferredSettings;
import com.microsoft.azure.docker.model.AzureDockerSubscription;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class AzureNewDockerWizard extends Wizard implements TelemetryProperties {
    private static final Logger log =  Logger.getLogger(AzureNewDockerWizard.class.getName());

    private AzureNewDockerConfigPage azureNewDockerConfigPage;
    private AzureNewDockerLoginPage azureNewDockerLoginPage;

    private IProject project;
    private AzureDockerHostsManager dockerManager;
    private DockerHost newHost;
    private AzureDockerSubscription subscription;

    public AzureNewDockerWizard(final IProject project, AzureDockerHostsManager dockerManager) {
        this.project = project;
        this.dockerManager = dockerManager;

        newHost = dockerManager.createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(project.getName())));

        azureNewDockerConfigPage = new AzureNewDockerConfigPage(this);
        azureNewDockerLoginPage = new AzureNewDockerLoginPage(this);

        setWindowTitle("Create Docker Host");
    }

    @Override
    public void addPages() {
        addPage(azureNewDockerConfigPage);
        addPage(azureNewDockerLoginPage);
    }

    @Override
    public boolean performFinish() {
        return doValidate();
    }

    public boolean doValidate() {
        return azureNewDockerConfigPage.doValidate() && azureNewDockerLoginPage.doValidate();
    }

    public void setNewDockerHost(DockerHost dockerHost) {
        newHost = dockerHost;
    }

    public DockerHost getDockerHost() {
        return newHost;
    }

    public IProject getProject() {
        return project;
    }

    public AzureDockerHostsManager getDockerManager() {
        return dockerManager;
    }

    public void createHost() {
        AzureDockerPreferredSettings dockerPreferredSettings = dockerManager.getDockerPreferredSettings();

        if (dockerPreferredSettings == null) {
            dockerPreferredSettings = new AzureDockerPreferredSettings();
        }
        dockerPreferredSettings.dockerApiName = newHost.apiUrl;
        dockerPreferredSettings.region = newHost.hostVM.region;
        dockerPreferredSettings.vmSize = newHost.hostVM.vmSize;
        dockerPreferredSettings.vmOS = newHost.hostOSType.name();
        dockerManager.setDockerPreferredSettings(dockerPreferredSettings);

        Job createDockerHostJob = new Job("Creating Docker virtual machine " + newHost.name) {
            @Override
            protected IStatus run(IProgressMonitor progressMonitor) {
                progressMonitor.beginTask("start task", 100);
                Operation operation = TelemetryManager.createOperation(WEBAPP, CREATE_DOCKER_HOST);
                operation.start();
                try {
                    DockerHost dockerHost = newHost;

                    progressMonitor.subTask(String.format("Reading subscription details for Docker host %s ...", dockerHost.apiUrl));
                    progressMonitor.worked(5);
                    Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
                    if (progressMonitor.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 0) {
                            progressMonitor.done();
                            return Status.CANCEL_STATUS;
                        }
                    }

                    progressMonitor.subTask(String.format("Creating new virtual machine %s ...", dockerHost.name));
                    progressMonitor.worked(10);
                    if (AzureDockerUtils.DEBUG) System.out.println("Creating new virtual machine: " + new Date().toString());
                    AzureDockerVMOps.createDockerHostVM(azureClient, dockerHost);
                    if (AzureDockerUtils.DEBUG) System.out.println("Done creating new virtual machine: " + new Date().toString());
                    if (progressMonitor.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 0) {
                            progressMonitor.done();
                            return Status.CANCEL_STATUS;
                        }
                    }

                    progressMonitor.subTask("Getting the new Docker virtual machines details...");
                    progressMonitor.worked(5);
                    if (AzureDockerUtils.DEBUG) System.out.println("Getting the new Docker hosts details: " + new Date().toString());
                    VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
                    if (vm != null) {
                        DockerHost updatedHost = AzureDockerVMOps.getDockerHost(vm, dockerManager.getDockerVaultsMap());
                        if (updatedHost != null) {
                            dockerHost.hostVM = updatedHost.hostVM;
                            dockerHost.apiUrl = updatedHost.apiUrl;
                        }
                    }
                    if (AzureDockerUtils.DEBUG) System.out.println("Done getting the new Docker hosts details: " + new Date().toString());
                    if (progressMonitor.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 0) {
                            progressMonitor.done();
                            return Status.CANCEL_STATUS;
                        }
                    }

                    progressMonitor.subTask(String.format("Waiting for virtual machine %s to be up...", dockerHost.name));
                    progressMonitor.worked(55);
                    if (AzureDockerUtils.DEBUG) System.out.println("Waiting for virtual machine to be up: " + new Date().toString());
                    AzureDockerVMOps.waitForVirtualMachineStartup(azureClient, dockerHost);
                    if (AzureDockerUtils.DEBUG) System.out.println("Done Waiting for virtual machine to be up: " + new Date().toString());
                    if (progressMonitor.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 0) {
                            progressMonitor.done();
                            return Status.CANCEL_STATUS;
                        }
                    }

                    progressMonitor.subTask(String.format("Configuring Docker service for %s ...", dockerHost.apiUrl));
                    progressMonitor.worked(15);
                    if (AzureDockerUtils.DEBUG) System.out.println("Configuring Docker host: " + new Date().toString());
                    AzureDockerVMOps.installDocker(dockerHost);
                    if (AzureDockerUtils.DEBUG) System.out.println("Done configuring Docker host: " + new Date().toString());
                    if (AzureDockerUtils.DEBUG) System.out.println("Finished setting up Docker host");
                    if (progressMonitor.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 0) {
                            progressMonitor.done();
                            return Status.CANCEL_STATUS;
                        }
                    }

                    if (dockerHost.certVault != null && dockerHost.certVault.hostName != null) {
                        AzureDockerUIResources.createDockerKeyVault(dockerHost, dockerManager);
                    }

                    progressMonitor.subTask("Refreshing the Docker virtual machines details...");
                    progressMonitor.worked(5);
                    if (AzureDockerUtils.DEBUG) System.out.println("Refreshing Docker hosts details: " + new Date().toString());
                    vm = azureClient.virtualMachines().getByResourceGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
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
                                AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.ADD, updatedHost));
                            }
                        }
                    }
                    if (AzureDockerUtils.DEBUG) System.out.println("Done refreshing Docker hosts details: " + new Date().toString());
                    if (progressMonitor.isCanceled()) {
                        if (displayWarningOnCreateHostCancelAction() == 0) {
                            progressMonitor.done();
                            return Status.CANCEL_STATUS;
                        }
                    }

                    progressMonitor.done();
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    String msg = "An error occurred while attempting to create Docker host." + "\n" + e.getMessage();
                    log.log(Level.SEVERE, "createHost: " + msg, e);
                    e.printStackTrace();
                    EventUtil.logError(operation, ErrorType.systemError, e, null, null);
                    return Status.CANCEL_STATUS;
                } finally {
                    operation.complete();
                }

//                progressMonitor.subTask("");
//                progressMonitor.worked(1);
//                if (progressMonitor.isCanceled()) {
//                    if (displayWarningOnCreateHostCancelAction() == 0) {
//                        progressMonitor.done();
//                        return Status.CANCEL_STATUS;
//                    }
//                }
//
//                for (int i = 0; i < 10; i++) {
//                    try {
//                        Thread.sleep(3000);
//                        progressMonitor.subTask("doing " + i);
//                        // Report that 10 units are done
//                        progressMonitor.worked(10);
//                    } catch (InterruptedException e1) {
//                        e1.printStackTrace();
//                    }
//                }
            }
        };

        createDockerHostJob.schedule();
//        DefaultLoader.getIdeHelper().runInBackground(null, "Creating Docker virtual machine " + newHost.name + "...", false, true, "Creating Docker virtual machine " + newHost.name + "...", new Runnable() {
//            @Override
//            public void run() {
//                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
//                    @Override
//                    public void run() {
//
//                    }
//                });
//            }
//        });
    }

    private int displayWarningOnCreateHostCancelAction() {
        Display currentDisplay = Display.getCurrent();
        Shell shell = currentDisplay.getActiveShell();

        if (shell != null) {
            MessageBox displayConfirmationDialog = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
            displayConfirmationDialog.setText("Stop Create Docker Host");
            displayConfirmationDialog.setMessage("This action can leave the Docker host virtual machine in an partial setup state and which can cause publishing to a Docker container to fail!\n\n Are you sure you want this?");
            return displayConfirmationDialog.open();
        }

        return 1;
    }

    public void setSubscription(AzureDockerSubscription subscription) {
        this.subscription = subscription;
    }

    public AzureDockerSubscription getSubscription() {
        return subscription;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        if(this.getSubscription() != null) {
            properties.put("SubscriptionName", this.getSubscription().name);
            properties.put("SubscriptionId", this.getSubscription().id);
        }

        return properties;
    }

}
