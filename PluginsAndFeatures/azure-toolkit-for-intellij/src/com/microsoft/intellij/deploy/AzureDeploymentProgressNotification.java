/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.deploy;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerContainerOps;
import com.microsoft.azure.docker.ops.AzureDockerImageOps;
import com.microsoft.azure.docker.ops.AzureDockerSSHOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.activitylog.ActivityLogToolWindowFactory;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DEPLOY_WEBAPP_DOCKERHOST;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public final class AzureDeploymentProgressNotification {

    private Project myProject;

    public AzureDeploymentProgressNotification(Project project) {
        this.myProject = project;
    }

//    private OperationStatus waitForStatus(Configuration configuration, WindowsAzureServiceManagement service, String requestId)
//            throws Exception {
//        OperationStatusResponse op;
//        OperationStatus status = null;
//        do {
//            op = service.getOperationStatus(configuration, requestId);
//            status = op.getStatus();
//
//            log(message("deplId") + op.getId());
//            log(message("deplStatus") + op.getStatus());
//            log(message("deplHttpStatus") + op.getHttpStatusCode());
//            if (op.getError() != null) {
//                log(message("deplErrorMessage") + op.getError().getMessage());
//                throw new RestAPIException(op.getError().getMessage());
//            }
//
//            Thread.sleep(5000);
//
//        } while (status == OperationStatus.InProgress);
//
//        return status;
//    }

    /**
     * Unlike Eclipse plugin, here startDate is deployment start time, not the event timestamp
     */
    public void notifyProgress(String deploymentId,
                               Date startDate,
                               String deploymentURL,
                               int progress,
                               String message,
                               Object... args) {

        DeploymentEventArgs arg = new DeploymentEventArgs(this);
        arg.setId(deploymentId);
        if (deploymentURL != null) {
            try {
                new URL(deploymentURL);
            } catch (MalformedURLException e) {
                deploymentURL = null;
            }
        }
        arg.setDeploymentURL(deploymentURL);
        arg.setDeployMessage(String.format(message, args));
        arg.setDeployCompleteness(progress);
        arg.setStartTime(startDate);
        AzurePlugin.fireDeploymentEvent(arg);
    }

    private void openWindowsAzureActivityLogView(final Project project) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ToolWindowManager.getInstance(project).getToolWindow(ActivityLogToolWindowFactory.ACTIVITY_LOG_WINDOW).activate(null);
            }
        });
    }

    public void deployToWebApps(WebApp webApp, String url) {
        Date startDate = new Date();
        try {
            String msg = String.format(message("webAppDeployMsg"), webApp.name());
            notifyProgress(webApp.name(), startDate, null, 20, msg);
            Thread.sleep(2000);
            notifyProgress(webApp.name(), startDate, null, 20, msg);
            notifyProgress(webApp.name(), startDate, null, 20, msg);
            notifyProgress(webApp.name(), startDate, null, 20, msg);
            Thread.sleep(2000);
            notifyProgress(webApp.name(), startDate, url, 20, message("runStatus"), webApp.name());
        } catch (InterruptedException e) {
            notifyProgress(webApp.name(), startDate, url, 100, message("runStatus"), webApp.name());
        }
    }

    public void deployToDockerContainer(AzureDockerImageInstance dockerImageInstance, String url) {
        Date startDate = new Date();
        Map<String, String> postEventProperties = new HashMap<String, String>();
        postEventProperties.put("DockerFileOption", dockerImageInstance.predefinedDockerfile);
        String descriptionTask = String.format("Publishing %s into Docker host %s at port(s) %s", new File(dockerImageInstance.artifactPath).getName(), dockerImageInstance.host.name, dockerImageInstance.dockerPortSettings);
        Operation operation = TelemetryManager.createOperation(WEBAPP, DEPLOY_WEBAPP_DOCKERHOST);
        operation.start();
        try {
            String msg = String.format("Publishing %s to Docker host %s ...", new File(dockerImageInstance.artifactPath).getName(), dockerImageInstance.host.name);
            notifyProgress(descriptionTask, startDate, null, 5, msg);

            AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureAuthManager == null) {
                throw new RuntimeException("User not signed in");
            }
            AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(azureAuthManager);
            Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerImageInstance.sid).azureClient;
            KeyVaultClient keyVaultClient = dockerManager.getSubscriptionsMap().get(dockerImageInstance.sid).keyVaultClient;

            if (dockerImageInstance.hasNewDockerHost) {
              msg = String.format("Creating new virtual machine %s ...", dockerImageInstance.host.name);
              notifyProgress(descriptionTask, startDate, null, 10, msg);
              if (AzureDockerUtils.DEBUG) System.out.println("Creating new virtual machine: " + new Date().toString());
              AzureDockerVMOps.createDockerHostVM(azureClient, dockerImageInstance.host);
              if (AzureDockerUtils.DEBUG) System.out.println("Done creating new virtual machine: " + new Date().toString());

              msg = String.format("Get new VM details...");
              notifyProgress(descriptionTask, startDate, null, 30, msg);
              if (AzureDockerUtils.DEBUG) System.out.println("Getting the new Docker host details: " + new Date().toString());
              VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(dockerImageInstance.host.hostVM.resourceGroupName, dockerImageInstance.host.hostVM.name);
              if (vm != null) {
                DockerHost updatedHost = AzureDockerVMOps.getDockerHost(vm, dockerManager.getDockerVaultsMap());
                if (updatedHost != null) {
                  dockerImageInstance.host.hostVM = updatedHost.hostVM;
                  dockerImageInstance.host.apiUrl = updatedHost.apiUrl;
                }
              }
              if (AzureDockerUtils.DEBUG) System.out.println("Done getting the new Docker host details: " + new Date().toString());

              msg = String.format("Waiting for virtual machine to be up %s ...", dockerImageInstance.host.name);
              notifyProgress(descriptionTask, startDate, null, 35, msg);
              if (AzureDockerUtils.DEBUG) System.out.println("Waiting for virtual machine to be up: " + new Date().toString());
              AzureDockerVMOps.waitForVirtualMachineStartup(azureClient, dockerImageInstance.host);
              if (AzureDockerUtils.DEBUG) System.out.println("Done Waiting for virtual machine to be up: " + new Date().toString());

              msg = String.format("Configuring Docker service for %s ...", dockerImageInstance.host.name);
              notifyProgress(descriptionTask, startDate, null, 45, msg);
              if (AzureDockerUtils.DEBUG) System.out.println("Configuring Docker host: " + new Date().toString());
              AzureDockerVMOps.installDocker(dockerImageInstance.host);
              if (AzureDockerUtils.DEBUG) System.out.println("Done configuring Docker host: " + new Date().toString());

              msg = String.format("Updating Docker hosts ...");
              notifyProgress(descriptionTask, startDate, null, 50, msg);
              if (AzureDockerUtils.DEBUG) System.out.println("Refreshing docker hosts: " + new Date().toString());
//            dockerManager.refreshDockerHostDetails();
              vm = azureClient.virtualMachines().getByResourceGroup(dockerImageInstance.host.hostVM.resourceGroupName, dockerImageInstance.host.hostVM.name);
              if (vm != null) {
                DockerHost updatedHost = AzureDockerVMOps.getDockerHost(vm, dockerManager.getDockerVaultsMap());
                if (updatedHost != null) {
                  updatedHost.sid = dockerImageInstance.host.sid;
                  updatedHost.hostVM.sid = dockerImageInstance.host.hostVM.sid;
                  if (updatedHost.certVault == null) {
                    updatedHost.certVault = dockerImageInstance.host.certVault;
                    updatedHost.hasPwdLogIn = dockerImageInstance.host.hasPwdLogIn;
                    updatedHost.hasSSHLogIn = dockerImageInstance.host.hasSSHLogIn;
                    updatedHost.isTLSSecured = dockerImageInstance.host.isTLSSecured;
                  }
                  dockerManager.addDockerHostDetails(updatedHost);
                  if (AzureUIRefreshCore.listeners != null) {
                    AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.ADD, updatedHost));
                  }
                }
              }
              if (AzureDockerUtils.DEBUG) System.out.println("Done refreshing Docker hosts: " + new Date().toString());

              if (AzureDockerUtils.DEBUG) System.out.println("Finished setting up Docker host");
            } else {
                msg = String.format("Using virtual machine %s ...", dockerImageInstance.host.name);
                notifyProgress(descriptionTask, startDate, null, 50, msg);
            }

            if (dockerImageInstance.host.session == null) {
                if (AzureDockerUtils.DEBUG) System.out.println("Opening a remote connection to the Docker host: " + new Date().toString());
                dockerImageInstance.host.session = AzureDockerSSHOps.createLoginInstance(dockerImageInstance.host);
                if (AzureDockerUtils.DEBUG) System.out.println("Done opening a remote connection to the Docker host: " + new Date().toString());
            }

            if (dockerImageInstance.hasNewDockerHost) {
                if (dockerImageInstance.host.certVault != null && dockerImageInstance.host.certVault.hostName != null) {
                    AzureDockerUIResources.createDockerKeyVault(null, dockerImageInstance.host, dockerManager);
                }
            }

            msg = String.format("Uploading Dockerfile and artifact %s on %s ...", dockerImageInstance.artifactName, dockerImageInstance.host.name);
            notifyProgress(descriptionTask, startDate, null, 60, msg);
            if (AzureDockerUtils.DEBUG) System.out.println("Uploading Dockerfile and artifact: " + new Date().toString());
            AzureDockerVMOps.uploadDockerfileAndArtifact(dockerImageInstance, dockerImageInstance.host.session);
            if (AzureDockerUtils.DEBUG) System.out.println("Uploading Dockerfile and artifact: " + new Date().toString());

            msg = String.format("Creating Docker image %s on %s ...", dockerImageInstance.dockerImageName, dockerImageInstance.host.name);
            notifyProgress(descriptionTask, startDate, null, 80, msg);
            if (AzureDockerUtils.DEBUG) System.out.println("Creating a Docker image to the Docker host: " + new Date().toString());
            AzureDockerImageOps.create(dockerImageInstance, dockerImageInstance.host.session);
            if (AzureDockerUtils.DEBUG) System.out.println("Done creating a Docker image to the Docker host: " + new Date().toString());

            msg = String.format("Creating Docker container %s for image %s on %s ...", dockerImageInstance.dockerContainerName, dockerImageInstance.dockerImageName, dockerImageInstance.host.name);
            notifyProgress(descriptionTask, startDate, null, 90, msg);
            if (AzureDockerUtils.DEBUG) System.out.println("Creating a Docker container to the Docker host: " + new Date().toString());
            AzureDockerContainerOps.create(dockerImageInstance, dockerImageInstance.host.session);
            if (AzureDockerUtils.DEBUG) System.out.println("Done creating a Docker container to the Docker host: " + new Date().toString());

            msg = String.format("Starting Docker container %s for image %s on %s ...", dockerImageInstance.dockerContainerName, dockerImageInstance.dockerImageName, dockerImageInstance.host.name);
            notifyProgress(descriptionTask, startDate, null, 95, msg);
            if (AzureDockerUtils.DEBUG) System.out.println("Starting a Docker container to the Docker host: " + new Date().toString());
            AzureDockerContainerOps.start(dockerImageInstance, dockerImageInstance.host.session);
            if (AzureDockerUtils.DEBUG) System.out.println("Done starting a Docker container to the Docker host: " + new Date().toString());

            notifyProgress(descriptionTask, startDate, url, 100, message("runStatus"), dockerImageInstance.host.name);
        } catch (InterruptedException e) {
            EventUtil.logError(operation, ErrorType.userError, e, null, null);
            postEventProperties.put("PublishInterruptedError", e.getMessage());
            notifyProgress(descriptionTask, startDate, url, 100, message("runStatus"), dockerImageInstance.host.name);
        } catch (Exception ee) {
            EventUtil.logError(operation, ErrorType.systemError, ee, null, null);
            postEventProperties.put("PublishError", ee.getMessage());
            notifyProgress(descriptionTask, startDate, url, 100, "Error: %s", ee.getMessage());
        } finally {
            operation.complete();
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.DockerContainer, null, "Deploy", postEventProperties);
    }
}
