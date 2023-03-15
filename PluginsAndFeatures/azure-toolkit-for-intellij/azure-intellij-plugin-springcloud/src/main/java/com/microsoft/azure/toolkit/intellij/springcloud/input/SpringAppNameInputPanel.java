/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.input;

import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import lombok.Setter;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

import static com.microsoft.azure.toolkit.intellij.springcloud.creation.AbstractSpringCloudAppInfoPanel.validateSpringCloudAppName;

public class SpringAppNameInputPanel implements AzureFormJPanel<String> {
    private JPanel rootPanel;
    @Setter
    private AzureTextInput txtAppName;
    @Setter
    private SpringCloudCluster cluster;

    public SpringAppNameInputPanel() {
        $$$setupUI$$$();
        this.txtAppName.setRequired(true);
        this.txtAppName.setLabel("App Name");
        this.txtAppName.addValidator(() -> {
            try {
                validateSpringCloudAppName(txtAppName.getValue(), this.cluster);
            } catch (final IllegalArgumentException e) {
                final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(txtAppName).type(AzureValidationInfo.Type.ERROR).message(e.getMessage()).build();
            }
            return AzureValidationInfo.success(txtAppName);
        });
    }

    @Override
    public String getValue() {
        return txtAppName.getValue();
    }

    public void setValue(final String value) {
        this.txtAppName.setValue(value);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(txtAppName);
    }

    @Override
    public JPanel getContentPanel() {
        return rootPanel;
    }
}
