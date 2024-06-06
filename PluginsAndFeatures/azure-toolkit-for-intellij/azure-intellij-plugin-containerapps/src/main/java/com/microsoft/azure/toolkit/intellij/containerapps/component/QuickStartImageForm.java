/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class QuickStartImageForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig>, DeploymentSourceForm {
    public static final String QUICK_START_IMAGE = "mcr.microsoft.com/azuredocs/containerapps-helloworld:latest";
    @Getter
    private JPanel contentPanel;
    private AzureTextInput txtImage;
    private JLabel lblImage;

    @Setter
    @Getter
    private ContainerApp containerApp;

    public QuickStartImageForm() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.txtImage.setLabel("Image");
        this.txtImage.setRequired(true);
        this.txtImage.setEnabled(false);
        this.txtImage.addValidator(() -> {
            final String value = this.txtImage.getValue();
            if (StringUtils.isBlank(value)) {
                return AzureValidationInfo.error("Image name is required.", this.txtImage);
            }
            return AzureValidationInfo.ok(this.txtImage);
        });
        this.txtImage.setValue(QUICK_START_IMAGE);
        this.lblImage.setLabelFor(txtImage);
    }

    @Override
    public ContainerAppDraft.ImageConfig getValue() {
        final String fullImageName = this.txtImage.getValue();
        return new ContainerAppDraft.ImageConfig(fullImageName);
    }

    @Override
    public void setValue(final ContainerAppDraft.ImageConfig config) {
        this.txtImage.setValue(config.getFullImageName());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.txtImage);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
    }
}
