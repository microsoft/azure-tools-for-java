/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.docker.ui.wizards.publish;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DEPLOY_WEBAPP_DOCKERHOST;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.AzureDockerPreferredSettings;
import com.microsoft.azure.docker.model.AzureDockerSubscription;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerContainerOps;
import com.microsoft.azure.docker.ops.AzureDockerImageOps;
import com.microsoft.azure.docker.ops.AzureDockerSSHOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventListener;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.core.ui.views.AzureDeploymentProgressNotification;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.ui.dialogs.AzureInputDockerLoginCredsDialog;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.io.File;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class AzureSelectDockerWizard extends Wizard implements TelemetryProperties{
	private static final Logger log =  Logger.getLogger(AzureSelectDockerWizard.class.getName());
	
	private AzureSelectDockerHostPage azureSelectDockerHostPage;
	private AzureConfigureDockerContainerStep azureConfigureDockerContainerStep;

	private IProject project;
	private AzureDockerHostsManager dockerManager;
	private AzureDockerImageInstance dockerImageDescription;
	private AzureDockerSubscription subscription;

	public AzureSelectDockerWizard(final IProject project, AzureDockerHostsManager dockerManager, AzureDockerImageInstance dockerImageDescription) {
	    this.project = project;
	    this.dockerManager = dockerManager;
	    this.dockerImageDescription = dockerImageDescription;

	    azureSelectDockerHostPage = new AzureSelectDockerHostPage(this);
	    azureConfigureDockerContainerStep = new AzureConfigureDockerContainerStep(this);

		setWindowTitle("Deploying Docker Container on Azure");
	}

	@Override
	public void addPages() {
		addPage(azureSelectDockerHostPage);
		addPage(azureConfigureDockerContainerStep);
	}

	@Override
	public boolean performFinish() {
		return doValidate();
	}
	
	public boolean doValidate() {
		return azureSelectDockerHostPage.doValidate() && 
				azureConfigureDockerContainerStep.doValidate() &&
				(dockerImageDescription.hasNewDockerHost || 
	            dockerImageDescription.host.state == DockerHost.DockerHostVMState.RUNNING ||
	            DefaultLoader.getUIHelper().showConfirmation(String.format("The selected Docker host %s state is %s and publishing could fail.\n\n Do you want to continue?", dockerImageDescription.host.name, dockerImageDescription.host.state),
	                "Docker Host Not in Running State", new String[]{"Yes", "No"}, null));
	}

	public AzureDockerImageInstance getDockerImageInstance() {
		return dockerImageDescription;
	}

	public IProject getProject() {
		return project;
	}

	public AzureDockerHostsManager getDockerManager() {
		return dockerManager;
	}

	public void setPredefinedDockerfileOptions(String artifactFileName) {
		if (azureConfigureDockerContainerStep != null) {
			azureConfigureDockerContainerStep.setPredefinedDockerfileOptions(artifactFileName);
		}
	}

	public void setDockerContainerName(String dockerContainerName) {
		if (azureConfigureDockerContainerStep != null) {
			azureConfigureDockerContainerStep.setDockerContainerName(dockerContainerName);
		}
	}

	public void selectDefaultDockerHost(DockerHost dockerHost, boolean selectOtherHosts) {
		if (azureSelectDockerHostPage != null) {
			azureSelectDockerHostPage.selectDefaultDockerHost(dockerHost, selectOtherHosts);
		}
	}

	public String deploy() {
		AzureDockerPreferredSettings dockerPreferredSettings = dockerManager.getDockerPreferredSettings();

		if (dockerPreferredSettings == null) {
			dockerPreferredSettings = new AzureDockerPreferredSettings();
		}

		dockerPreferredSettings.dockerApiName = dockerImageDescription.host.apiUrl;
		dockerPreferredSettings.dockerfileOption = dockerImageDescription.predefinedDockerfile;
		dockerPreferredSettings.region = dockerImageDescription.host.hostVM.region;
		dockerPreferredSettings.vmSize = dockerImageDescription.host.hostVM.vmSize;
		dockerPreferredSettings.vmOS = dockerImageDescription.host.hostOSType.name();
		dockerManager.setDockerPreferredSettings(dockerPreferredSettings);

		DefaultLoader.getIdeHelper().runInBackground(project, "Deploying Docker Container on Azure", false, true, "Deploying Web app to a Docker host on Azure...", new Runnable() {
			@Override
			public void run() {
				try {
					DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
						@Override
						public void run() {
							if (!dockerImageDescription.hasNewDockerHost) {
								Session session = null;

								do {
									try {
										// check if the Docker host is accessible
										session = AzureDockerSSHOps.createLoginInstance(dockerImageDescription.host);
									} catch (Exception e) {
										session = null;
									}

									if (session == null) {
										EditableDockerHost editableDockerHost = new EditableDockerHost(dockerImageDescription.host);
										AzureInputDockerLoginCredsDialog loginCredsDialog = new AzureInputDockerLoginCredsDialog(PluginUtil.getParentShell(), project, editableDockerHost, dockerManager, false);

										if (loginCredsDialog.open() == Window.OK) {
											// Update Docker host log in credentials
											dockerImageDescription.host.certVault = editableDockerHost.updatedDockerHost.certVault;
											dockerImageDescription.host.hasSSHLogIn = editableDockerHost.updatedDockerHost.hasSSHLogIn;
											dockerImageDescription.host.hasPwdLogIn = editableDockerHost.updatedDockerHost.hasPwdLogIn;
//											AzureDockerVMOps.updateDockerHostVM(dockerManager.getSubscriptionsMap().get(dockerImageDescription.sid).azureClient, editableDockerHost.updatedDockerHost);
										} else {
											return;
										}
									}
								} while (session == null);
							}

//							Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerImageDescription.sid).azureClient;
//							DockerContainerDeployTask task = new DockerContainerDeployTask(project, azureClient, dockerImageDescription);
//							task.queue();
							createDockerContainerDeployTask(project, dockerImageDescription, dockerManager);
						}
					});
				} catch (Exception e) {
					String msg = "An error occurred while attempting to deploy to the selected Docker host." + "\n" + e.getMessage();
					PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), "Failed to Deploy Web App as Docker Container", msg, e);
				}
			}
		});

		return AzureDockerUtils.getUrl(dockerImageDescription);
	}
	
	public void createDockerContainerDeployTask(IProject project, AzureDockerImageInstance dockerImageInstance, AzureDockerHostsManager dockerManager) {
		String url = AzureDockerUtils.getUrl(dockerImageInstance);

		String deploymentName = url;
		String jobDescription = String.format("Publishing %s as Docker Container", new File(dockerImageInstance.artifactPath).getName());
		AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(deploymentName, jobDescription);
		
		Map<String, String> postEventProperties = new HashMap<String, String>();
		postEventProperties.put("DockerFileOption", dockerImageInstance.predefinedDockerfile);
		
		Job createDockerHostJob = new Job(jobDescription) {
			@Override
			protected IStatus run(IProgressMonitor progressMonitor) {
				Operation operation = TelemetryManager.createOperation(WEBAPP, DEPLOY_WEBAPP_DOCKERHOST);
				operation.start();
		        try {
		        	// Setup Azure Console and Azure Activity Log Window notifications
					MessageConsole console = com.microsoft.azuretools.core.Activator.findConsole(com.microsoft.azuretools.core.Activator.CONSOLE_NAME);
					console.activate();
					final MessageConsoleStream azureConsoleOut = console.newMessageStream();
		            progressMonitor.beginTask("start task", 100);
//		            com.microsoft.azuretools.core.Activator.removeUnNecessaryListener();
		            DeploymentEventListener undeployListnr = new DeploymentEventListener() {
		                @Override
		                public void onDeploymentStep(DeploymentEventArgs args) {
		                    progressMonitor.subTask(args.getDeployMessage());
		                    progressMonitor.worked(args.getDeployCompleteness());
		                    azureConsoleOut.println(String.format("%s: %s", deploymentName, args.getDeployMessage()));
		                }
		            };
		            com.microsoft.azuretools.core.Activator.getDefault().addDeploymentEventListener(undeployListnr);
		            com.microsoft.azuretools.core.Activator.depEveList.add(undeployListnr);

		            // Start the real job here
		            String msg = String.format("Publishing %s to Docker host %s ...", new File(dockerImageInstance.artifactPath).getName(), dockerImageInstance.host.name);
					AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 0, msg);

					msg = "Connecting to Azure...";
					AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 5, msg);

					AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
		            // not signed in
		            if (azureAuthManager == null) {
		                throw new RuntimeException("User not signed in");
		            }
		            AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(azureAuthManager);
		            Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerImageInstance.host.sid).azureClient;
					if (progressMonitor.isCanceled()) {
						displayWarningOnCreateDockerContainerDeployTask(this, progressMonitor, deploymentName);
						return Status.CANCEL_STATUS;
					}
					
		            if (dockerImageInstance.hasNewDockerHost) {
		                msg = String.format("Creating new virtual machine %s ...", dockerImageInstance.host.name);
						AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 20, msg);
		                AzureDockerUIResources.printDebugMessage(this, "Creating new virtual machine: " + new Date().toString());
		                AzureDockerVMOps.createDockerHostVM(azureClient, dockerImageInstance.host);
		                AzureDockerUIResources.printDebugMessage(this, "Done creating new virtual machine: " + new Date().toString());
						if (progressMonitor.isCanceled()) {
							displayWarningOnCreateDockerContainerDeployTask(this, progressMonitor, deploymentName);
							return Status.CANCEL_STATUS;
						}
						
		                msg = String.format("Updating Docker hosts ...");
						AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 5, msg);
			            AzureDockerUIResources.printDebugMessage(this, "Getting the new docker host details: " + new Date().toString());
//			            dockerManager.refreshDockerHostDetails();
			            VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(dockerImageInstance.host.hostVM.resourceGroupName, dockerImageInstance.host.hostVM.name);
			            if (vm != null) {
			                DockerHost updatedHost = AzureDockerVMOps.getDockerHost(vm, dockerManager.getDockerVaultsMap());
			                if (updatedHost != null) {
								dockerImageInstance.host.hostVM = updatedHost.hostVM;
								dockerImageInstance.host.apiUrl = updatedHost.apiUrl;
			                }
			            }
			            AzureDockerUIResources.printDebugMessage(this, "Done getting new Docker host details: " + new Date().toString());

		                msg = String.format("Waiting for virtual machine to be up %s ...", dockerImageInstance.host.name);
						AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 10, msg);
		                AzureDockerUIResources.printDebugMessage(this, "Waiting for virtual machine to be up: " + new Date().toString());
		                AzureDockerVMOps.waitForVirtualMachineStartup(azureClient, dockerImageInstance.host);
		                AzureDockerUIResources.printDebugMessage(this, "Done Waiting for virtual machine to be up: " + new Date().toString());
						if (progressMonitor.isCanceled()) {
							displayWarningOnCreateDockerContainerDeployTask(this, progressMonitor, deploymentName);
							return Status.CANCEL_STATUS;
						}
						

		                msg = String.format("Configuring Docker service for %s ...", dockerImageInstance.host.name);
						AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 10, msg);
		                AzureDockerUIResources.printDebugMessage(this, "Configuring Docker host: " + new Date().toString());
		                AzureDockerVMOps.installDocker(dockerImageInstance.host);
		                AzureDockerUIResources.printDebugMessage(this, "Done configuring Docker host: " + new Date().toString());

		                msg = String.format("Updating Docker hosts ...");
						AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 5, msg);
			            AzureDockerUIResources.printDebugMessage(this, "Refreshing docker hosts: " + new Date().toString());
//			            dockerManager.refreshDockerHostDetails();
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
			            AzureDockerUIResources.printDebugMessage(this, "Done refreshing Docker hosts: " + new Date().toString());
		                AzureDockerUIResources.printDebugMessage(this, "Finished setting up Docker host");
		            } else {
		                msg = String.format("Using virtual machine %s ...", dockerImageInstance.host.name);
						AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 55, msg);
		            }
					if (progressMonitor.isCanceled()) {
						displayWarningOnCreateDockerContainerDeployTask(this, progressMonitor, deploymentName);
						return Status.CANCEL_STATUS;
					}
					

		            if (dockerImageInstance.host.session == null) {
		            	AzureDockerUIResources.printDebugMessage(this, "Opening a remote connection to the Docker host: " + new Date().toString());
		                dockerImageInstance.host.session = AzureDockerSSHOps.createLoginInstance(dockerImageInstance.host);
		                AzureDockerUIResources.printDebugMessage(this, "Done opening a remote connection to the Docker host: " + new Date().toString());
		            }

		            if (dockerImageInstance.hasNewDockerHost) {
		                if (dockerImageInstance.host.certVault != null && dockerImageInstance.host.certVault.hostName != null) {
		                    AzureDockerUIResources.createDockerKeyVault(dockerImageInstance.host, dockerManager);
		                }
		            }

		            msg = String.format("Uploading Dockerfile and artifact %s on %s ...", dockerImageInstance.artifactName, dockerImageInstance.host.name);
					AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 15, msg);
		            AzureDockerUIResources.printDebugMessage(this, "Uploading Dockerfile and artifact: " + new Date().toString());
		            AzureDockerVMOps.uploadDockerfileAndArtifact(dockerImageInstance, dockerImageInstance.host.session);
		            AzureDockerUIResources.printDebugMessage(this, "Uploading Dockerfile and artifact: " + new Date().toString());
					if (progressMonitor.isCanceled()) {
						displayWarningOnCreateDockerContainerDeployTask(this, progressMonitor, deploymentName);
						return Status.CANCEL_STATUS;
					}
					

		            msg = String.format("Creating Docker image %s on %s ...", dockerImageInstance.dockerImageName, dockerImageInstance.host.name);
					AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 15, msg);
		            AzureDockerUIResources.printDebugMessage(this, "Creating a Docker image to the Docker host: " + new Date().toString());
		            AzureDockerImageOps.create(dockerImageInstance, dockerImageInstance.host.session);
		            AzureDockerUIResources.printDebugMessage(this, "Done creating a Docker image to the Docker host: " + new Date().toString());
					if (progressMonitor.isCanceled()) {
						displayWarningOnCreateDockerContainerDeployTask(this, progressMonitor, deploymentName);
						return Status.CANCEL_STATUS;
					}
					

		            msg = String.format("Creating Docker container %s for image %s on %s ...", dockerImageInstance.dockerContainerName, dockerImageInstance.dockerImageName, dockerImageInstance.host.name);
					AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 5, msg);
		            AzureDockerUIResources.printDebugMessage(this, "Creating a Docker container to the Docker host: " + new Date().toString());
		            AzureDockerContainerOps.create(dockerImageInstance, dockerImageInstance.host.session);
		            AzureDockerUIResources.printDebugMessage(this, "Done creating a Docker container to the Docker host: " + new Date().toString());
					if (progressMonitor.isCanceled()) {
						displayWarningOnCreateDockerContainerDeployTask(this, progressMonitor, deploymentName);
						return Status.CANCEL_STATUS;
					}
					

		            msg = String.format("Starting Docker container %s for image %s on %s ...", dockerImageInstance.dockerContainerName, dockerImageInstance.dockerImageName, dockerImageInstance.host.name);
					AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 5, msg);
		            AzureDockerUIResources.printDebugMessage(this, "Starting a Docker container to the Docker host: " + new Date().toString());
		            AzureDockerContainerOps.start(dockerImageInstance, dockerImageInstance.host.session);
		            AzureDockerUIResources.printDebugMessage(this, "Done starting a Docker container to the Docker host: " + new Date().toString());
					if (progressMonitor.isCanceled()) {
						displayWarningOnCreateDockerContainerDeployTask(this, progressMonitor, deploymentName);
						return Status.CANCEL_STATUS;
					}
					
					AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, url, 100, "");
					
					try {
			            com.microsoft.azuretools.core.Activator.depEveList.remove(undeployListnr);
			            com.microsoft.azuretools.core.Activator.removeDeploymentEventListener(undeployListnr);
					} catch (Exception ignored) { }
					progressMonitor.done();
					AppInsightsClient.create("Deploy as DockerContainer", "", postEventProperties);
					return Status.OK_STATUS;
				} catch (Exception e) {
					String msg = "An error occurred while attempting to publish a Docker container!" + "\n" + e.getMessage();
					log.log(Level.SEVERE, "createDockerContainerDeployTask: " + msg, e);
					e.printStackTrace();
					AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, null, -1, "Error: " + e.getMessage());
					EventUtil.logError(operation, ErrorType.systemError, e, null, null);
					return Status.CANCEL_STATUS;
				} finally {
		        	operation.complete();
				}
			}
		};

		createDockerHostJob.schedule();	
	}
	
	private static void displayWarningOnCreateDockerContainerDeployTask(Object parent, IProgressMonitor progressMonitor, String deploymentName) {
		DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
			@Override
			public void run() {
				Shell shell = PluginUtil.getParentShell();
				
				if (shell != null) {
					MessageDialog.openWarning(shell, "Stop Publishing to Docker Container", "Canceling the task at this time can leave the Docker virtual machine host in a broken state and it can resut in publishing to fail the next time!");
					AzureDeploymentProgressNotification.notifyProgress(parent, deploymentName, "Error", -1, "Stopped by the user");
					progressMonitor.done();
				}
			}
		});
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
