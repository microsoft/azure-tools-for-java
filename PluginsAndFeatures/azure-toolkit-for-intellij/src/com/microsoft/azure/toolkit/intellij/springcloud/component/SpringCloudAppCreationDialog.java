/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;

import javax.annotation.Nullable;
import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SpringCloudAppCreationDialog extends ConfigDialog<SpringCloudAppConfig> {
    private final SpringCloudCluster cluster;
    private JPanel panel;
    private SpringCloudAppInfoBasicPanel basicForm;
    private SpringCloudAppInfoAdvancedPanel advancedForm;

    public SpringCloudAppCreationDialog(@Nullable SpringCloudCluster cluster) {
        super(null);
        this.init();
        this.cluster = cluster;
        setFrontPanel(basicForm);
    }

    @Override
    protected AzureFormPanel<SpringCloudAppConfig> getAdvancedFormPanel() {
        return advancedForm;
    }

    @Override
    protected AzureFormPanel<SpringCloudAppConfig> getBasicFormPanel() {
        return basicForm;
    }

    @Override
    protected String getDialogTitle() {
        return message("springCloud.app.create.title");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.panel;
    }

    private void createUIComponents() {
        advancedForm = new SpringCloudAppInfoAdvancedPanel(this.cluster);
        basicForm = new SpringCloudAppInfoBasicPanel(this.cluster);
    }
}
