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
package com.microsoft.azuretools.docker.utils;

import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.AzureDockerSSHOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.components.AzureWizardDialog;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.ui.dialogs.AzureInputDockerLoginCredsDialog;
import com.microsoft.azuretools.docker.ui.wizards.publish.AzureSelectDockerWizard;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

public class AzureDockerUIResources {
    private static final Logger log = Logger.getLogger(AzureDockerUIResources.class.getName());
    public static boolean CANCELED = false;

    public static void updateAzureResourcesWithProgressDialog(Shell shell, IProject project) {
        ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
        try {
            dialog.run(true, true, new IRunnableWithProgress(){
                public void run(IProgressMonitor monitor) {
                    monitor.beginTask("Loading Azure Resources", 100);
                    try {
                        monitor.subTask("Creating an Azure instance...");
                        AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();
                        if (monitor.isCanceled()) {
                            monitor.done();
                            return;
                        }
                        // not signed in
                        if (azureAuthManager == null) {
                            monitor.done();
                            return;
                        }
                        AzureDockerHostsManager dockerManager = AzureDockerHostsManager
                                .getAzureDockerHostsManagerEmpty(azureAuthManager);
                        monitor.worked(10);

                        monitor.subTask("Retrieving the subscription details...");
                        dockerManager.refreshDockerSubscriptions();
                        if (monitor.isCanceled()) {
                            monitor.done();
                            return;
                        }

                        monitor.worked(10);
                        monitor.subTask("Retrieving the key vault...");
                        dockerManager.refreshDockerVaults();
                        monitor.worked(10);
                        if (monitor.isCanceled()) {
                            monitor.done();
                            return;
                        }

                        monitor.worked(10);
                        monitor.subTask("Retrieving the key vault details...");
                        dockerManager.refreshDockerVaultDetails();
                        if (monitor.isCanceled()) {
                            CANCELED = true;
                            monitor.done();
                            return;
                        }

                        monitor.worked(10);
                        monitor.subTask("Retrieving the network details...");
                        dockerManager.refreshDockerVnetDetails();
                        if (monitor.isCanceled()) {
                            monitor.done();
                            return;
                        }

                        monitor.worked(10);
                        monitor.subTask("Retrieving the storage account details...");
                        dockerManager.refreshDockerStorageAccountDetails();
                        if (monitor.isCanceled()) {
                            monitor.done();
                            return;
                        }

                        monitor.subTask("Retrieving the Docker virtual machines details...");
                        dockerManager.refreshDockerHostDetails();
                        CANCELED = false;
                    } catch (Exception e) {
                        CANCELED = true;
                        log.log(Level.SEVERE, "updateAzureResourcesWithProgressDialog: " + e.getMessage(), e);
                        e.printStackTrace();
                    }

                    monitor.done();
                }
            });
        } catch (Exception e) {
            CANCELED = true;
            log.log(Level.SEVERE, "updateAzureResourcesWithProgressDialog: " + e.getMessage(), e);
            e.printStackTrace();
        }
      }

    public static void createDockerKeyVault(DockerHost dockerHost, AzureDockerHostsManager dockerManager) {
        Job createDockerHostJob = new Job(String.format("Creating Azure Key Vault %s for %s", dockerHost.certVault.name, dockerHost.name)) {
            @Override
            protected IStatus run(IProgressMonitor progressMonitor) {
                progressMonitor.beginTask("start task", 100);
                try {
                    progressMonitor.subTask(String.format("Reading subscription details for Docker host %s ...", dockerHost.apiUrl));
                    progressMonitor.worked(5);
                    Azure azureClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).azureClient;
                    KeyVaultClient keyVaultClient = dockerManager.getSubscriptionsMap().get(dockerHost.sid).keyVaultClient;
                    if (progressMonitor.isCanceled()) {
                        progressMonitor.done();
                        return Status.CANCEL_STATUS;
                    }

                    String retryMsg = "Create";
                    int retries = 5;
                    AzureDockerCertVault certVault = null;
                    do {
                        progressMonitor.subTask(String.format("%s new key vault %s ...", retryMsg, dockerHost.certVault.name));
                        progressMonitor.worked(15 + 15 * retries);
                        if (AzureDockerUtils.DEBUG) System.out.println(retryMsg + " new Docker key vault: " + new Date().toString());
                        AzureDockerCertVaultOps.createOrUpdateVault(azureClient, dockerHost.certVault, keyVaultClient);
                        if (AzureDockerUtils.DEBUG) System.out.println("Done creating new key vault: " + new Date().toString());
                        if (progressMonitor.isCanceled()) {
                            progressMonitor.done();
                            return Status.CANCEL_STATUS;
                        }
                        certVault = AzureDockerCertVaultOps.getVault(azureClient, dockerHost.certVault.name,
                                dockerHost.certVault.resourceGroupName, keyVaultClient);
                        retries++;
                        retryMsg = "Retry creating";
                    } while (retries < 5 && (certVault == null || certVault.vmUsername == null)); // Retry couple times

                    progressMonitor.subTask("Updating key vaults ...");
                    progressMonitor.worked(95);
                    if (AzureDockerUtils.DEBUG) System.out.println("Refreshing key vaults: " + new Date().toString());
                    dockerManager.refreshDockerVaults();
                    dockerManager.refreshDockerVaultDetails();
                    if (AzureDockerUtils.DEBUG) System.out.println("Done refreshing key vaults: " + new Date().toString());

//                    progressMonitor.subTask("");
//                    progressMonitor.worked(1);
//                    if (progressMonitor.isCanceled()) {
//                        if (displayWarningOnCreateKeyVaultCancelAction() == 0) {
//                            progressMonitor.done();
//                            return Status.CANCEL_STATUS;
//                        }
//                    }
//
                    progressMonitor.done();
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    String msg = "An error occurred while attempting to create a new Azure Key Vault." + "\n" + e.getMessage();
                    log.log(Level.SEVERE, "createDockerKeyVault: " + msg, e);
                    e.printStackTrace();
                    PluginUtil.displayErrorDialog(Display.getDefault().getActiveShell(), "Error Creating Azure Key Vault " + dockerHost.certVault.name, "An error occurred while attempting to create a new Azure Key Vault." + "\n" + e.getMessage());
                    return Status.CANCEL_STATUS;
                }
            }
        };

        createDockerHostJob.schedule();
    }

    public static void createArtifact(Shell shell, IProject project) {
        if (project == null) {
            return;
        }

        String projectName = project.getName();
        String destinationPath = project.getLocation() + "/" + projectName + ".war";
        ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
        try {
            dialog.run(true, true, new IRunnableWithProgress(){
                public void run(IProgressMonitor progressMonitor) {
                    progressMonitor.beginTask("Creating WAR artifact", 100);
                    try {
                        progressMonitor.subTask(String.format("Building selected project: %s ...", project.getName()));
                        progressMonitor.worked(35);
                        project.build(IncrementalProjectBuilder.FULL_BUILD, null);

                        progressMonitor.subTask("Exporting to WAR ...");
                        progressMonitor.worked(75);
                        IDataModel dataModel = DataModelFactory.createDataModel(new WebComponentExportDataModelProvider());
                        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.PROJECT_NAME, projectName);
                        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.ARCHIVE_DESTINATION, destinationPath);

                        if (dataModel.getDefaultOperation() != null)
                            try {
                                dataModel.getDefaultOperation().execute(null, null);
                            } catch (Exception ignored) {}

//                        progressMonitor.subTask("");
//                        progressMonitor.worked(1);
//                        if (progressMonitor.isCanceled()) {
//                            if (displayWarningOnCreateKeyVaultCancelAction() == 0) {
//                                progressMonitor.done();
//                                return Status.CANCEL_STATUS;
//                            }
//                        }
                        progressMonitor.done();
                    } catch (Exception e) {
                        String msg = "An error occurred while attempting to create WAR artifact" + "\n" + e.getMessage();
                        log.log(Level.SEVERE, "createArtifact: " + msg, e);
                        e.printStackTrace();
                    }

                    progressMonitor.done();
                }
            });
        } catch (Exception e) {
            CANCELED = true;
            log.log(Level.SEVERE, "updateAzureResourcesWithProgressDialog: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public static Color getColor(int systemColorID) {
        Display display = Display.getCurrent();
        return display.getSystemColor(systemColorID);
    }

    public static IProject getCurrentSelectedProject() {
        IProject project = null;
        ISelectionService selectionService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
        ISelection selection = selectionService.getSelection();

        if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();

            if (element instanceof IResource) {
                project = ((IResource) element).getProject();
            } else if (element instanceof PackageFragmentRoot) {
                IJavaProject jProject = ((PackageFragmentRoot) element).getJavaProject();
                project = jProject.getProject();
            } else if (element instanceof IJavaElement) {
                IJavaProject jProject = ((IJavaElement) element).getJavaProject();
                project = jProject.getProject();
            }
        }

        if (project == null) {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            if (workspace.getRoot() != null && workspace.getRoot().getProjects().length > 0) {
                IProject[] projects = workspace.getRoot().getProjects();
                project = projects[projects.length - 1];
            } else {
                PluginUtil.displayErrorDialog(Display.getDefault().getActiveShell(), "No Active Project", "Must have a project first");
            }
        }

        return project;
    }

    public static int deleteAzureDockerHostConfirmationDialog(Shell shell, Azure azureClient, DockerHost dockerHost) {
        String promptMessageDeleteAll = String.format(
                "This operation will delete virtual machine %s and its resources:\n" + "\t - network interface: %s\n"
                        + "\t - public IP: %s\n" + "\t - virtual network: %s\n"
                        + "The associated disks and storage account will not be deleted\n",
                dockerHost.hostVM.name, dockerHost.hostVM.nicName, dockerHost.hostVM.publicIpName,
                dockerHost.hostVM.vnetName);

        String promptMessageDelete = String.format("This operation will delete virtual machine %s.\n"
                + "The associated disks and storage account will not be deleted\n\n"
                + "Are you sure you want to continue?\n", dockerHost.hostVM.name);

        String[] options;
        String promptMessage;

        if (AzureDockerVMOps.isDeletingDockerHostAllSafe(azureClient, dockerHost.hostVM.resourceGroupName,
                dockerHost.hostVM.name)) {
            promptMessage = promptMessageDeleteAll;
            options = new String[] { "Cancel", "Delete VM Only", "Delete All" };
        } else {
            promptMessage = promptMessageDelete;
            options = new String[] { "Cancel", "Delete" };
        }

        MessageDialog deleteDialog = new MessageDialog(shell, "Delete Docker Host", null, promptMessage, MessageDialog.ERROR,options, 0);
        int dialogReturn = deleteDialog.open();

        switch (dialogReturn) {
        case 0:
            if (AzureDockerUtils.DEBUG) System.out.format("Delete Docker Host op was canceled %s\n", dialogReturn);
            break;
        case 1:
            if (AzureDockerUtils.DEBUG) System.out.println("Delete VM only: " + dockerHost.name);
            break;
        case 2:
            if (AzureDockerUtils.DEBUG) System.out.println("Delete VM and resources: " + dockerHost.name);
            break;
        default:
            if (AzureDockerUtils.DEBUG) System.out.format("Delete Docker Host op was canceled %s\n", dialogReturn);
        }

        return dialogReturn;
    }

    public static void deleteDockerHost(Shell shell, IProject project, Azure azureClient, DockerHost dockerHost, int option, Runnable runnable) {
        String progressMsg = (option == 2)
                ? String.format("Deleting Virtual Machine %s and Its Resources...", dockerHost.name)
                : String.format("Deleting Docker Host %s...", dockerHost.name);

        DefaultLoader.getIdeHelper().runInBackground(project, "Deleting Docker Host", false, true, progressMsg, new Runnable() {
                @Override
                public void run() {
                    try {
                        if (option == 2) {
                                AzureDockerVMOps.deleteDockerHostAll(azureClient, dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
                        } else {
                                AzureDockerVMOps.deleteDockerHost(azureClient, dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
                        }
                        DefaultLoader.getIdeHelper().runInBackground(project, "Updating Docker Hosts Details ",
                                false, true, "Updating Docker hosts details...", new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            AzureDockerHostsManager dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);
                                            dockerManager.refreshDockerHostDetails();

                                            if (runnable != null) {
                                                runnable.run();
                                            }
                                        } catch (Exception ee) {
                                            if (AzureDockerUtils.DEBUG)
                                                ee.printStackTrace();
                                            log.log(Level.SEVERE, "onRemoveDockerHostAction", ee);
                                        }
                                    }
                                });
                    } catch (Exception e) {
                        DefaultLoader.getUIHelper().showException(
                                String.format("Unexpected error detected while deleting Docker host %s:\n\n%s", dockerHost.name, e.getMessage()),
                                e, "Error Deleting Docker Host", false, true);
                        if (AzureDockerUtils.DEBUG) e.printStackTrace();
                        log.log(Level.SEVERE, "onRemoveDockerHostAction", e);
                    }
                }
        });
    }

    public static void publish2DockerHostContainer(Shell shell, IProject project, DockerHost host) {
        try {
            AzureDockerUIResources.createArtifact(shell, project);

            AzureManager azureAuthManager = AuthMethodManager.getInstance().getAzureManager();

            // not signed in
            if (azureAuthManager == null) {
                return;
            }

            AzureDockerHostsManager dockerManager = AzureDockerHostsManager
                    .getAzureDockerHostsManager(azureAuthManager);

            if (!dockerManager.isInitialized()) {
                AzureDockerUIResources.updateAzureResourcesWithProgressDialog(shell, project);
                if (AzureDockerUIResources.CANCELED) {
                    return;
                }
                dockerManager = AzureDockerHostsManager.getAzureDockerHostsManagerEmpty(null);
            }

            if (dockerManager.getSubscriptionsMap().isEmpty()) {
                PluginUtil.displayErrorDialog(shell, "Create Docker Host", "Must select an Azure subscription first");
                return;
            }

            DockerHost dockerHost = (host != null) ? host : (dockerManager.getDockerPreferredSettings() != null) ? dockerManager.getDockerHostForURL(dockerManager.getDockerPreferredSettings().dockerApiName) : null;
            AzureDockerImageInstance dockerImageDescription = dockerManager.getDefaultDockerImageDescription(project.getName(), dockerHost);
            AzureSelectDockerWizard selectDockerWizard = new AzureSelectDockerWizard(project, dockerManager, dockerImageDescription);
            WizardDialog selectDockerHostDialog = new AzureWizardDialog(shell, selectDockerWizard);

            if (dockerHost != null) {
                selectDockerWizard.selectDefaultDockerHost(dockerHost, true);
            }

            if (selectDockerHostDialog.open() == Window.OK) {
                try {
                    String url = selectDockerWizard.deploy();
                    if (AzureDockerUtils.DEBUG) System.out.println("Web app published at: " + url);
                  } catch (Exception ex) {
                    PluginUtil.displayErrorDialogAndLog(shell, "Unexpected error detected while publishing to a Docker container", ex.getMessage(), ex);
                    log.log(Level.SEVERE, "publish2DockerHostContainer: " + ex.getMessage(), ex);
                  }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "publish2DockerHostContainer: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public static void printDebugMessage(Object source, String msg) {
        if (AzureDockerUtils.DEBUG) {
            System.out.println(msg);
            try {
                MessageConsole console = com.microsoft.azuretools.core.Activator.findConsole(com.microsoft.azuretools.core.Activator.CONSOLE_NAME);
                console.activate();
                final MessageConsoleStream azureConsoleOut = console.newMessageStream();
                azureConsoleOut.println(String.format("%s: %s", source.getClass().toString(), msg));
            } catch (Exception ignored) {}
        }
    }

    public static void updateDockerHost(Shell shell, IProject project, EditableDockerHost editableDockerHost, AzureDockerHostsManager dockerManager, boolean doReset) {
        DockerHost updateHost = editableDockerHost.originalDockerHost;
        AzureInputDockerLoginCredsDialog loginCredsDialog = new AzureInputDockerLoginCredsDialog(PluginUtil.getParentShell(), project, editableDockerHost, dockerManager, doReset);

        if (loginCredsDialog.open() == Window.OK) {
            // Update Docker host log in credentials
            updateHost.isUpdating = true;
            DefaultLoader.getIdeHelper().runInBackground(project, String.format("Updating %s Log In Credentials", updateHost.name), false, true, String.format("Updating log in credentials for %s...", updateHost.name), new Runnable() {
                @Override
                public void run() {
                    try {
                        AzureDockerVMOps.updateDockerHostVM(dockerManager.getSubscriptionsMap().get(updateHost.sid).azureClient, editableDockerHost.updatedDockerHost);
                        updateHost.certVault = editableDockerHost.updatedDockerHost.certVault;
                        updateHost.hasPwdLogIn = editableDockerHost.updatedDockerHost.hasPwdLogIn;
                        updateHost.hasSSHLogIn = editableDockerHost.updatedDockerHost.hasSSHLogIn;
                        updateHost.hasKeyVault = false;
                        updateHost.certVault.uri = "";
                        updateHost.certVault.name = "";
                        Session session = AzureDockerSSHOps.createLoginInstance(updateHost);
                        AzureDockerVMOps.UpdateCurrentDockerUser(session);
                        updateHost.session = session;
                    } catch (Exception ee) {
                        if (AzureDockerUtils.DEBUG)
                            ee.printStackTrace();
                        log.log(Level.SEVERE, "dockerHostsEditButton.addSelectionListener", ee);
                    }

                    updateHost.isUpdating = false;
                }
            });
        }
    }

}
