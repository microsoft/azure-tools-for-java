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

import com.intellij.openapi.project.Project;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.intellij.docker.forms.AzureDockerHostUpdateLoginPanel;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AzureInputDockerLoginCredsDialog extends AzureDialogWrapper {
  private JPanel mainPanel;

  private Project project;
  private EditableDockerHost editableHost;
  private AzureDockerHostsManager dockerManager;
  private AzureDockerHostUpdateLoginPanel loginPanel;
  private boolean resetCredentials;

  public AzureInputDockerLoginCredsDialog(Project project, EditableDockerHost editableHost, AzureDockerHostsManager dockerManager, boolean resetCredentials) {
    super(project, true);

    this.project = project;
    this.editableHost = editableHost;
    this.dockerManager = dockerManager;
    this.resetCredentials = resetCredentials;

    loginPanel = new AzureDockerHostUpdateLoginPanel(project, editableHost, dockerManager, this);
    loginPanel.dockerHostAutoSshRadioButton.setVisible(resetCredentials);
    loginPanel.dockerHostSecondPwdLabel.setVisible(resetCredentials);
    loginPanel.dockerHostSecondPwdField.setVisible(resetCredentials);

    init();
    setTitle("Docker Host Log In Credentials");
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return loginPanel.getMainPanel();
    //return mainPanel;
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
    updateAction.putValue(Action.NAME, resetCredentials ? "Update" : "OK");
    return new Action[] {getCancelAction(), updateAction};
  }

  @Nullable
  @Override
  protected void doOKAction() {
    try {
      if (loginPanel.doValidate(true) == null) {
        if (editableHost.originalDockerHost.hasKeyVault &&
            !DefaultLoader.getUIHelper().showConfirmation("We've detected that the selected host's login credentials are currently loaded from an Azure Key Vault. Reseting them will remove this association and will require to enter the credentials manually.\n\n Do you want to proceed with this update?",
            "Removing Key Vault Association", new String[]{"Yes", "No"},null)) {
          return;
        } else {
          super.doOKAction();
        }
      }
    }
    catch (Exception e){
      String msg = "An error occurred while attempting to use the updated log in credentials.\n" + e.getMessage();
      PluginUtil.displayErrorDialogAndLog("Error", msg, e);
    }
  }

}
