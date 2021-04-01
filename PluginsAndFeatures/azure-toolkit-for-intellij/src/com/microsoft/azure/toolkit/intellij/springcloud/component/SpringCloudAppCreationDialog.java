/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.AzureValidationInfoBuilder;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.util.ValidationUtils;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SpringCloudAppCreationDialog extends AzureDialog<SpringCloudAppEntity>
    implements AzureForm<SpringCloudAppEntity> {
    private final SpringCloudCluster cluster;
    private JPanel contentPanel;
    private ValidationDebouncedTextInput textName;

    public SpringCloudAppCreationDialog(final SpringCloudCluster cluster) {
        super();
        this.init();
        this.cluster = cluster;
        this.textName.setValidator(this::validateName);
        this.pack();
    }

    private AzureValidationInfo validateName() {
        try {
            ValidationUtils.validateSpringCloudAppName(this.textName.getValue(), this.cluster);
        } catch (final IllegalArgumentException e) {
            final AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this.textName).type(AzureValidationInfo.Type.ERROR).message(e.getMessage()).build();
        }
        return AzureValidationInfo.OK;
    }

    @Override
    public AzureForm<SpringCloudAppEntity> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return message("springCloud.app.create.title");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public SpringCloudAppEntity getData() {
        return SpringCloudAppEntity.fromName(this.textName.getValue(), this.cluster.entity());
    }

    @Override
    public void setData(final SpringCloudAppEntity data) {
        this.textName.setValue(data.getName());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }
}
