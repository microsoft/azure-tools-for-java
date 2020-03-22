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
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AzureSelectKeyVault extends AzureDialogWrapper {
  private JPanel rootPanel;
  private JComboBox<String> dockerKeyvaultsComboBox;

  private AzureDockerHostsManager dockerManager;
  private String keyvault;

  public AzureSelectKeyVault(Project project, AzureDockerHostsManager dockerManager) {
    super(project, false);

    this.dockerManager = dockerManager;
    keyvault = null;

    init();
    setTitle("Select Azure Key Vault");

    DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
    if (dockerManager != null && dockerManager.getDockerVaultsMap() != null) {
      for (String keyvaultName : dockerManager.getDockerVaultsMap().keySet()) {
        comboBoxModel.addElement(keyvaultName);
      }
      dockerKeyvaultsComboBox.setModel(comboBoxModel);
      if (dockerKeyvaultsComboBox.getItemCount() > 0) {
        dockerKeyvaultsComboBox.setSelectedIndex(0);
        keyvault = (String) dockerKeyvaultsComboBox.getSelectedItem();
      }
    }

  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return rootPanel;
  }

  @Override
  protected void doOKAction() {
    keyvault = (String) dockerKeyvaultsComboBox.getSelectedItem();
    super.doOKAction();
  }

  public String getSelectedKeyvault() {
    return keyvault;
  }

}
