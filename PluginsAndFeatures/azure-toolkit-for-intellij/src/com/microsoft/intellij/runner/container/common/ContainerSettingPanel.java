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

package com.microsoft.intellij.runner.container.common;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;

import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
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

    private static final String SELECT_REGISTRY = "<Select Container Registry>";
    private static final String LOADING = "<Loading...>";

    /**
     * Constructor.
     */
    public ContainerSettingPanel() {
        presenter = new ContainerSettingPresenter<>();
        presenter.onAttachView(this);

        dockerFilePathTextField.addActionListener(e -> {
            String path = dockerFilePathTextField.getText();
            final VirtualFile file = FileChooser.chooseFile(
                    new FileChooserDescriptor(
                            true /*chooseFiles*/,
                            false /*chooseFolders*/,
                            false /*chooseJars*/,
                            false /*chooseJarsAsFiles*/,
                            false /*chooseJarContents*/,
                            false /*chooseMultiple*/
                    ),
                    null,
                    Utils.isEmptyString(path) ? null : LocalFileSystem.getInstance().findFileByPath(path)
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
                if (e.getItem() instanceof Registry) {
                    Registry registry = (Registry) e.getItem();
                    disableWidgets();
                    presenter.onGetRegistryCredential(registry);
                }
            }
        });

        cbContainerRegistry.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList list, Object object, int
                    index, boolean isSelected, boolean cellHasFocus) {
                if (object != null) {
                    if (object instanceof Registry) {
                        setText(((Registry)object).name());
                    } else {
                        setText(object.toString());
                    }
                }
            }
        });

        cbContainerRegistry.addItem(LOADING);
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

    public void setServerUrl(String url) {
        txtServerUrl.setText(url);
    }

    public void setUserName(String userName) {
        txtUserName.setText(userName);
    }

    public void setPasswordField(String password) {
        passwordField.setText(password);
    }

    public void setImageTag(String imageTag) {
        txtImageTag.setText(imageTag);
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
    public void onListRegistries() {
        presenter.onListRegistries();
    }

    @Override
    public void listRegistries(@NotNull final List<Registry> registries) {
        DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) cbContainerRegistry.getModel();
        model.removeAllElements();
        model.addElement(SELECT_REGISTRY);
        for (Registry registry : registries) {
            model.addElement(registry);
        }
    }

    @Override
    public void fillCredential(@NotNull final PrivateRegistryImageSetting setting) {
        txtServerUrl.setText(setting.getServerUrl());
        txtUserName.setText(setting.getUsername());
        passwordField.setText(setting.getPassword());
        txtImageTag.setText(setting.getServerUrl() + "/");
        txtImageTag.requestFocus();
    }

    @Override
    public void disposeEditor() {
        presenter.onDetachView();
    }
}
