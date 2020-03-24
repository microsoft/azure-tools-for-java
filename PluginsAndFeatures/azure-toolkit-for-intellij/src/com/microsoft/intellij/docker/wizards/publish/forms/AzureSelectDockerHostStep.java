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

package com.microsoft.intellij.docker.wizards.publish.forms;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.ui.*;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.AzureDockerPreferredSettings;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.docker.dialogs.AzureViewDockerDialog;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardDialog;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardModel;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardModel;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardStep;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class AzureSelectDockerHostStep extends AzureSelectDockerWizardStep implements TelemetryProperties {
  private static final Logger LOGGER = Logger.getInstance(AzureSelectDockerHostStep.class);

  private JPanel rootPanel;
  private JPanel dockerHostsPanel;
  private JTextField dockerImageNameTextField;
  private JLabel dockerImageNameLabel;
  private JLabel dockerArtifactPathLabel;
  private ComboboxWithBrowseButton dockerArtifactComboboxWithBrowse;
  private JBTable dockerHostsTable;

  private AzureSelectDockerWizardModel model;
  private AzureDockerHostsManager dockerManager;
  private AzureDockerImageInstance dockerImageDescription;
//  private Artifact artifact;

  private DockerHostsTableSelection dockerHostsTableSelection;

  public AzureSelectDockerHostStep(String title, AzureSelectDockerWizardModel model, AzureDockerHostsManager dockerManager, AzureDockerImageInstance dockerImageInstance) {
    // TODO: The message should go into the plugin property file that handles the various dialog titles
    super(title, "Type an image name, select the artifact's path and check a Docker host to be used");
    this.model = model;
    this.dockerManager = dockerManager;
    this.dockerImageDescription = dockerImageInstance;

    dockerImageNameTextField.setText(dockerImageInstance.dockerImageName);
    dockerImageNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerImageNameTip());
    dockerImageNameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerImageName(((JTextField) input).getText())) {
          dockerImageNameLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        } else {
          dockerImageNameLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        }
      }
    });
    dockerImageNameTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerImageNameLabel.setVisible(false);

//    dockerArtifactPath.addActionListener(UIUtils.createFileChooserListener(dockerArtifactPath, model.getProject(),
//        FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
//
//    artifact = null;
//
//    for (Artifact item : ArtifactUtil.getArtifactWithOutputPaths(model.getProject())) {
//      if (item.getArtifactType().getPresentableName().equals("Web Plugin: Archive") ||
//          item.getArtifactType().getPresentableName().equals("JAR")) {
//        artifact = item;
//        break;
//      }
//    }
//
//    if (artifact != null && AzureDockerValidationUtils.validateDockerArtifactPath(artifact.getOutputFilePath())) {
//      String artifactFileName = new File(artifact.getOutputFilePath()).getName();
//      dockerImageDescription.artifactName = artifactFileName.indexOf(".") > 0 ? artifactFileName.substring(0, artifactFileName.lastIndexOf(".")) : "";
//      dockerArtifactPath.setText(artifact.getOutputFilePath());
//      dockerArtifactPathLabel.setVisible(false);
//    } else {
//      dockerArtifactPath.setText("");
//      dockerArtifactPathLabel.setVisible(true);
//    }
//
//    dockerArtifactPath.setToolTipText(AzureDockerValidationUtils.getDockerArtifactPathTip());
//    dockerArtifactPath.getTextField().setInputVerifier(new InputVerifier() {
//      @Override
//      public boolean verify(JComponent input) {
//        if (AzureDockerValidationUtils.validateDockerArtifactPath(dockerArtifactPath.getText())) {
//          dockerArtifactPathLabel.setVisible(false);
//          setDialogButtonsState(doValidate(false) == null);
//          return true;
//        } else {
//          dockerArtifactPathLabel.setVisible(true);
//          setDialogButtonsState(false);
//          return false;
//        }
//      }
//    });
//    dockerArtifactPath.getTextField().getDocument().addDocumentListener(resetDialogButtonsState(null));

    dockerArtifactComboboxWithBrowse.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String path = (String) dockerArtifactComboboxWithBrowse.getComboBox().getSelectedItem();
        final VirtualFile[] files = FileChooser.chooseFiles(new FileChooserDescriptor(true, false, true, true, false, false),
            dockerArtifactComboboxWithBrowse, model.getProject(),
            (model.getProject() == null) && path != null && !path.isEmpty() ? LocalFileSystem.getInstance().findFileByPath(path) : null);
        if (files.length > 0) {
          final StringBuilder builder = new StringBuilder();
          for (VirtualFile file : files) {
            if (builder.length() > 0) {
              builder.append(File.pathSeparator);
            }
            builder.append(FileUtil.toSystemDependentName(file.getPath()));
          }
          path = builder.toString();
          int idx = dockerArtifactComboboxWithBrowse.getComboBox().getItemCount() - 1;
          for (; idx >= 0; idx--) {
            if (dockerArtifactComboboxWithBrowse.getComboBox().getItemAt(idx).equals(path)) {
              dockerArtifactComboboxWithBrowse.getComboBox().setSelectedIndex(idx);
              break;
            }
          }
          if (idx < 0) {
            dockerArtifactComboboxWithBrowse.getComboBox().addItem(path);
            dockerArtifactComboboxWithBrowse.getComboBox().setSelectedItem(path);
          }
        }
      }
    });
    dockerArtifactComboboxWithBrowse.setToolTipText(AzureDockerValidationUtils.getDockerArtifactPathTip());
    dockerArtifactComboboxWithBrowse.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerArtifactPath((String) dockerArtifactComboboxWithBrowse.getComboBox().getSelectedItem())) {
          dockerArtifactPathLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          System.out.println("\tCOMBOBOXWITHBROWSE Validator " + (String) dockerArtifactComboboxWithBrowse.getComboBox().getSelectedItem());
          return true;
        } else {
          dockerArtifactPathLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        }
      }
    });
    dockerArtifactComboboxWithBrowse.getComboBox().setEditable(true);
    dockerArtifactComboboxWithBrowse.getComboBox().setToolTipText(dockerArtifactComboboxWithBrowse.getToolTipText());
    dockerArtifactComboboxWithBrowse.getComboBox().setInputVerifier(dockerArtifactComboboxWithBrowse.getInputVerifier());
    dockerArtifactComboboxWithBrowse.getComboBox().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.out.println("\tCOMBOBOXWITHBROWSE Selector " + (String) dockerArtifactComboboxWithBrowse.getComboBox().getSelectedItem());
        setDialogButtonsState(doValidate(false) == null);
      }
    });
    for (Artifact item : ArtifactUtil.getArtifactWithOutputPaths(model.getProject())) {
      String path = item.getOutputFilePath();
      if (path != null && (path.toLowerCase().endsWith(".war") || path.toLowerCase().endsWith(".jar")) &&
          AzureDockerValidationUtils.validateDockerArtifactPath(path)) {
        dockerArtifactComboboxWithBrowse.getComboBox().addItem(path);
      }
    }
    if (dockerArtifactComboboxWithBrowse.getComboBox().getItemCount() > 0) {
      dockerArtifactComboboxWithBrowse.getComboBox().setSelectedIndex(0);
      String artifactFileName = new File((String) dockerArtifactComboboxWithBrowse.getComboBox().getItemAt(0)).getName();
      dockerImageDescription.artifactName = artifactFileName.indexOf(".") > 0 ? artifactFileName.substring(0, artifactFileName.lastIndexOf(".")) : "";
      dockerArtifactPathLabel.setVisible(false);
    } else {
      dockerArtifactPathLabel.setVisible(true);
    }

    refreshDockerHostsTable();

    if (dockerHostsTable.getRowCount() > 0) {
      DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
      tableModel.setValueAt(true, 0, 0);
      dockerHostsTableSelection = new DockerHostsTableSelection();
      dockerHostsTableSelection.row = 0;
      dockerHostsTableSelection.host = dockerManager.getDockerHostForURL((String) tableModel.getValueAt(0, 4));
      dockerHostsTable.repaint();
    }
  }

  private void createUIComponents() {
    final DefaultTableModel dockerListTableModel = new DefaultTableModel() {
      @Override
      public boolean isCellEditable(int row, int col) {
        return (col == 0);
      }

      public Class<?> getColumnClass(int colIndex) {
        return getValueAt(0, colIndex).getClass();
      }
    };

    dockerListTableModel.addColumn("");
    dockerListTableModel.addColumn("Name");
    dockerListTableModel.addColumn("State");
    dockerListTableModel.addColumn("OS");
    dockerListTableModel.addColumn("API URL");
    dockerHostsTable = new JBTable(dockerListTableModel);
    dockerHostsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    TableColumn column = dockerHostsTable.getColumnModel().getColumn(0);
    column.setMinWidth(23);
    column.setMaxWidth(23);
    column = dockerHostsTable.getColumnModel().getColumn(1);
    column.setPreferredWidth(150);
    column = dockerHostsTable.getColumnModel().getColumn(2);
    column.setPreferredWidth(30);
    column = dockerHostsTable.getColumnModel().getColumn(3);
    column.setPreferredWidth(110);
    column = dockerHostsTable.getColumnModel().getColumn(4);
    column.setPreferredWidth(150);

    dockerListTableModel.addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent tableEvent) {
        if (tableEvent.getType() == TableModelEvent.UPDATE) {
          DockerHostsTableSelection currentSelection = new DockerHostsTableSelection();
//          int column = dockerHostsTable.getSelectedColumn();
          int column = tableEvent.getColumn();
          currentSelection.row = tableEvent.getFirstRow();

          if (column == 0) {
            DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
            if ((Boolean) tableModel.getValueAt(currentSelection.row, 0)) {
              if (dockerHostsTableSelection == null) {
                dockerHostsTableSelection = currentSelection;
                dockerHostsTableSelection.host = dockerManager.getDockerHostForURL((String) tableModel.getValueAt(currentSelection.row, 4));
                if(dockerHostsTableSelection.host != null) {
                  model.setSubscription(dockerManager.getSubscriptionsMap().get(dockerHostsTableSelection.host.sid));
                }
              } else {
                int oldRow = dockerHostsTableSelection.row;
                dockerHostsTableSelection = null;
                if (currentSelection.row != oldRow) {
                  // disable previous selection
                  tableModel.setValueAt(false, oldRow, 0);
                  dockerHostsTableSelection = currentSelection;
                  dockerHostsTableSelection.host = dockerManager.getDockerHostForURL((String) tableModel.getValueAt(dockerHostsTable.getSelectedRow(), 4));
                  if (dockerHostsTableSelection.host != null) {
                    model.setSubscription(dockerManager.getSubscriptionsMap().get(dockerHostsTableSelection.host.sid));
                  }
                }
              }
              setFinishButtonState(doValidate(false) == null);
              setNextButtonState(doValidate(false) == null);
            } else {
              dockerHostsTableSelection = null;
              setFinishButtonState(false);
              setNextButtonState(false);
            }
          }
        }
      }
    });

    AnActionButton viewDockerHostsAction = new ToolbarDecorator.ElementActionButton("Details", AllIcons.Actions.Export) {
      @Override
      public void actionPerformed(AnActionEvent anActionEvent) {
        onViewDockerHostAction();
      }
    };

    AnActionButton refreshDockerHostsAction = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
      @Override
      public void actionPerformed(AnActionEvent anActionEvent) {
        AppInsightsClient.createByType(AppInsightsClient.EventType.DockerContainer, "", "Refresh", null);
        onRefreshDockerHostAction();
      }
    };

    ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(dockerHostsTable)
        .setAddAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            onAddNewDockerHostAction();
          }
        }).setEditAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton anActionButton) {
            onEditDockerHostAction();
          }
        }).setRemoveAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            onRemoveDockerHostAction();
          }
        }).setEditActionUpdater(new AnActionButtonUpdater() {
          @Override
          public boolean isEnabled(AnActionEvent e) {
            return dockerHostsTable.getSelectedRow() != -1;
          }
        }).setRemoveActionUpdater(new AnActionButtonUpdater() {
          @Override
          public boolean isEnabled(AnActionEvent e) {
            return dockerHostsTable.getSelectedRow() != -1;
          }
        }).disableUpDownActions()
        .addExtraActions(viewDockerHostsAction, refreshDockerHostsAction);

    dockerHostsPanel = tableToolbarDecorator.createPanel();
  }

  private void onRefreshDockerHostAction() {
    AzureDockerUIResources.updateAzureResourcesWithProgressDialog(model.getProject());

    refreshDockerHostsTable();
    setFinishButtonState(doValidate(false) == null);
    setNextButtonState(doValidate(false) == null);
  }

  private void onViewDockerHostAction() {
    try {
      DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
      String apiURL = (String) tableModel.getValueAt(dockerHostsTable.getSelectedRow(), 4);
      DockerHost dockerHost = dockerManager.getDockerHostForURL(apiURL);

      if (dockerHost == null) {
        throw new RuntimeException(String.format("Unexpected error: can't locate the Docker host for %s!", apiURL));
      }

      // TODO: Check if dockerHost.certVault and dockerHost.hostVM have valid values and if not warn

      AzureViewDockerDialog viewDockerDialog = new AzureViewDockerDialog(model.getProject(), dockerHost, dockerManager);
      viewDockerDialog.show();

      if (viewDockerDialog.getInternalExitCode() == AzureViewDockerDialog.UPDATE_EXIT_CODE) {
        onEditDockerHostAction();
      }
    } catch (Exception e) {
      String msg = "An error occurred while attempting to view the selected Docker host.\n" + e.getMessage();
      PluginUtil.displayErrorDialogAndLog("Error", msg, e);
      if (AzureDockerUtils.DEBUG) e.printStackTrace();
      LOGGER.error("onViewDockerHostAction", e);
      PluginUtil.displayErrorDialog("View Docker Hosts Error", msg);
    }
  }

  private void onAddNewDockerHostAction() {
    AppInsightsClient.createByType(AppInsightsClient.EventType.DockerHost, "", "Add");
    AzureNewDockerWizardModel newDockerHostModel = new AzureNewDockerWizardModel(model.getProject(), dockerManager);
    AzureNewDockerWizardDialog wizard = new AzureNewDockerWizardDialog(newDockerHostModel);
    wizard.setTitle("Create Docker Host");
    wizard.show();

    if (wizard.getExitCode() == 0) {
      dockerHostsTable.setEnabled(false);

      DockerHost host = newDockerHostModel.getDockerHost();

      dockerImageDescription.host = host;
      dockerImageDescription.hasNewDockerHost = true;
      dockerImageDescription.sid = host.sid;
      model.setSubscription(this.dockerManager.getSubscriptionsMap().get(host.sid));

      AzureDockerPreferredSettings dockerPrefferedSettings = dockerManager.getDockerPreferredSettings();
      if (dockerPrefferedSettings == null) {
        dockerPrefferedSettings = new AzureDockerPreferredSettings();
      }
      dockerPrefferedSettings.region = host.hostVM.region;
      dockerPrefferedSettings.vmSize = host.hostVM.vmSize;
      dockerPrefferedSettings.vmOS = host.hostOSType.name();
      dockerManager.setDockerPreferredSettings(dockerPrefferedSettings);

      final DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();

      if (dockerHostsTableSelection != null && (Boolean) tableModel.getValueAt(dockerHostsTableSelection.row, 0)) {
        tableModel.setValueAt(false, dockerHostsTableSelection.row, 0);
      }

      Vector<Object> row = new Vector<>();
      row.add(false);
      row.add(host.name);
      row.add("TO_BE_CREATED");
      row.add(host.hostOSType.toString());
      row.add(host.apiUrl);
      tableModel.insertRow(0, row);
      tableModel.setValueAt(true, 0, 0);
      dockerHostsTable.setRowSelectionInterval(0, 0);

      setFinishButtonState(doValidate(false) == null);
      setNextButtonState(doValidate(false) == null);
    }
  }

  public void selectDefaultDockerHost(DockerHost dockerHost, boolean selectOtherHosts) {
    dockerHostsTable.setEnabled(selectOtherHosts);

    dockerImageDescription.host = dockerHost;
    dockerImageDescription.hasNewDockerHost = false;
    dockerImageDescription.sid = dockerHost.sid;

    final DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
    tableModel.setValueAt(false, 0, 0);
    for (int i = 0; i < tableModel.getRowCount(); i++ ) {
      String apiURL = (String) tableModel.getValueAt(i, 4);
      if (dockerHost.apiUrl.equals(apiURL)) {
        tableModel.setValueAt(true, i, 0);
        dockerHostsTable.setRowSelectionInterval(i, i);
        dockerHostsTableSelection = new DockerHostsTableSelection();
        dockerHostsTableSelection.host = dockerHost;
        dockerHostsTableSelection.row = i;
        model.setSubscription(dockerManager.getSubscriptionsMap().get(dockerHostsTableSelection.host.sid));
        break;
      }
    }
  }

  private void onEditDockerHostAction() {
    try {
      DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
      String apiURL = (String) tableModel.getValueAt(dockerHostsTable.getSelectedRow(), 4);

      DockerHost updateHost = dockerManager.getDockerHostForURL(apiURL);

      if (updateHost != null && !updateHost.isUpdating) {
        AzureDockerUIResources.updateDockerHost(model.getProject(), new EditableDockerHost(updateHost), model.getDockerHostsManager(), true);
      } else {
        PluginUtil.displayErrorDialog("Error: Invalid Edit Selection", "The selected Docker host can not be edited at this time!");
      }
    } catch (Exception e) {
      setDialogButtonsState(false);
      String msg = "An error occurred while attempting to edit the selected Docker host.\n" + e.getMessage();
      if (AzureDockerUtils.DEBUG) e.printStackTrace();
      LOGGER.error("onEditDockerHostAction", e);
      PluginUtil.displayErrorDialog("Update Docker Hosts Error", msg);
    }
  }

  private void onRemoveDockerHostAction() {
    DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
    String apiURL = (String) tableModel.getValueAt(dockerHostsTable.getSelectedRow(), 4);
    DockerHost deleteHost = dockerManager.getDockerHostForURL(apiURL);
    Azure azureClient = dockerManager.getSubscriptionsMap().get(deleteHost.sid).azureClient;

    int option = AzureDockerUIResources.deleteAzureDockerHostConfirmationDialog(azureClient, deleteHost);

    if (option !=1 && option != 2) {
      if (AzureDockerUtils.DEBUG) System.out.format("User canceled delete Docker host op: %d\n", option);
      return;
    }
    AppInsightsClient.createByType(AppInsightsClient.EventType.DockerHost, "", "Remove");
    int currentRow = dockerHostsTable.getSelectedRow();
    tableModel.removeRow(currentRow);
    tableModel.fireTableDataChanged();
    if (dockerHostsTableSelection.row == currentRow) {
      dockerHostsTableSelection = null;
    }

    AzureDockerUIResources.deleteDockerHost(model.getProject(), azureClient, deleteHost, option, new Runnable() {
      @Override
      public void run() {
        dockerManager.refreshDockerHostDetails();
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            refreshDockerHostsTable();
          }
        });
      }
    });

    setFinishButtonState(doValidate(false) == null);
    setNextButtonState(doValidate(false) == null);
  }

  /* Force a refresh of the docker hosts entries in the select host table
 *   This call will retrieve the latest list of VMs from Azure suitable to be a Docker Host
 */
  private void forceRefreshDockerHostsTable() {
    dockerManager.forceRefreshDockerHosts();
    refreshDockerHostsTable();
  }

  /* Refresh the docker hosts entries in the select host table
   *
   */
  private void refreshDockerHostsTable() {
    DefaultTableModel tableModel = (DefaultTableModel) dockerHostsTable.getModel();
    String oldSelection = dockerHostsTableSelection != null ? dockerHostsTableSelection.host.apiUrl : null;
    if (dockerHostsTableSelection != null) {
      tableModel.setValueAt(false, dockerHostsTableSelection.row, 0);
      dockerHostsTableSelection = null;
    }
    if (dockerHostsTable.getSelectedRow() >= 0) {
      dockerHostsTable.removeRowSelectionInterval(dockerHostsTable.getSelectedRow(), dockerHostsTable.getSelectedRow());
    }

    int size = tableModel.getRowCount();
    while (size > 0) {
      size--;
      try {
        tableModel.removeRow(size);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    dockerHostsTable.removeAll();
    dockerHostsTable.repaint();

    try {
      List<DockerHost> dockerHosts = dockerManager.getDockerHostsList();
      boolean selected = false;
      if (dockerHosts != null) {
        int idx = 0;
        for (DockerHost host : dockerHosts) {
          Vector<Object> row = new Vector<>();
          row.add(false);
          row.add(host.name);
          row.add(host.state.toString());
          row.add(host.hostOSType.toString());
          row.add(host.apiUrl);
          tableModel.addRow(row);
          if (oldSelection != null && oldSelection.equals(host.apiUrl)) {
            tableModel.setValueAt(true, idx, 0);
            selected = true;
          }
          idx++;
        }

        if (!selected) {
          dockerHostsTableSelection = null;
        }
      }
      dockerHostsTable.repaint();
    } catch (Exception e) {
      setDialogButtonsState(false);
      String msg = "An error occurred while attempting to get the list of recognizable Docker hosts.\n" + e.getMessage();
      if (AzureDockerUtils.DEBUG) e.printStackTrace();
      LOGGER.error("refreshDockerHostsTable", e);
      PluginUtil.displayErrorDialog("Refresh Docker Hosts Error", msg);
    }
  }

  @Override
  public JComponent prepare(final WizardNavigationState state) {
    rootPanel.revalidate();
    setFinishButtonState(true);
    setNextButtonState(true);

    return rootPanel;
  }

  private void setFinishButtonState(boolean finishButtonState) {
    // Dialog buttons might not be ready when we call to set them - see tableChanged listener
    try {
      model.getCurrentNavigationState().FINISH.setEnabled(finishButtonState);
    } catch (Exception ignored) {}
  }

  private void setNextButtonState(boolean nextButtonState) {
    // Dialog buttons might not be ready when we call to set them - see tableChanged listener
    try {
      model.getCurrentNavigationState().NEXT.setEnabled(nextButtonState);
      model.getCurrentNavigationState().PREVIOUS.setEnabled(false);
    } catch (Exception ignored) {}
  }

  @Override
  protected void setDialogButtonsState(boolean buttonsState) {
    setFinishButtonState(buttonsState);
    setNextButtonState(buttonsState);
  }

  public ValidationInfo doValidate(boolean shakeOnError) {
    if (dockerImageNameTextField.getText() == null || dockerImageNameTextField.getText().equals("")) {
      ValidationInfo info = new ValidationInfo("Missing Docker image name", dockerImageNameTextField);
      dockerImageNameLabel.setVisible(true);
      setDialogButtonsState(false);
      if (shakeOnError)
        model.getSelectDockerWizardDialog().DialogShaker(info);
      return info;
    }
    dockerImageDescription.dockerImageName = dockerImageNameTextField.getText();
    model.setDockerContainerName(AzureDockerUtils.getDefaultDockerContainerName(dockerImageDescription.dockerImageName));

//    if (dockerArtifactPath.getText() == null || !AzureDockerValidationUtils.validateDockerArtifactPath(dockerArtifactPath.getText())) {
//      ValidationInfo info = new ValidationInfo("Invalid artifact file name", dockerArtifactPath);
//      dockerArtifactPathLabel.setVisible(true);
//      setDialogButtonsState(false);
//      if (shakeOnError)
//        model.getSelectDockerWizardDialog().DialogShaker(info);
//      return info;
//    }
//    String artifactFilePath = dockerArtifactPath.getText();
    String dockerArtifactPath = (String) dockerArtifactComboboxWithBrowse.getComboBox().getSelectedItem();
    if (dockerArtifactPath == null || !AzureDockerValidationUtils.validateDockerArtifactPath(dockerArtifactPath)) {
      ValidationInfo info = new ValidationInfo("Invalid artifact file name", dockerArtifactComboboxWithBrowse);
      dockerArtifactPathLabel.setVisible(true);
      setDialogButtonsState(false);
      if (shakeOnError)
        model.getSelectDockerWizardDialog().DialogShaker(info);
      return info;
    }
    String artifactFileName = new File(dockerArtifactPath).getName();
    dockerImageDescription.artifactName = artifactFileName.indexOf(".") > 0 ? artifactFileName.substring(0, artifactFileName.lastIndexOf(".")) : "";
    if (dockerImageDescription.artifactName.isEmpty()) {
      ValidationInfo info = new ValidationInfo("Invalid artifact file name (it is missing a file name)", dockerArtifactComboboxWithBrowse);
//      ValidationInfo info = new ValidationInfo("Invalid artifact file name (it's missing a file name)", dockerArtifactPath);
      dockerArtifactPathLabel.setVisible(true);
      setDialogButtonsState(false);
      if (shakeOnError)
        model.getSelectDockerWizardDialog().DialogShaker(info);
      return info;
    }
    if (dockerImageDescription.artifactPath == null || !dockerImageDescription.artifactPath.equals(dockerArtifactPath)) {
      dockerImageDescription.artifactPath = dockerArtifactPath;
      dockerImageDescription.hasRootDeployment = artifactFileName.toLowerCase().matches(".*.jar");
      model.setPredefinedDockerfileOptions(artifactFileName);
    }

    if (dockerHostsTableSelection == null && !dockerImageDescription.hasNewDockerHost){
      ValidationInfo info = new ValidationInfo("Please check a Docker host or create a new", dockerHostsTable);
      setDialogButtonsState(false);
      if (shakeOnError)
        model.getSelectDockerWizardDialog().DialogShaker(info);
      return info;
    }
    if (!dockerImageDescription.hasNewDockerHost) {
      dockerImageDescription.host = dockerHostsTableSelection.host;
      dockerImageDescription.sid = dockerImageDescription.host.sid;
    }

    setDialogButtonsState(true);

    return null;
  }

  public ValidationInfo doValidate() {
    return doValidate(true);
  }

  @Override
  public WizardStep onNext(final AzureSelectDockerWizardModel model) {
    if (dockerHostsTableSelection != null && doValidate() == null) {
      return super.onNext(model);
    } else {
      setDialogButtonsState(false);
      ValidationInfo info = new ValidationInfo("Please check a Docker host or create a new", dockerHostsTable);
      model.getSelectDockerWizardDialog().DialogShaker(info);
      return this;
    }
  }

  @Override
  public boolean onFinish() {
    setFinishButtonState(false);
    return model.doValidate() == null && super.onFinish();
  }

  @Override
  public boolean onCancel() {
    setFinishButtonState(false);
    model.finishedOK = true;
    return super.onCancel();
  }

  private class DockerHostsTableSelection {
    int row;
    DockerHost host;
  }

  @Override
  public Map<String, String> toProperties() {
    return model.toProperties();
  }

// CREATE CUSTOM ACTION FOR DIALOG WRAPPER!!!!!
//  @Nullable
//  @Override
//  protected Action[] createActions() {
//    myClickApplyAction = new ClickApplyAction();
//    myClickApplyAction.setEnabled(false);
//    return new Action[] {getCancelAction(), myClickApplyAction, getOKAction()};
//  }
//
//  protected class ClickApplyAction extends DialogWrapper.DialogWrapperAction {
//    protected ClickApplyAction() {
//      super("Apply");
//    }
//
//    protected void doAction(ActionEvent e) {
//      ValidationInfo info = doClickApplyValidate();
//      if(info != null) {
//        dialogShaker(info);
//      } else {
//        doClickApplyAction();
//      }
//    }
//  }

}
