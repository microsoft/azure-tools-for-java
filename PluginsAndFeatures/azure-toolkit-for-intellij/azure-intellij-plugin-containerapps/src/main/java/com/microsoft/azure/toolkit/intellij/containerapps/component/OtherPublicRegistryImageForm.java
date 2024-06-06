/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.icons.AllIcons;
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
import java.util.regex.Pattern;

public class OtherPublicRegistryImageForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig>, DeploymentSourceForm {
    @Getter
    private JPanel contentPanel;
    private AzureTextInput txtImage;
    private JLabel lblImage;

    final Pattern dockerHubImage = Pattern.compile("^[\\w.\\-_]+(?::\\d+)?/[a-z0-9._\\-]+/[a-z0-9._\\-]+(:(?<tag>[\\w.\\-_]{1,127})|)$");
    @Setter
    @Getter
    private ContainerApp containerApp;

    public OtherPublicRegistryImageForm() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.txtImage.setLabel("Image");
        this.txtImage.setRequired(true);
        this.txtImage.addValidator(() -> {
            final String value = this.txtImage.getValue();
            if (StringUtils.isBlank(value)) {
                return AzureValidationInfo.error("Image name is required.", this.txtImage);
            } else if (!value.startsWith("default/") && !dockerHubImage.matcher(value).matches()) {
                return AzureValidationInfo.error("Should be in format of 'host[:port]/namespace/repository:tag', e.g. 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest'.", this.txtImage);
            }
            return AzureValidationInfo.ok(this.txtImage);
        });
        this.lblImage.setLabelFor(txtImage);
        this.lblImage.setIcon(AllIcons.General.ContextHelp);
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
