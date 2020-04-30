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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.KnownDockerImages;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardModel;
import com.microsoft.intellij.docker.wizards.publish.AzureSelectDockerWizardStep;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;

public class AzureConfigureDockerContainerStep extends AzureSelectDockerWizardStep implements TelemetryProperties {
  private static final Logger LOGGER = Logger.getInstance(AzureConfigureDockerContainerStep.class);

  private AzureSelectDockerWizardModel model;
  private JPanel rootConfigureContainerPanel;
  private JTextField dockerContainerNameTextField;
  private JTextField dockerContainerPortSettings;
  private JRadioButton predefinedDockerfileRadioButton;
  private JRadioButton customDockerfileRadioButton;
  private ButtonGroup selectContainerButtonGroup;
  private JComboBox<KnownDockerImages> dockerfileComboBox;
  private TextFieldWithBrowseButton customDockerfileBrowseButton;
  private JLabel dockerContainerNameLabel;
  private JLabel customDockerfileBrowseLabel;
  private JLabel dockerContainerPortSettingsLabel;

  private AzureDockerHostsManager dockerManager;
  private AzureDockerImageInstance dockerImageDescription;
  private int randomPort;

  public AzureConfigureDockerContainerStep(String title, AzureSelectDockerWizardModel model, AzureDockerHostsManager dockerManager, AzureDockerImageInstance dockerImageInstance) {
    // TODO: The message should go into the plugin property file that handles the various dialog titles
    super(title, "Configure the Docker container to be created");
    this.model = model;
    this.dockerManager = dockerManager;
    this.dockerImageDescription = dockerImageInstance;

    this.randomPort = new Random().nextInt(10000); // default to host port 2xxxx and 5xxxx (debug)

    initUI();
  }

  private void initUI() {
    dockerContainerNameTextField.setText(dockerImageDescription.dockerContainerName);
    dockerContainerNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerContainerNameTip());
    dockerContainerNameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerContainerName(((JTextField) input).getText())) {
          dockerContainerNameLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        } else {
          dockerContainerNameLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        }
      }
    });
    dockerContainerNameTextField.getDocument().addDocumentListener(resetDialogButtonsState(dockerContainerNameLabel));
    dockerContainerNameLabel.setVisible(false);

    selectContainerButtonGroup = new ButtonGroup();
    selectContainerButtonGroup.add(predefinedDockerfileRadioButton);
    selectContainerButtonGroup.add(customDockerfileRadioButton);

    predefinedDockerfileRadioButton.setSelected(true);
    dockerfileComboBox.setEnabled(true);

    for (KnownDockerImages image : dockerManager.getDefaultDockerImages()) {
      // Add predefined images that can run .WAR as default
      if (!image.isCanRunJarFile()) {
        dockerfileComboBox.addItem(image);
      }
    }

    if (dockerImageDescription.predefinedDockerfile != null) {
      dockerfileComboBox.setSelectedItem(KnownDockerImages.valueOf(dockerImageDescription.predefinedDockerfile));
    } else {
      dockerfileComboBox.setSelectedIndex(0);
    }
    dockerfileComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerContainerPortSettings.setText(getDefaultPortMapping((KnownDockerImages) dockerfileComboBox.getSelectedItem()));
        setDialogButtonsState(doValidate(false) == null);
      }
    });

    customDockerfileBrowseButton.setEnabled(false);
    customDockerfileBrowseButton.addActionListener(UIUtils.createFileChooserListener(customDockerfileBrowseButton, model.getProject(),
        FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
    customDockerfileBrowseButton.getTextField().setToolTipText(AzureDockerValidationUtils.getDockerfilePathTip());
    customDockerfileBrowseButton.getTextField().setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (customDockerfileRadioButton.isSelected() && !AzureDockerValidationUtils.validateDockerfilePath(customDockerfileBrowseButton.getText())) {
          customDockerfileBrowseLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        } else {
          customDockerfileBrowseLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        }
      }
    });
    customDockerfileBrowseButton.getTextField().getDocument().addDocumentListener(resetDialogButtonsState(customDockerfileBrowseLabel));
    customDockerfileBrowseLabel.setVisible(false);
    customDockerfileBrowseButton.setText("");

    predefinedDockerfileRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerfileComboBox.setEnabled(true);
        customDockerfileBrowseLabel.setVisible(false);
        customDockerfileBrowseButton.setEnabled(false);
        setDialogButtonsState(doValidate(false) == null);
      }
    });

    customDockerfileRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerfileComboBox.setEnabled(false);
        customDockerfileBrowseButton.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerfilePath(customDockerfileBrowseButton.getText())) {
          customDockerfileBrowseLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
        } else {
          customDockerfileBrowseLabel.setVisible(true);
          setDialogButtonsState(false);
        }
      }
    });

    dockerContainerPortSettings.setText(getDefaultPortMapping((KnownDockerImages) dockerfileComboBox.getSelectedItem()));
    dockerContainerPortSettings.setToolTipText(AzureDockerValidationUtils.getDockerPortSettingsTip());
    dockerContainerPortSettings.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerPortSettings(dockerContainerPortSettings.getText()) != null) {
          dockerContainerPortSettingsLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        } else {
          dockerContainerPortSettingsLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        }
      }
    });
    dockerContainerPortSettings.getDocument().addDocumentListener(resetDialogButtonsState(dockerContainerPortSettingsLabel));
    dockerContainerPortSettingsLabel.setVisible(false);

  }

//  @Override
  public ValidationInfo doValidate() {
    return doValidate(true);
  }

  private ValidationInfo doValidate(Boolean shakeOnError) {
    if (dockerContainerNameTextField.getText() == null || dockerContainerNameTextField.getText().equals("") ||
        !AzureDockerValidationUtils.validateDockerContainerName(dockerContainerNameTextField.getText())) {
      ValidationInfo info = new ValidationInfo("Please name your Docker container", dockerContainerNameTextField);
      dockerContainerNameLabel.setVisible(true);
      setDialogButtonsState(false);
      if (shakeOnError) model.getSelectDockerWizardDialog().DialogShaker(info);
      return info;
    }
    dockerContainerNameLabel.setVisible(false);
    dockerImageDescription.dockerContainerName = dockerContainerNameTextField.getText();
    dockerContainerNameLabel.setVisible(false);

    if (predefinedDockerfileRadioButton.isSelected()) {
      KnownDockerImages dockerfileImage = (KnownDockerImages) dockerfileComboBox.getSelectedItem();
      if (dockerfileImage == null) {
        ValidationInfo info = new ValidationInfo("Please select a Docker image type form the list", dockerfileComboBox);
        setDialogButtonsState(false);
        if (shakeOnError) model.getSelectDockerWizardDialog().DialogShaker(info);
        return info;
      }
      dockerImageDescription.predefinedDockerfile = dockerfileImage.name();
      if (dockerImageDescription.artifactPath != null) {
        dockerImageDescription.dockerfileContent = dockerfileImage.getDockerfileContent()
            .replace(KnownDockerImages.DOCKER_ARTIFACT_FILENAME, new File(dockerImageDescription.artifactPath).getName());
      }
    }

    if (customDockerfileRadioButton.isSelected()) {
      String dockerfileName = customDockerfileBrowseButton.getText();
      if (dockerfileName == null || dockerfileName.equals("") || Files.notExists(Paths.get(dockerfileName))) {
        ValidationInfo info = new ValidationInfo("Dockerfile not found", customDockerfileBrowseButton);
        customDockerfileBrowseLabel.setVisible(true);
        setDialogButtonsState(false);
        if (shakeOnError) model.getSelectDockerWizardDialog().DialogShaker(info);
        return info;
      }
      dockerImageDescription.predefinedDockerfile = null;

      try {
        dockerImageDescription.dockerfileContent = new String(Files.readAllBytes(Paths.get(customDockerfileBrowseButton.getText())));
        customDockerfileBrowseLabel.setVisible(true);
      } catch (Exception e) {
        customDockerfileBrowseLabel.setVisible(true);
        setDialogButtonsState(false);

        String errTitle = "Error reading Dockerfile content";
        String msg = "An error occurred while attempting to get the content of " + customDockerfileBrowseButton.getText() + "\n" + e.getMessage();
        if (AzureDockerUtils.DEBUG) e.printStackTrace();
        LOGGER.error("doValidate", e);
        PluginUtil.displayErrorDialog(errTitle, msg);

        return new ValidationInfo(errTitle, customDockerfileBrowseButton);
      }
    }

    if (dockerContainerPortSettings.getText() == null || dockerContainerPortSettings.getText().equals("") ||
        AzureDockerValidationUtils.validateDockerPortSettings(dockerContainerPortSettings.getText()) == null) {
      ValidationInfo info = new ValidationInfo("Invalid port settings", dockerContainerPortSettings);
      if (shakeOnError) model.getSelectDockerWizardDialog().DialogShaker(info);
      dockerContainerPortSettingsLabel.setVisible(true);
      setDialogButtonsState(false);
      return info;
    }
    dockerImageDescription.dockerPortSettings = dockerContainerPortSettings.getText();

    setDialogButtonsState(true);

    return null;
  }

  private String getDefaultPortMapping(KnownDockerImages knownImage) {
    if (knownImage == null) {
      return String.format("2%04d:80", randomPort);
    }
    if (knownImage.getDebugPortSettings() != null && !knownImage.getDebugPortSettings().isEmpty()) {
      // TODO: add JVM port mapping
      return String.format("2%04d:%s 5%04d:%s", randomPort, knownImage.getPortSettings(), randomPort, knownImage.getDebugPortSettings());
    } else {
      return String.format("2%04d:%s", randomPort, knownImage.getPortSettings());
    }
  }

  public void setPredefinedDockerfileOptions(String artifactFileName) {
    boolean isJarFile = artifactFileName.toLowerCase().matches(".*.jar");
    dockerfileComboBox.removeAllItems();
    for (KnownDockerImages image : dockerManager.getDefaultDockerImages()) {
      if (image.isCanRunJarFile() == isJarFile) {
        dockerfileComboBox.addItem(image);
      }
    }
    if (dockerImageDescription.predefinedDockerfile != null) {
      dockerfileComboBox.setSelectedItem(KnownDockerImages.valueOf(dockerImageDescription.predefinedDockerfile));
    } else {
      dockerfileComboBox.setSelectedIndex(0);
    }
    dockerContainerPortSettings.setText(getDefaultPortMapping((KnownDockerImages) dockerfileComboBox.getSelectedItem()));
  }

  public void setDockerContainerName(String dockerContainerName) {
    if (dockerContainerName != null) {
      dockerContainerNameTextField.setText(dockerContainerName);
    }
  }

  private void setFinishButtonState(boolean finishButtonState) {
    try {
      model.getCurrentNavigationState().FINISH.setEnabled(finishButtonState);
    } catch (Exception ignored) {}
  }

  private void setPreviousButtonState(boolean previousButtonState) {
    try {
      model.getCurrentNavigationState().PREVIOUS.setEnabled(previousButtonState);
    } catch (Exception ignored) {}
  }

  @Override
  protected void setDialogButtonsState(boolean buttonsState) {
    setFinishButtonState(buttonsState);
    setPreviousButtonState(buttonsState);
  }

  @Override
  public JComponent prepare(final WizardNavigationState state) {
    rootConfigureContainerPanel.revalidate();
    setDialogButtonsState(true);

    return rootConfigureContainerPanel;
  }

  @Override
  public WizardStep onPrevious(final AzureSelectDockerWizardModel model) {
    if (doValidate() == null) {
      return super.onPrevious(model);
    } else {
      return this;
    }
  }

  @Override
  public WizardStep onNext(final AzureSelectDockerWizardModel model) {
    if (doValidate() == null) {
      return super.onNext(model);
    } else {
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
    model.finishedOK = true;

    return super.onCancel();
  }

  @Override
  public Map<String, String> toProperties() {
    return model.toProperties();
  }
}
