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

package com.microsoft.intellij.docker.forms;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.intellij.docker.dialogs.AzureSelectKeyVault;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.intellij.ui.util.UIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AzureDockerHostUpdateDaemonPanel {
  private JPanel contentPane;
  private JPanel mainPanel;
  private JTextField dockerDaemonPortTextField;
  private JRadioButton dockerHostNoTlsRadioButton;
  private JRadioButton dockerHostEnableTlsRadioButton;
  private JRadioButton dockerHostAutoTlsRadioButton;
  private JRadioButton dockerHostImportTlsRadioButton;
  private TextFieldWithBrowseButton dockerHostImportTLSBrowseTextField;
  private JRadioButton dockerHostKeepTlsRadioButton;
  private JButton dockerSelectKeyvaultButton;
  private ButtonGroup mainSelectionGroup;

  private Project project;
  private EditableDockerHost editableHost;
  private AzureDockerHostsManager dockerManager;


  public AzureDockerHostUpdateDaemonPanel(Project project, EditableDockerHost editableHost, AzureDockerHostsManager dockerUIManager) {
    this.project = project;
    this.editableHost = editableHost;
    this.dockerManager = dockerUIManager;

    dockerDaemonPortTextField.setText(editableHost.updatedDockerHost.port);

    mainSelectionGroup = new ButtonGroup();
    mainSelectionGroup.add(dockerHostNoTlsRadioButton);
    mainSelectionGroup.add(dockerHostKeepTlsRadioButton);
    mainSelectionGroup.add(dockerHostAutoTlsRadioButton);
    mainSelectionGroup.add(dockerHostImportTlsRadioButton);

    initDefaultUIState();
    dockerSelectKeyvaultButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AzureSelectKeyVault selectKeyvaultDialog = new AzureSelectKeyVault(project, dockerUIManager);
        selectKeyvaultDialog.show();

        if (selectKeyvaultDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
          updateUIWithKeyvault(selectKeyvaultDialog.getSelectedKeyvault());
        }
      }
    });
    dockerHostNoTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(false);
      }
    });
    dockerHostNoTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(false);
      }
    });
    dockerHostAutoTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(false);
      }
    });
    dockerHostImportTlsRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostImportTLSBrowseTextField.setEnabled(true);
      }
    });
  }

  private void initDefaultUIState() {
    if (editableHost.originalDockerHost.isTLSSecured) {
      dockerHostKeepTlsRadioButton.setSelected(true);
    } else {
      dockerHostNoTlsRadioButton.setSelected(true);
    }

    if(editableHost.isUpdated) {
      dockerDaemonPortTextField.setText(editableHost.originalDockerHost.port + " (updating)");
      dockerDaemonPortTextField.setEnabled(false);
      disableTlsUI();
    } else {
      dockerDaemonPortTextField.setText(editableHost.originalDockerHost.port);
    }

    dockerHostImportTLSBrowseTextField.addActionListener(UIUtils.createFileChooserListener(dockerHostImportTLSBrowseTextField, project,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));
    dockerHostImportTLSBrowseTextField.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        return AzureDockerValidationUtils.validateDockerHostTlsDirectory(dockerHostImportTLSBrowseTextField.getText());
      }
    });

  }

  public void disableTlsUI() {
    dockerHostNoTlsRadioButton.setEnabled(false);
    dockerHostKeepTlsRadioButton.setEnabled(false);
    dockerHostAutoTlsRadioButton.setEnabled(false);
    dockerHostImportTlsRadioButton.setEnabled(false);
    dockerHostImportTLSBrowseTextField.setEnabled(false);
  }

  private void updateUIWithKeyvault(String keyvault) {
    // TODO: call into dockerManager to retrieve the keyvault secrets
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }
}
