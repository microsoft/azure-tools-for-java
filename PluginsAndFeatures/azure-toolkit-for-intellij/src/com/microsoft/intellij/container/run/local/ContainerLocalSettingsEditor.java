package com.microsoft.intellij.container.run.local;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class ContainerLocalSettingsEditor extends SettingsEditor<ContainerLocalRunConfiguration> {

    private JPanel rootPanel;
    private JTextField textDockerHost;
    private JCheckBox comboTlsEnabled;
    private JTextField textImageName;
    private JTextField textTagName;
    private TextFieldWithBrowseButton dockerCertPathTextField;

    @Override
    protected void resetEditorFrom(@NotNull ContainerLocalRunConfiguration containerLocalRunConfiguration) {
        ContainerLocalRunModel model = containerLocalRunConfiguration.getContainerLocalRunModel();
        textDockerHost.setText(model.getDockerHost());
        comboTlsEnabled.setSelected(model.isTlsEnabled());
        dockerCertPathTextField.setText(model.getDockerCertPath());
        textImageName.setText(model.getImageName());
        textTagName.setText(model.getTagName());
        updateComponentEnabledState();
    }


    private void updateComponentEnabledState() {
        dockerCertPathTextField.setEnabled(comboTlsEnabled.isSelected());
    }

    @Override
    protected void applyEditorTo(@NotNull ContainerLocalRunConfiguration containerLocalRunConfiguration) throws ConfigurationException {
        ContainerLocalRunModel model = containerLocalRunConfiguration.getContainerLocalRunModel();
        model.setDockerCertPath(textDockerHost.getText());
        model.setTlsEnabled(comboTlsEnabled.isSelected());
        model.setDockerCertPath(dockerCertPathTextField.getText());
        model.setImageName(textImageName.getText());
        model.setTagName(textTagName.getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        dockerCertPathTextField.addActionListener(event -> onDockerCertPathBrowseButtonClick(event));
        comboTlsEnabled.addActionListener(event -> updateComponentEnabledState());

        return rootPanel;
    }

    private void onDockerCertPathBrowseButtonClick(ActionEvent event) {
        String path = dockerCertPathTextField.getText();
        final VirtualFile[] files = FileChooser.chooseFiles(
                new FileChooserDescriptor(false, true, true, false, false, false),
                dockerCertPathTextField,
                null,
                path != null && !path.isEmpty() ? LocalFileSystem.getInstance().findFileByPath(path) : null);
        if (files.length > 0) {
            final StringBuilder builder = new StringBuilder();
            for (VirtualFile file : files) {
                if (builder.length() > 0) {
                    builder.append(File.pathSeparator);
                }
                builder.append(FileUtil.toSystemDependentName(file.getPath()));
            }
            path = builder.toString();
            dockerCertPathTextField.setText(path);
        }

    }

}
