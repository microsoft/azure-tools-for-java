/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Getter(AccessLevel.PROTECTED)
public class SpringCloudAppInfoAdvancedPanel extends AbstractSpringCloudAppInfoPanel {
    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private SpringCloudClusterComboBox selectorCluster;
    private ValidationDebouncedTextInput textName;
    private SpringCloudAppConfigPanel formConfig;

    public SpringCloudAppInfoAdvancedPanel(@Nullable final SpringCloudCluster cluster) {
        super(cluster);
        $$$setupUI$$$();
        this.init();
    }

    protected void onAppChanged(SpringCloudApp app) {
        this.formConfig.updateForm(app);
        super.onAppChanged(app);
    }

    @Override
    public SpringCloudAppConfig getData() {
        final SpringCloudAppConfig config = this.formConfig.getData();
        return super.getData(config);
    }

    @Override
    public void setData(final SpringCloudAppConfig config) {
        super.setData(config);
        this.formConfig.setData(config);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final List<AzureFormInput<?>> inputs = this.formConfig.getInputs();
        inputs.addAll(Arrays.asList(
                this.getSelectorSubscription(),
                this.getSelectorCluster(),
                this.getTextName()
        ));
        return inputs;
    }
}
