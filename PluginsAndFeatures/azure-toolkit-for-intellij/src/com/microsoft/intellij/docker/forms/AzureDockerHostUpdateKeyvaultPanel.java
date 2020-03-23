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

import com.intellij.openapi.project.Project;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.EditableDockerHost;
import org.jdesktop.swingx.JXHyperlink;

import javax.swing.*;

public class AzureDockerHostUpdateKeyvaultPanel {
  private JPanel contentPane;
  private JPanel mainPanel;
  private JRadioButton keepCurrentSettingsRadioButton;
  private JRadioButton donTUseKeyRadioButton;
  private JXHyperlink dockerHostDeleteKeyvaultHyperlink;
  private JRadioButton newKeyVaultRadioButton;
  private JTextField dockerHostNewKeyvaultTextField;

  private Project project;
  private EditableDockerHost editableHost;
  private AzureDockerHostsManager dockerManager;

  public AzureDockerHostUpdateKeyvaultPanel(Project project, EditableDockerHost editableHost, AzureDockerHostsManager dockerUIManager) {
    this.project = project;
    this.editableHost = editableHost;
    this.dockerManager = dockerManager;
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }
}
