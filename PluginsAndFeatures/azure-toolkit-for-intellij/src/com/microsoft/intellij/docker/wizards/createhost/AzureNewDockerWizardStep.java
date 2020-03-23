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

package com.microsoft.intellij.docker.wizards.createhost;

import com.microsoft.intellij.docker.wizards.AzureDockerValidatable;
import com.microsoft.intellij.ui.components.AzureWizardStep;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class AzureNewDockerWizardStep extends AzureWizardStep<AzureNewDockerWizardModel> implements AzureDockerValidatable {

  protected AzureNewDockerWizardStep(String title, String explanation) {
    super(title, explanation, null);
  }

  public String getHelpId() {
    return "azure_docker_project";
  }

  protected DocumentListener resetDialogButtonsState(JComponent componentLabel) {
    return new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        setDialogButtonsState(true);
        if (componentLabel != null) {
          componentLabel.setVisible(false);
        }
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        setDialogButtonsState(true);
        if (componentLabel != null) {
          componentLabel.setVisible(false);
        }
      }
    };
  }

  protected abstract void setDialogButtonsState(boolean buttonsState);
}
