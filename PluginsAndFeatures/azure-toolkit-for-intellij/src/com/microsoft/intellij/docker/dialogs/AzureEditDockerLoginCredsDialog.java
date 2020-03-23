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

package com.microsoft.intellij.docker.dialogs;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.intellij.docker.forms.AzureDockerHostUpdateDaemonPanel;
import com.microsoft.intellij.docker.forms.AzureDockerHostUpdateLoginPanel;
import com.microsoft.intellij.docker.forms.AzureDockerHostUpdateState;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class AzureEditDockerLoginCredsDialog extends AzureDialogWrapper {
  private JPanel contentPane;
  private JPanel selectionPanel;
  private JPanel menuPanel;
  private JList titleSelectList;
  private JPanel mainPanel;
  private JList list1;
  private DockerHostEditPanel[] editPanels;

  private Project project;
  private AzureDockerHostsManager dockerManager;
  private EditableDockerHost editableHost;
  private DockerHost dockerHost;

  public AzureEditDockerLoginCredsDialog(Project project, EditableDockerHost editableHost, AzureDockerHostsManager dockerUIManager) {
    super(project, true);

    this.project = project;
    this.dockerManager = dockerUIManager;
    this.editableHost = editableHost;

    setModal(true);

    init();
    setTitle("Updating " + editableHost.originalDockerHost.name);

    DefaultListModel<DockerHostEditPanel> selectionModel = new DefaultListModel<>();

    for (DockerHostEditPanel item : editPanels) {
      selectionModel.addElement(item);
    }

    list1.setModel(selectionModel);
    list1.setFixedCellHeight(titleSelectList.getFont().getSize() * 2);
    list1.setSelectedIndex(0);
    list1.revalidate();
    list1.repaint();

    titleSelectList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        selectionPanel.removeAll();
        int idx = titleSelectList.getSelectedIndex();
        if (idx < 0 || idx > 2) {
          idx = 0;
        }
        selectionPanel.add(editPanels[idx].getPanel());
        editPanels[idx].getPanel().setSize(selectionPanel.getSize());
        selectionPanel.revalidate();
        selectionPanel.repaint();
      }
    });
    list1.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        selectionPanel.removeAll();
        int idx = list1.getSelectedIndex();
        if (idx < 0 || idx > 2) {
          idx = 0;
        }
        selectionPanel.add(editPanels[idx].getPanel());
        selectionPanel.revalidate();
        selectionPanel.repaint();
      }
    });
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return contentPane;
  }

  @Nullable
  @Override
  protected String getHelpId() {
    return null;
  }

  @Nullable
  @Override
  protected Action[] createActions() {
    Action updateAction = getOKAction();
    updateAction.putValue(Action.NAME, "Update");
    return new Action[] {getCancelAction(), updateAction};
  }

  @Nullable
  @Override
  protected void doOKAction() {
    try {
      ValidationInfo info = doUpdateValidate();
      if(info != null) {
        DialogShaker(info);
      } else {
        executeUpdate();
        super.doOKAction();
      }
    }
    catch (Exception e){
      String msg = "An error occurred while attempting to update the Docker host settings:\n" + e.getMessage();
      PluginUtil.displayErrorDialogAndLog("Error", msg, e);
    }
  }

  private void createUIComponents() {
    editPanels = new DockerHostEditPanel[] {
        new DockerHostEditPanel("Log in credentials", new AzureDockerHostUpdateLoginPanel(project, editableHost, dockerManager, this).getMainPanel()),
        new DockerHostEditPanel("Docker daemon settings", new AzureDockerHostUpdateDaemonPanel(project, editableHost, dockerManager).getMainPanel()),
//        new DockerHostEditPanel("Key vault settings", new AzureDockerHostUpdateKeyvaultPanel(project, editableHost, dockerManager).getMainPanel()),
        new DockerHostEditPanel("Virtual machine state", new AzureDockerHostUpdateState(project, editableHost, dockerManager).getMainPanel()),
    };
    selectionPanel = new JPanel();
    selectionPanel.add(editPanels[0].getPanel());
    DefaultListModel<DockerHostEditPanel> selectionModel = new DefaultListModel<>();

    for (DockerHostEditPanel item : editPanels) {
      selectionModel.addElement(item);
    }

    titleSelectList = new JList(selectionModel);
    titleSelectList.setFixedCellHeight(titleSelectList.getFont().getSize() * 2);
//    titleSelectList.setOpaque(false);
    titleSelectList.setSelectedIndex(0);
    titleSelectList.revalidate();
    titleSelectList.repaint();
  }

  class DockerHostEditPanel {
    String itemName;
    JPanel mainPanel;

    public DockerHostEditPanel(String name, JPanel panel) {
      this.itemName = name;
      this.mainPanel = panel;
    }

    public String getName() {
      return itemName;
    }

    public JPanel getPanel() {
      return mainPanel;
    }

    public void setName(String name) {
      this.itemName = name;
    }

    public void setPanel(JPanel panel) {
      this.mainPanel = panel;
    }

    @Override
    public String toString() {
      return itemName;
    }
  }

  private ValidationInfo doUpdateValidate() {
    // return new ValidationInfo("Apply is not implemented", this.getButton(getOKAction()));
    return null;
  }

  private void doUpdateAction() {
    if (editableHost.updatedDockerHost.equalsTo(editableHost.originalDockerHost)) {
      return;
    }

    editableHost.updatedDockerHost.isUpdating = true;
//    initDefaultUIValues(editableHost.updatedDockerHost, " (updating...)");
//    myClickApplyAction.setEnabled(false);

    executeUpdate();

    editableHost.originalDockerHost = dockerManager.getDockerHostForURL(editableHost.updatedDockerHost.apiUrl);
  }

  public void executeUpdate() {
    if (editableHost.updatedDockerHost.equalsTo(editableHost.originalDockerHost)) {
      return;
    }

    editableHost.originalDockerHost.isUpdating = true;

    AzureEditDockerLoginCredsDialog editDockerHostDialog = this;

    AzureDockerUpdateRunner updateRunner = new AzureDockerUpdateRunner(project, this);
    updateRunner.queue();

  }

  private void DialogShaker(ValidationInfo info) {
    PluginUtil.dialogShaker(info, this);
  }

  private class AzureDockerUpdateRunner extends Task.Backgroundable {
    public AzureEditDockerLoginCredsDialog editDockerHostDialog;
    //ProgressIndicator progressIndicator;
    public AzureDockerUpdateRunner(Project project, AzureEditDockerLoginCredsDialog editDockerHostDialog) {
      super(project, "Updating Docker Host...", true);
      this.editDockerHostDialog = editDockerHostDialog;
    }

    @Override
    public void onCancel() {
      editDockerHostDialog.editableHost.originalDockerHost.isUpdating = false;
      super.onCancel();
    }

    @Override
    public void onSuccess() {
      editDockerHostDialog.editableHost.originalDockerHost.isUpdating = false;
      super.onSuccess();
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
      try {
        Thread workThread = new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              // TODO: split this into multiple steps and do simple ops with progress bar for each update
              dockerManager.updateDockerHost(editDockerHostDialog.editableHost.originalDockerHost, editDockerHostDialog.editableHost.updatedDockerHost);
              Thread.sleep(1);
            } catch (InterruptedException e) {
              PluginUtil.displayInfoDialog("Updater", "got stopped");
            }
          }
        });

        workThread.start();
        // timeout after 10 minutes
        Thread.sleep(360000);
        workThread.interrupt();
      } catch (Exception e) {
        String msg = "An error occurred while attempting to update the Docker host settings: \n" + e.getMessage();
        PluginUtil.displayErrorDialogAndLog("Error", msg, e);
      }
      editDockerHostDialog.editableHost.originalDockerHost.isUpdating = false;

//      if (editDockerHostDialog.isVisible()) {
//        ApplicationManager.getApplication().invokeLater(new Runnable() {
//          @Override
//          public void run() {
//            initDefaultUIValues(editableHost.originalDockerHost, null);
//          }
//        }, ModalityState.any());
//      }
    }
  }

  private class AzureDockerUpdateTimer implements Runnable {
    private AzureDockerUpdateRunner updateRunner;

    public AzureDockerUpdateTimer(AzureDockerUpdateRunner updateRunner) {
      this.updateRunner = updateRunner;
    }

    @Override
    public void run() {
      updateRunner.queue();
    }
  }

}
