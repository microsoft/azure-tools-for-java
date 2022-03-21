/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerSettingPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerSettingView;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.util.List;

public class ContainerSettingPanel implements ContainerSettingView {

    private final ContainerSettingPresenter<ContainerSettingPanel> presenter;

    private JTextField txtServerUrl;
    private JTextField txtUserName;
    private JPasswordField passwordField;
    private JTextField txtImageTag;
    private TextFieldWithBrowseButton dockerFilePathTextField;
    private JComboBox<Object> cbContainerRegistry;
    private JPanel pnlRoot;
    private JTextField txtStartupFile;
    private JLabel lblStartupFile;
    private JLabel lblServerUrl;

    private static final String SELECT_REGISTRY = "<Select Container Registry>";
    private static final String LOADING = "<Loading...>";

    private final Project project;

    /**
     * Constructor.
     */
    public ContainerSettingPanel(Project project) {
        this.project = project;
        presenter = new ContainerSettingPresenter<>();
        presenter.onAttachView(this);

        dockerFilePathTextField.addActionListener(e -> {
            final String path = dockerFilePathTextField.getText();
            final VirtualFile file = FileChooser.chooseFile(
                    new FileChooserDescriptor(
                            true /*chooseFiles*/,
                            false /*chooseFolders*/,
                            false /*chooseJars*/,
                            false /*chooseJarsAsFiles*/,
                            false /*chooseJarContents*/,
                            false /*chooseMultiple*/
                    ),
                    this.project,
                    StringUtils.isEmpty(path) ? null : LocalFileSystem.getInstance().findFileByPath(path)
            );
            if (file != null) {
                dockerFilePathTextField.setText(file.getPath());
            }
        });

        cbContainerRegistry.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (e.getItem() instanceof String) {
                    enableWidgets();
                    return;
                }
                if (e.getItem() instanceof ContainerRegistry) {
                    final ContainerRegistry registry = (ContainerRegistry) e.getItem();
                    disableWidgets();
                    presenter.onGetRegistryCredential(registry);
                }
            }
        });

        cbContainerRegistry.setRenderer(new ListCellRendererWrapper<>() {
            @Override
            public void customize(JList list, Object object, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (object != null) {
                    if (object instanceof ContainerRegistry) {
                        setText(((ContainerRegistry) object).getName());
                    } else {
                        setText(object.toString());
                    }
                }
            }
        });

        cbContainerRegistry.addItem(LOADING);

        txtServerUrl.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                lblServerUrl.setText(txtServerUrl.getText() + "/");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                lblServerUrl.setText(txtServerUrl.getText() + "/");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                lblServerUrl.setText(txtServerUrl.getText() + "/");
            }
        });
    }

    public String getServerUrl() {
        return txtServerUrl.getText();
    }

    public String getUserName() {
        return txtUserName.getText();
    }

    public String getPassword() {
        return String.valueOf(passwordField.getPassword());
    }

    public String getImageTag() {
        return txtImageTag.getText();
    }

    public String getDockerPath() {
        return dockerFilePathTextField.getText();
    }

    public String getStartupFile() {
        return txtStartupFile.getText();
    }

    public void setTxtFields(PrivateRegistryImageSetting acrInfo) {
        txtServerUrl.setText(acrInfo.getServerUrl());
        txtUserName.setText(acrInfo.getUsername());
        passwordField.setText(acrInfo.getPassword());
        txtImageTag.setText(acrInfo.getImageNameWithTag());
        txtStartupFile.setText(acrInfo.getStartupFile());
    }

    public void setDockerPath(String path) {
        dockerFilePathTextField.setText(path);
    }

    private void disableWidgets() {
        txtServerUrl.setEnabled(false);
        txtUserName.setEnabled(false);
        passwordField.setEnabled(false);
    }

    private void enableWidgets() {
        txtServerUrl.setEnabled(true);
        txtUserName.setEnabled(true);
        passwordField.setEnabled(true);
    }

    @Override
    public void setStartupFileVisible(boolean visible) {
        lblStartupFile.setVisible(visible);
        txtStartupFile.setVisible(visible);
    }

    @Override
    public void onListRegistries() {
        presenter.onListRegistries();
    }

    @Override
    public void listRegistries(@NotNull final List<ContainerRegistry> registries) {
        final DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) cbContainerRegistry.getModel();
        model.removeAllElements();
        model.addElement(SELECT_REGISTRY);
        for (final ContainerRegistry registry : registries) {
            model.addElement(registry);
        }
    }

    @Override
    public void fillCredential(@NotNull final PrivateRegistryImageSetting setting) {
        txtServerUrl.setText(setting.getServerUrl());
        txtUserName.setText(setting.getUsername());
        passwordField.setText(setting.getPassword());
        txtImageTag.setText(setting.getImageNameWithTag());
    }

    @Override
    public void disposeEditor() {
        presenter.onDetachView();
    }
}
