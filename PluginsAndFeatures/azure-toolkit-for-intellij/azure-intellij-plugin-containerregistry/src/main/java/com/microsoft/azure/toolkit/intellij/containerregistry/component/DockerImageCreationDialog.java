/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DockerImageCreationDialog extends AzureDialog<DockerImage>
        implements AzureForm<DockerImage> {
    private JLabel lblDockerFile;
    private JLabel lblImageName;
    private JLabel lblTagName;
    private AzureFileInput txtDockerFile;
    private AzureTextInput txtImageName;
    private AzureTextInput txtTagName;
    private AzureArtifactComboBox cbAzureArtifact;
    private JPanel pnlRoot;
    private AzureFileInput txtBaseDirectory;

    private Project project;

    public DockerImageCreationDialog(Project project) {
        super(project);
        init();
    }

    @Override
    protected void init() {
        super.init();
        txtDockerFile.addValueChangedListener(path -> {
            final File file = new File(path);
            if (file.exists() && file.isFile()) {
                txtBaseDirectory.setValue(file.getParentFile().getAbsolutePath());
            }
        });

        final FileChooserDescriptor dockerDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        txtDockerFile.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>("Select Docker File", "Select Docker File",
                txtDockerFile, project, dockerDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));

        final FileChooserDescriptor baseDirectory = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        txtBaseDirectory.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>("Select Base Directory", "Select base directory for docker build",
                txtDockerFile, project, baseDirectory, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
    }

    @Override
    public AzureForm<DockerImage> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "New Docker Image";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public DockerImage getValue() {
        return DockerImage.builder()
                .repositoryName(txtImageName.getValue())
                .tagName(txtTagName.getValue())
                .azureArtifact(cbAzureArtifact.getValue())
                .dockerFile(new File(txtDockerFile.getValue()))
                .baseDirectory(new File(txtBaseDirectory.getValue()))
                .isDraft(true)
                .build();
    }

    @Override
    public void setValue(@Nonnull final DockerImage data) {
        Optional.ofNullable(data.getRepositoryName()).ifPresent(file -> txtImageName.setValue(file));
        Optional.ofNullable(data.getTagName()).ifPresent(file -> txtTagName.setValue(file));
        Optional.ofNullable(data.getDockerFile()).ifPresent(file -> txtDockerFile.setValue(file.getAbsolutePath()));
        Optional.ofNullable(data.getBaseDirectory()).ifPresent(file -> txtBaseDirectory.setValue(file.getAbsolutePath()));
        Optional.ofNullable(data.getAzureArtifact()).ifPresent(file -> cbAzureArtifact.setValue(file));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtDockerFile, txtImageName, txtTagName, txtBaseDirectory);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbAzureArtifact = new AzureArtifactComboBox(project);
    }
}
