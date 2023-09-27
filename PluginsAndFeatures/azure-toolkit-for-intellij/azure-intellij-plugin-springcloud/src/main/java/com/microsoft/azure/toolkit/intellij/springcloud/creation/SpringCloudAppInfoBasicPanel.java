/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.intellij.icons.AllIcons;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import com.microsoft.azure.toolkit.lib.springcloud.model.Sku;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter(AccessLevel.PROTECTED)
public class SpringCloudAppInfoBasicPanel extends SpringCloudAppInfoPanel {
    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private SpringCloudClusterComboBox selectorCluster;
    private AzureTextInput textName;
    private JRadioButton useJava8;
    private JRadioButton useJava11;
    private JRadioButton useJava17;
    private JLabel lblRuntime;
    private TitledSeparator sectionConfiguration;
    private JLabel lblSubscription;
    @Setter
    private String defaultRuntimeVersion = SpringCloudDeploymentDraft.DEFAULT_RUNTIME_VERSION.toString();

    public SpringCloudAppInfoBasicPanel() {
        super();
        $$$setupUI$$$();
        this.init();
        this.lblSubscription.setIcon(AllIcons.General.ContextHelp);
    }

    protected void onAppChanged(SpringCloudApp app) {
        final Sku sku = app.getParent().getSku();
        if (Objects.nonNull(sku)) {
            final boolean enterprise = sku.isEnterpriseTier();
            this.useJava8.setVisible(!enterprise);
            this.useJava11.setVisible(!enterprise);
            this.useJava17.setVisible(!enterprise);
            this.lblRuntime.setVisible(!enterprise);
            this.sectionConfiguration.setVisible(!enterprise);
        }
        super.onAppChanged(app);
    }

    @Override
    @Nullable
    public SpringCloudAppDraft getValue() {
        final SpringCloudAppDraft app = super.getValue();
        if (Objects.nonNull(app) && this.useJava17.isVisible()) {
            final String javaVersion = this.useJava17.isSelected() ? RuntimeVersion.JAVA_17.toString() :
                this.useJava11.isSelected() ? RuntimeVersion.JAVA_11.toString() : RuntimeVersion.JAVA_8.toString();
            final SpringCloudDeploymentDraft deployment = app.updateOrCreateActiveDeployment();
            deployment.setRuntimeVersion(javaVersion);
        }
        return app;
    }

    @Override
    public void setValue(@Nullable final SpringCloudAppDraft app) {
        if (Objects.isNull(app)) {
            return;
        }
        super.setValue(app);
        final SpringCloudDeploymentDraft deployment = app.updateOrCreateActiveDeployment();
        final String runtime = Optional.ofNullable(deployment.getRuntimeVersion())
            .or(() -> Optional.ofNullable(this.getDefaultRuntimeVersion()))
            .orElse(SpringCloudDeploymentDraft.DEFAULT_RUNTIME_VERSION.toString());
        this.useJava17.setSelected(StringUtils.equalsIgnoreCase(runtime, RuntimeVersion.JAVA_17.toString()));
        this.useJava11.setSelected(StringUtils.equalsIgnoreCase(runtime, RuntimeVersion.JAVA_11.toString()));
        this.useJava8.setSelected(StringUtils.equalsIgnoreCase(runtime, RuntimeVersion.JAVA_8.toString()));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(
            this.getTextName(),
            this.getSelectorSubscription(),
            this.getSelectorCluster()
        );
    }
}
