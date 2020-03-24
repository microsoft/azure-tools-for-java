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

package com.microsoft.azuretools.docker.ui.wizards.publish;

import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.AzureDockerPreferredSettings;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerSSHOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureWizardDialog;
import com.microsoft.azuretools.core.components.AzureWizardPage;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.docker.ui.dialogs.AzureInputDockerLoginCredsDialog;
import com.microsoft.azuretools.docker.ui.dialogs.AzureViewDockerDialog;
import com.microsoft.azuretools.docker.ui.wizards.createhost.AzureNewDockerWizard;
import com.microsoft.azuretools.docker.utils.AzureDockerUIResources;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class AzureSelectDockerHostPage extends AzureWizardPage {
    private static final Logger log =  Logger.getLogger(AzureSelectDockerHostPage.class.getName());

    private static final String REFRESH = "Refresh";
    private static final String VIEW = "View";
    private static final String ADD = "Add";
    private static final String DELETE = "Delete";
    private static final String EDIT = "Edit";

    private Composite mainContainer;
    private Text dockerArtifactPathTextField;
    private Text dockerImageNameTextField;
    private Button dockerArtifactPathBrowseButton;
    private TableViewer dockerHostsTableViewer;
    private Table dockerHostsTable;
    private Button dockerHostsRefreshButton;
    private Button dockerHostsViewButton;
    private Button dockerHostsAddButton;
    private Button dockerHostsDeleteButton;
    private Button dockerHostsEditButton;

    private ManagedForm managedForm;
    private ScrolledForm errMsgForm;
    private IMessageManager errDispatcher;

    private IProject project;
    private AzureDockerHostsManager dockerManager;
    private AzureDockerImageInstance dockerImageDescription;
    private AzureSelectDockerWizard wizard;

    private DockerHostsTableSelection dockerHostsTableSelection;
    private List<DockerHost> dockerHostsList;

    /**
     * Create the wizard.
     */
    public AzureSelectDockerHostPage(AzureSelectDockerWizard wizard) {
        super("Deploying Docker Container on Azure", "", Activator.getImageDescriptor("icons/large/DeploytoAzureWizard.png"));

        this.wizard = wizard;
        this.dockerManager = wizard.getDockerManager();
        this.dockerImageDescription = wizard.getDockerImageInstance();
        this.project = wizard.getProject();

        setTitle("Type an image name, select the artifact's path and check a Docker host to be used");
        setDescription("");

        dockerHostsList = new ArrayList<>();
        for (DockerHost host : dockerManager.getDockerHostsList()) {
            dockerHostsList.add(host);
        }

    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        mainContainer = new Composite(parent, SWT.NULL);

        setControl(mainContainer);
        mainContainer.setLayout(new GridLayout(4, false));

        Label lblNewLabel = new Label(mainContainer, SWT.NONE);
        GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblNewLabel.verticalIndent = 1;
        gd_lblNewLabel.horizontalIndent = 1;
        lblNewLabel.setLayoutData(gd_lblNewLabel);
        lblNewLabel.setText("Docker image name:");

        dockerImageNameTextField = new Text(mainContainer, SWT.BORDER);
        GridData gd_dockerImageNameTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_dockerImageNameTextField.verticalIndent = 1;
        dockerImageNameTextField.setLayoutData(gd_dockerImageNameTextField);
        new Label(mainContainer, SWT.NONE);

        Label lblNewLabel_1 = new Label(mainContainer, SWT.NONE);
        GridData gd_lblNewLabel_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblNewLabel_1.horizontalIndent = 1;
        lblNewLabel_1.setLayoutData(gd_lblNewLabel_1);
        lblNewLabel_1.setText("Artifact to deploy (.WAR or .JAR):");

        dockerArtifactPathTextField = new Text(mainContainer, SWT.BORDER);
        dockerArtifactPathTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        dockerArtifactPathBrowseButton = new Button(mainContainer, SWT.NONE);
        dockerArtifactPathBrowseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        dockerArtifactPathBrowseButton.setText("Browse...");

        Label lblHosts = new Label(mainContainer, SWT.NONE);
        lblHosts.setText("Hosts");

        Label label = new Label(mainContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

        dockerHostsTableViewer = new TableViewer(mainContainer, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);

        dockerHostsTable = dockerHostsTableViewer.getTable();
        dockerHostsTable.setHeaderVisible(true);
        dockerHostsTable.setLinesVisible(true);
        dockerHostsTable.setToolTipText("Check a Docker host from the list or create a new host");
        GridData gd_dockerHostsTableView = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 7);
        gd_dockerHostsTableView.horizontalIndent = 1;
        gd_dockerHostsTableView.heightHint = 155;
        dockerHostsTable.setLayoutData(gd_dockerHostsTableView);

        dockerHostsRefreshButton = new Button(mainContainer, SWT.NONE);
        GridData gd_dockerHostsRefreshButton = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostsRefreshButton.verticalIndent = 1;
        dockerHostsRefreshButton.setLayoutData(gd_dockerHostsRefreshButton);
        dockerHostsRefreshButton.setText(REFRESH);

        dockerHostsViewButton = new Button(mainContainer, SWT.NONE);
        dockerHostsViewButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        dockerHostsViewButton.setText(VIEW);

        dockerHostsAddButton = new Button(mainContainer, SWT.NONE);
        dockerHostsAddButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        dockerHostsAddButton.setText(ADD);

        dockerHostsDeleteButton = new Button(mainContainer, SWT.NONE);
        dockerHostsDeleteButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        dockerHostsDeleteButton.setText("Delete");

        dockerHostsEditButton = new Button(mainContainer, SWT.NONE);
        dockerHostsEditButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        dockerHostsEditButton.setText("Edit");
        new Label(mainContainer, SWT.NONE);
        new Label(mainContainer, SWT.NONE);

        FormToolkit toolkit = new FormToolkit(mainContainer.getDisplay());
        toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
                HyperlinkSettings.UNDERLINE_HOVER);
        managedForm = new ManagedForm(mainContainer);
        errMsgForm = managedForm.getForm();
        errMsgForm.setVisible(false);
//        errMsgForm.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
//        errMsgForm.setBackground(mainContainer.getBackground());
        errDispatcher = managedForm.getMessageManager();
//        errMsgForm.setMessage("This is an error message", IMessageProvider.ERROR);

        initUIMainContainer(mainContainer);
    }

    private void initUIMainContainer(Composite mainContainer) {

        dockerImageNameTextField.setText(dockerImageDescription.dockerImageName);
        dockerImageNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerImageNameTip());
        dockerImageNameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerImageName(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerImageNameTextField", dockerImageNameTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerImageNameTextField", AzureDockerValidationUtils.getDockerImageNameTip(), null, IMessageProvider.ERROR, dockerImageNameTextField);
                    setErrorMessage("Invalid Docker image name");
                    setPageComplete(false);
                }
            }
        });

        String artifactPath;
        if (project != null) {
            try {
                String projectName = project.getName();
                artifactPath = project.getLocation() + "/" + projectName + ".war";
            } catch (Exception ignored) {
                artifactPath = "";
            }
        } else {
            artifactPath = "";
        }

        if (artifactPath == null || artifactPath.isEmpty() || !Files.isRegularFile(Paths.get(artifactPath))) {
            errDispatcher.addMessage("dockerArtifactPathTextField", AzureDockerValidationUtils.getDockerArtifactPathTip(), null, IMessageProvider.ERROR, dockerArtifactPathTextField);
            setErrorMessage("Invalid artifact path");
        } else {
            dockerArtifactPathTextField.setText(artifactPath);
        }
        dockerArtifactPathTextField.setToolTipText(AzureDockerValidationUtils.getDockerArtifactPathTip());
        dockerArtifactPathTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerArtifactPath(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerArtifactPathTextField", dockerArtifactPathTextField);
                    String artifactFileName = new File(((Text) event.getSource()).getText()).getName();
                    wizard.setPredefinedDockerfileOptions(artifactFileName);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerArtifactPathTextField", AzureDockerValidationUtils.getDockerArtifactPathTip(), null, IMessageProvider.ERROR, dockerArtifactPathTextField);
                    setErrorMessage("Invalid artifact path");
                    setPageComplete(false);
                }
            }
        });

        dockerArtifactPathBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(dockerArtifactPathBrowseButton.getShell(), SWT.OPEN);
                fileDialog.setText("Select Artifact .WAR or .JAR");
                fileDialog.setFilterPath(System.getProperty("user.home"));
                fileDialog.setFilterExtensions(new String[] { "*.war;*.jar", "*.jar", "*.*" });
                String path = fileDialog.open();
                if (path == null || (!path.toLowerCase().contains(".war") && !path.toLowerCase().contains(".jar"))) {
                    return;
                }
                dockerArtifactPathTextField.setText(path);
                String artifactFileName = new File(path).getName();
                wizard.setPredefinedDockerfileOptions(artifactFileName);
                setPageComplete(doValidate());
            }
        });

        TableViewerColumn colHostName = createTableViewerColumn(dockerHostsTableViewer, "Name", 150, 1);
        colHostName.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((DockerHost) element).name;
            }
        });
        TableViewerColumn colHostState = createTableViewerColumn(dockerHostsTableViewer, "State", 80, 2);
        colHostState.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                DockerHost dockerHost = (DockerHost) element;
                return dockerHost.hostVM.state != null ? dockerHost.hostVM.state.toString() : "TO_BE_CREATED";
            }
        });
        TableViewerColumn colHostOS = createTableViewerColumn(dockerHostsTableViewer, "OS", 200, 3);
        colHostOS.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((DockerHost) element).hostOSType.toString();
            }
        });
        TableViewerColumn colHostApiUrl = createTableViewerColumn(dockerHostsTableViewer, "API URL", 250, 4);
        colHostApiUrl.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((DockerHost) element).apiUrl.toString();
            }
        });
        dockerHostsTableViewer.setContentProvider(new ArrayContentProvider());
        dockerHostsList = new ArrayList<>();
        dockerHostsTableViewer.setInput(dockerHostsList);
        refreshDockerHostsTable(mainContainer);
        if (!dockerHostsList.isEmpty()) {
            if (dockerHostsTableSelection == null) {
                dockerHostsTable.select(0);
                dockerHostsTable.getItem(0).setChecked(true);
                dockerHostsTableSelection = new DockerHostsTableSelection();
                dockerHostsTableSelection.row = 0;
                dockerHostsTableSelection.host = (DockerHost) dockerHostsTable.getItem(0).getData();
            } else {
                dockerHostsTable.select(dockerHostsTableSelection.row);
                dockerHostsTable.getItem(dockerHostsTableSelection.row).setChecked(true);
            }
        } else {
            dockerHostsTableSelection = null;
            setPageComplete(false);
        }

        dockerHostsTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.detail == SWT.CHECK) {
                    DockerHost dockerHost = (DockerHost) ((TableItem) e.item).getData();
                    if (dockerHostsTableSelection == null || dockerHostsTableSelection.host != dockerHost) {
                        dockerHostsTableSelection = new DockerHostsTableSelection();
                        dockerHostsTableSelection.row = dockerHostsTable.indexOf((TableItem) e.item);
                        dockerHostsTableSelection.host = dockerHost;
                        for (TableItem tableItem : dockerHostsTable.getItems()) {
                            if (tableItem != ((TableItem) e.item) && tableItem.getChecked()) {
                                tableItem.setChecked(false);
                            }
                        }
                        dockerHostsTable.redraw();
                    } else {
                        dockerHostsTableSelection = null;
                    }
                    setPageComplete(doValidate());
                }
            }
        });

        dockerHostsRefreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                AzureDockerUIResources.updateAzureResourcesWithProgressDialog(mainContainer.getShell(), project);
                refreshDockerHostsTable(mainContainer);
                setPageComplete(doValidate());
                sendButtonClickedTelemetry(REFRESH);
            }
        });

        dockerHostsViewButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = dockerHostsTable.getSelectionIndex();
                if (idx >= 0 && dockerHostsTable.getItem(idx) != null) {
                    DockerHost dockerHost = (DockerHost) dockerHostsTable.getItem(dockerHostsTable.getSelectionIndex()).getData();
                    if (dockerHost != null) {
                        AzureViewDockerDialog viewDockerDialog = new AzureViewDockerDialog(mainContainer.getShell(), project, dockerHost, dockerManager);
                        viewDockerDialog.open();

                        if (viewDockerDialog.getInternalExitCode() == AzureViewDockerDialog.UPDATE_EXIT_CODE) {
                            if (dockerHost != null && !dockerHost.isUpdating) {
                                AzureDockerUIResources.updateDockerHost(PluginUtil.getParentShell(), project, new EditableDockerHost(dockerHost), dockerManager, true);
                            } else {
                                PluginUtil.displayErrorDialog(mainContainer.getShell(), "Error: Invalid Edit Selection", "The selected Docker host can not be edited at this time!");
                            }
                        }
                        setPageComplete(doValidate());
                    }
                }
                sendButtonClickedTelemetry(VIEW);
            }
        });

        dockerHostsAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                AzureNewDockerWizard newDockerWizard = new AzureNewDockerWizard(project, dockerManager);
                WizardDialog createNewDockerHostDialog = new AzureWizardDialog(mainContainer.getShell(), newDockerWizard);
                if (createNewDockerHostDialog.open() == Window.OK) {
                    DockerHost host = newDockerWizard.getDockerHost();
                    dockerImageDescription.host = host;
                    dockerImageDescription.hasNewDockerHost = true;
                    dockerImageDescription.sid = host.sid;

                    AzureDockerPreferredSettings dockerPrefferedSettings = dockerManager.getDockerPreferredSettings();
                    if (dockerPrefferedSettings == null) {
                        dockerPrefferedSettings = new AzureDockerPreferredSettings();
                    }
                    dockerPrefferedSettings.region = host.hostVM.region;
                    dockerPrefferedSettings.vmSize = host.hostVM.vmSize;
                    dockerPrefferedSettings.vmOS = host.hostOSType.name();
                    dockerManager.setDockerPreferredSettings(dockerPrefferedSettings);

                    dockerHostsList.add(0, host);

                    dockerHostsTable.setEnabled(false);
                    dockerHostsRefreshButton.setEnabled(false);
                    dockerHostsAddButton.setEnabled(false);
                    dockerHostsDeleteButton.setEnabled(false);
                    dockerHostsEditButton.setEnabled(false);
                    dockerHostsTableViewer.refresh();
                    dockerHostsTable.getItem(0).setChecked(true);
                    dockerHostsTable.select(0);
                }
                setPageComplete(doValidate());
                sendButtonClickedTelemetry(ADD);
            }
        });

        dockerHostsDeleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                //dockerHostsList
                int idx = dockerHostsTable.getSelectionIndex();

                if (idx >= 0 && dockerHostsTable.getItem(idx) != null) {
                    DockerHost deleteHost = (DockerHost) dockerHostsTable.getItem(idx).getData();
                    if (deleteHost != null) {
                        Azure azureClient = dockerManager.getSubscriptionsMap().get(deleteHost.sid).azureClient;
                        int option = AzureDockerUIResources.deleteAzureDockerHostConfirmationDialog(mainContainer.getShell(), azureClient, deleteHost);

                        if (option != 1 && option != 2) {
                            if (AzureDockerUtils.DEBUG) System.out.format("User canceled delete Docker host op: %d\n", option);
                            return;
                        }
                        dockerHostsList.remove(deleteHost);
                        if (dockerHostsTableSelection != null && dockerHostsTableSelection.row == idx) {
                            dockerHostsTableSelection = null;
                        }
                        dockerHostsTableViewer.refresh();

                        AzureDockerUIResources.deleteDockerHost(mainContainer.getShell(), project, azureClient, deleteHost, option, new Runnable() {
                            @Override
                            public void run() {
                                dockerManager.getDockerHostsList().remove(deleteHost);
                                dockerManager.refreshDockerHostDetails();
                                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshDockerHostsTable(mainContainer);
                                    }
                                });
                            }
                          });
                    }
                    setPageComplete(doValidate());
                }
                sendButtonClickedTelemetry(DELETE);
            }
        });

        dockerHostsEditButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = dockerHostsTable.getSelectionIndex();
                if (idx >= 0 && dockerHostsTable.getItem(idx) != null) {
                    DockerHost updateHost = (DockerHost) dockerHostsTable.getItem(idx).getData();
                    if (updateHost != null && !updateHost.isUpdating) {
                        AzureDockerUIResources.updateDockerHost(PluginUtil.getParentShell(), project, new EditableDockerHost(updateHost), dockerManager, true);
                    } else {
                        PluginUtil.displayErrorDialog(mainContainer.getShell(), "Error: Invalid Edit Selection", "The selected Docker host can not be edited at this time!");
                    }
                }
                setPageComplete(doValidate());
                sendButtonClickedTelemetry(EDIT);
            }
        });
    }

    private TableViewerColumn createTableViewerColumn(TableViewer tableViewer, String title, int bound, final int colNumber) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setWidth(bound);
        column.setResizable(true);
        column.setMoveable(true);

        return viewerColumn;
    }

    private void refreshDockerHostsTable(Composite mainContainer) {
        dockerHostsList.clear();

        for (DockerHost host : dockerManager.getDockerHostsList()) {
            dockerHostsList.add(host);
        }

        dockerHostsTableViewer.refresh();
        if (dockerHostsTableSelection != null) {
            int idx = 0;
            boolean selected = false;
            for (DockerHost host : dockerHostsList) {
                if (dockerHostsTableSelection.host.apiUrl.equals(host.apiUrl)) {
                    dockerHostsTableSelection.host = host;
                    dockerHostsTableSelection.row = idx;
                    selected = true;
                    break;
                }
                idx++;
            }
            if (selected) {
                dockerHostsTable.getItem(idx).setChecked(true);
            } else {
                dockerHostsTableSelection = null;
            }
        }
    }

    public void selectDefaultDockerHost(DockerHost dockerHost, boolean selectOtherHosts) {
        if (dockerHost != null) {
            int idx = 0;
            DockerHost selected = null;
            for (DockerHost host : dockerHostsList) {
                if (dockerHost.apiUrl.equals(host.apiUrl)) {
                    selected = host;
                    break;
                }
                idx++;
            }
            if (selected != null) {
                if (dockerHostsTableSelection == null) {
                    dockerHostsTableSelection = new DockerHostsTableSelection();
                }
                dockerHostsTableSelection.row = idx;
                dockerHostsTableSelection.host = selected;
            } else {
                dockerHostsTableSelection = null;
            }
        }
    }

    public boolean doValidate() {
        if (dockerImageNameTextField.getText() == null || dockerImageNameTextField.getText().equals("")) {
            errDispatcher.addMessage("dockerImageNameTextField", AzureDockerValidationUtils.getDockerImageNameTip(), null, IMessageProvider.ERROR, dockerImageNameTextField);
            setErrorMessage("Invalid Docker image name");
            return false;
        } else {
            errDispatcher.removeMessage("dockerImageNameTextField", dockerImageNameTextField);
            setErrorMessage(null);
            dockerImageDescription.dockerImageName = dockerImageNameTextField.getText();
            wizard.setDockerContainerName(AzureDockerUtils.getDefaultDockerContainerName(dockerImageDescription.dockerImageName));
        }

        String artifactPath = dockerArtifactPathTextField.getText();
        if (artifactPath == null || !AzureDockerValidationUtils.validateDockerArtifactPath(artifactPath)) {
            errDispatcher.addMessage("dockerArtifactPathTextField", AzureDockerValidationUtils.getDockerArtifactPathTip(), null, IMessageProvider.ERROR, dockerArtifactPathTextField);
            setErrorMessage("Invalid artifact path");
            return false;
        } else {
            String artifactFileName = new File(artifactPath).getName();
            dockerImageDescription.artifactName = artifactFileName.indexOf(".") > 0 ? artifactFileName.substring(0, artifactFileName.lastIndexOf(".")) : "";
            if (dockerImageDescription.artifactName.isEmpty()) {
                errDispatcher.addMessage("dockerArtifactPathTextField", AzureDockerValidationUtils.getDockerArtifactPathTip(), null, IMessageProvider.ERROR, dockerArtifactPathTextField);
                setErrorMessage("Invalid artifact path: missing file name");
                return false;
            }
            dockerImageDescription.artifactPath = artifactPath;
            dockerImageDescription.hasRootDeployment = artifactFileName.toLowerCase().matches(".*.jar");
            errDispatcher.removeMessage("dockerArtifactPathTextField", dockerArtifactPathTextField);
            setErrorMessage(null);
        }

        if (dockerHostsTableSelection == null && !dockerImageDescription.hasNewDockerHost) {
            String errMsg = "Missing check for the Docker host to publish into";
            errDispatcher.addMessage("dockerHostsTable", errMsg, null, IMessageProvider.ERROR, dockerHostsTable);
            setErrorMessage(errMsg);
            return false;
        } else {
            errDispatcher.removeMessage("dockerHostsTable", dockerHostsTable);
            setErrorMessage(null);
            dockerImageDescription.dockerImageName = dockerImageNameTextField.getText();
        }

        if (!dockerImageDescription.hasNewDockerHost) {
            dockerImageDescription.host = dockerHostsTableSelection.host;
            dockerImageDescription.sid = dockerImageDescription.host.sid;
        }

        return true;
    }

    private class DockerHostsTableSelection {
        int row;
        DockerHost host;
    }

}
