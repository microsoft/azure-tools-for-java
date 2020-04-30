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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AzureExportDockerTlsKeysDialog extends AzureDialogWrapper {
  private JPanel mainPanel;
  private TextFieldWithBrowseButton exportTlsPath;

  private String path;

  public AzureExportDockerTlsKeysDialog(Project project) {
    super(project, true);

    setModal(true);

    exportTlsPath.addActionListener(UIUtils.createFileChooserListener(exportTlsPath, project,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()));
    path = project.getBasePath();
    exportTlsPath.setText(path);
    exportTlsPath.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        return Files.isDirectory(Paths.get(exportTlsPath.getText()));
      }
    });

    init();
    setTitle("Export TLS Certificates");
  }

  public String getPath() { return path;}

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return mainPanel;
  }

  @Nullable
  @Override
  protected String getHelpId() {
    return null;
  }

  @Nullable
  @Override
  protected Action[] createActions() {
    Action exportAction = getOKAction();
    exportAction.putValue(Action.NAME, "Save");
    return new Action[] {getCancelAction(), exportAction};
  }

  @Nullable
  @Override
  protected void doOKAction() {
    path = exportTlsPath.getText();
    super.doOKAction();
  }

}
