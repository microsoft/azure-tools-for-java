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

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerImageInstance;
import com.microsoft.azure.docker.model.KnownDockerImages;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureWizardPage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class AzureConfigureDockerContainerStep extends AzureWizardPage {
    private static final Logger log =  Logger.getLogger(AzureConfigureDockerContainerStep.class.getName());

    private Text dockerContainerNameTextField;
    private Button customDockerfileRadioButton;
    private Combo dockerfileComboBox;
    private Button predefinedDockerfileRadioButton;
    private Text customDockerfileTextField;
    private Button customDockerfileBrowseButton;
    private Text dockerContainerPortSettings;

    private ManagedForm managedForm;
    private ScrolledForm errMsgForm;
    private IMessageManager errDispatcher;

    private IProject project;
    private AzureDockerHostsManager dockerManager;
    private AzureDockerImageInstance dockerImageDescription;
    private AzureSelectDockerWizard wizard;
    private String hostAppPortSettings;
    private String hostDebugPortSettings;

    /**
     * Create the wizard.
     */
    public AzureConfigureDockerContainerStep(AzureSelectDockerWizard wizard) {
        super("Deploying Docker Container on Azure", "", Activator.getImageDescriptor("icons/large/DeploytoAzureWizard.png"));

        this.wizard = wizard;
        this.dockerManager = wizard.getDockerManager();
        this.dockerImageDescription = wizard.getDockerImageInstance();
        this.project = wizard.getProject();

        int randomPort = new Random().nextInt(10000); // default to host port 2xxxx and 5xxxx (debug)
        hostAppPortSettings = String.format("%d", 20000 + randomPort);
        hostDebugPortSettings = String.format("%d", 50000 + randomPort);

        setTitle("Configure the Docker container to be created");
        setDescription("");
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite mainContainer = new Composite(parent, SWT.NULL);

        setControl(mainContainer);
        mainContainer.setLayout(new GridLayout(3, false));

        Label lblDockerContainerName = new Label(mainContainer, SWT.NONE);
        GridData gd_lblDockerContainerName = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblDockerContainerName.horizontalIndent = 1;
        lblDockerContainerName.setLayoutData(gd_lblDockerContainerName);
        lblDockerContainerName.setText("Container name:");

        dockerContainerNameTextField = new Text(mainContainer, SWT.BORDER);
        dockerContainerNameTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(mainContainer, SWT.NONE);

        Label lblNewLabel = new Label(mainContainer, SWT.NONE);
        GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblNewLabel.horizontalIndent = 1;
        lblNewLabel.setLayoutData(gd_lblNewLabel);
        lblNewLabel.setText("Dockerfile settings");

        Label lblDockerfileSettings = new Label(mainContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
        lblDockerfileSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        predefinedDockerfileRadioButton = new Button(mainContainer, SWT.RADIO);
        GridData gd_predefinedDockerfileRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_predefinedDockerfileRadioButton.horizontalIndent = 3;
        predefinedDockerfileRadioButton.setLayoutData(gd_predefinedDockerfileRadioButton);
        predefinedDockerfileRadioButton.setText("Predefined Docker image");
        new Label(mainContainer, SWT.NONE);

        dockerfileComboBox = new Combo(mainContainer, SWT.NONE);
        GridData gd_dockerfileComboBox = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
        gd_dockerfileComboBox.horizontalIndent = 22;
        dockerfileComboBox.setLayoutData(gd_dockerfileComboBox);
        new Label(mainContainer, SWT.NONE);

        customDockerfileRadioButton = new Button(mainContainer, SWT.RADIO);
        GridData gd_customDockerfileRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_customDockerfileRadioButton.horizontalIndent = 3;
        customDockerfileRadioButton.setLayoutData(gd_customDockerfileRadioButton);
        customDockerfileRadioButton.setText("Custom Dockerfile");
        new Label(mainContainer, SWT.NONE);

        customDockerfileTextField = new Text(mainContainer, SWT.BORDER);
        GridData gd_customDockerfileTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
        gd_customDockerfileTextField.horizontalIndent = 22;
        customDockerfileTextField.setLayoutData(gd_customDockerfileTextField);

        customDockerfileBrowseButton = new Button(mainContainer, SWT.NONE);
        customDockerfileBrowseButton.setText("Browse...");

        Label label = new Label(mainContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        Label lblPortSettings = new Label(mainContainer, SWT.NONE);
        GridData gd_lblPortSettings = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblPortSettings.horizontalIndent = 1;
        lblPortSettings.setLayoutData(gd_lblPortSettings);
        lblPortSettings.setText("Port settings:");

        dockerContainerPortSettings = new Text(mainContainer, SWT.BORDER);
        dockerContainerPortSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(mainContainer, SWT.NONE);

        Label lblForExampletcp = new Label(mainContainer, SWT.NONE);
        GridData gd_lblForExampletcp = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblForExampletcp.horizontalIndent = 22;
        lblForExampletcp.setLayoutData(gd_lblForExampletcp);
        lblForExampletcp.setText("For example \"10022:22\"");
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

        dockerContainerNameTextField.setText(dockerImageDescription.dockerContainerName);
        dockerContainerNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerContainerNameTip());
        dockerContainerNameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerContainerName(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerContainerNameTextField", dockerContainerNameTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerContainerNameTextField", AzureDockerValidationUtils.getDockerContainerNameTip(), null, IMessageProvider.ERROR, dockerContainerNameTextField);
                    setErrorMessage("Invalid Docker container name");
                    setPageComplete(false);
                }
            }
        });

        predefinedDockerfileRadioButton.setSelection(true);
        predefinedDockerfileRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerfileComboBox.setEnabled(true);
                customDockerfileTextField.setEnabled(false);
                customDockerfileBrowseButton.setEnabled(false);
                errDispatcher.removeMessage("customDockerfileTextField", customDockerfileTextField);
                setErrorMessage(null);
                setPageComplete(doValidate());
            }
        });

        int idx = 0;
        boolean selected = false;
        for (KnownDockerImages predefinedImage : dockerManager.getDefaultDockerImages()) {
            // Add predefined images that can run .WAR as default
            if (!predefinedImage.isCanRunJarFile()) {
                dockerfileComboBox.add(predefinedImage.getName());
                dockerfileComboBox.setData(predefinedImage.getName(), predefinedImage);
                if (dockerImageDescription.predefinedDockerfile != null && dockerImageDescription.predefinedDockerfile.equals(predefinedImage)) {
                    dockerfileComboBox.select(idx);
                    selected = true;
                }
                idx ++;
            }
        }
        if (!selected) {
            dockerfileComboBox.select(0);
        }
        dockerfileComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerContainerPortSettings.setText(getDefaultPortMapping((KnownDockerImages) dockerfileComboBox.getData(dockerfileComboBox.getText())));
                setPageComplete(doValidate());
            }
        });

        customDockerfileRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerfileComboBox.setEnabled(false);
                customDockerfileTextField.setEnabled(true);
                customDockerfileBrowseButton.setEnabled(true);
                setPageComplete(doValidate());
            }
        });

        customDockerfileTextField.setToolTipText(AzureDockerValidationUtils.getDockerfilePathTip());
        customDockerfileTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerfilePath(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("customDockerfileTextField", customDockerfileTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("customDockerfileTextField", AzureDockerValidationUtils.getDockerfilePathTip(), null, IMessageProvider.ERROR, customDockerfileTextField);
                    setErrorMessage("Invalid Dockerfile location");
                    setPageComplete(false);
                }
            }
        });
        customDockerfileBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(customDockerfileBrowseButton.getShell(), SWT.OPEN);
                fileDialog.setText("Select Custom Dockerfile");
                fileDialog.setFilterPath(System.getProperty("user.home"));
                fileDialog.setFilterExtensions(new String[] { "Dockerfile", "*.*" });
                String path = fileDialog.open();
                if (path == null) {
                    return;
                }
                customDockerfileTextField.setText(path);
                setPageComplete(doValidate());
            }
        });

        dockerContainerPortSettings.setText(getDefaultPortMapping((KnownDockerImages) dockerfileComboBox.getData(dockerfileComboBox.getText())));
        dockerContainerPortSettings.setToolTipText(AzureDockerValidationUtils.getDockerPortSettingsTip());
        dockerContainerPortSettings.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerPortSettings(((Text) event.getSource()).getText()) != null) {
                    errDispatcher.removeMessage("dockerContainerPortSettings", dockerContainerPortSettings);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerContainerPortSettings", AzureDockerValidationUtils.getDockerPortSettingsTip(), null, IMessageProvider.ERROR, dockerContainerPortSettings);
                    setErrorMessage("Invalid Docker container port settings");
                    setPageComplete(false);
                }
            }
        });
    }

    public boolean doValidate() {
        if (dockerContainerNameTextField.getText() == null || dockerContainerNameTextField.getText().equals("") ||
                !AzureDockerValidationUtils.validateDockerContainerName(dockerContainerNameTextField.getText())) {
            errDispatcher.addMessage("dockerContainerNameTextField", AzureDockerValidationUtils.getDockerContainerNameTip(), null, IMessageProvider.ERROR, dockerContainerNameTextField);
            setErrorMessage("Invalid Docker container name");
            return false;
        } else {
            errDispatcher.removeMessage("dockerContainerNameTextField", dockerContainerNameTextField);
            setErrorMessage(null);
            dockerImageDescription.dockerContainerName = dockerContainerNameTextField.getText();
        }

        if (predefinedDockerfileRadioButton.getSelection()) {
            KnownDockerImages dockerfileImage = (KnownDockerImages) dockerfileComboBox.getData(dockerfileComboBox.getText());
            if (dockerfileImage == null) {
                errDispatcher.addMessage("dockerfileComboBox", "Missing predefined Dockerfile options", null, IMessageProvider.ERROR, dockerfileComboBox);
                setErrorMessage("Invalid Dockerfile selection");
                return false;
            } else {
                errDispatcher.removeMessage("dockerfileComboBox", dockerfileComboBox);
                setErrorMessage(null);
                dockerImageDescription.predefinedDockerfile = dockerfileImage.name();
                if (dockerImageDescription.artifactPath != null) {
                    dockerImageDescription.dockerfileContent = dockerfileImage.getDockerfileContent().replace(
                            KnownDockerImages.DOCKER_ARTIFACT_FILENAME, new File(dockerImageDescription.artifactPath).getName());
                }
            }
        }

        if (customDockerfileRadioButton.getSelection()) {
            String dockerfileName = customDockerfileTextField.getText();
            if (dockerfileName == null || dockerfileName.equals("") || Files.notExists(Paths.get(dockerfileName))) {
                errDispatcher.addMessage("customDockerfileTextField", AzureDockerValidationUtils.getDockerfilePathTip(), null, IMessageProvider.ERROR, customDockerfileTextField);
                setErrorMessage("Invalid Docker container name");
                return false;
            } else {
                try {
                    dockerImageDescription.dockerfileContent = new String(Files.readAllBytes(Paths.get(dockerfileName)));
                } catch (Exception e) {
                    errDispatcher.addMessage("customDockerfileTextField", AzureDockerValidationUtils.getDockerfilePathTip(), null, IMessageProvider.ERROR, customDockerfileTextField);
                    setErrorMessage("Error reading Dockerfile content");
                    return false;
                }
                errDispatcher.removeMessage("customDockerfileTextField", customDockerfileTextField);
                setErrorMessage(null);
                dockerImageDescription.predefinedDockerfile = null;
            }
        }

        if (dockerContainerPortSettings.getText() == null || dockerContainerPortSettings.getText().equals("") ||
                AzureDockerValidationUtils.validateDockerPortSettings(dockerContainerPortSettings.getText()) == null) {
            errDispatcher.addMessage("dockerContainerPortSettings", AzureDockerValidationUtils.getDockerPortSettingsTip(), null, IMessageProvider.ERROR, dockerContainerPortSettings);
            setErrorMessage("Invalid Docker container port settings");
            return false;
        } else {
            errDispatcher.removeMessage("dockerContainerPortSettings", dockerContainerPortSettings);
            setErrorMessage(null);
            dockerImageDescription.dockerPortSettings = dockerContainerPortSettings.getText();
        }

        return true;
    }

    private String getDefaultPortMapping(KnownDockerImages knownImage) {
        if (knownImage == null) {
            return String.format("%s:80", hostAppPortSettings);
        }
        if (knownImage.getDebugPortSettings() != null && !knownImage.getDebugPortSettings().isEmpty()) {
            // TODO: add JVM port mapping
            return String.format("%s:%s %s:%s", hostAppPortSettings, knownImage.getPortSettings(), hostDebugPortSettings,
                    knownImage.getDebugPortSettings());
        } else {
            return String.format("%s:%s", hostAppPortSettings, knownImage.getPortSettings());
        }
    }

    public void setPredefinedDockerfileOptions(String artifactFileName) {
        boolean isJarFile = artifactFileName.toLowerCase().matches(".*.jar");
        dockerfileComboBox.removeAll();
        int idx = 0;
        boolean selected = false;
        for (KnownDockerImages predefinedImage : dockerManager.getDefaultDockerImages()) {
            if (predefinedImage.isCanRunJarFile() == isJarFile) {
                dockerfileComboBox.add(predefinedImage.getName());
                dockerfileComboBox.setData(predefinedImage.getName(), predefinedImage);
                if (dockerImageDescription.predefinedDockerfile != null && dockerImageDescription.predefinedDockerfile.equals(predefinedImage)) {
                    dockerfileComboBox.select(idx);
                    selected = true;
                }
                idx ++;
            }
        }
        if (!selected) {
            dockerfileComboBox.select(0);
        }
        dockerContainerPortSettings.setText(getDefaultPortMapping((KnownDockerImages) dockerfileComboBox.getData(dockerfileComboBox.getText())));
    }

    public void setDockerContainerName(String dockerContainerName) {
        if (dockerContainerName != null) {
            dockerContainerNameTextField.setText(dockerContainerName);
        }
    }

}
