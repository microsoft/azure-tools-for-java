/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Getter(AccessLevel.PROTECTED)
public class SpringCloudAppInfoBasicPanel extends AbstractSpringCloudAppInfoPanel {
    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private SpringCloudClusterComboBox selectorCluster;
    private ValidationDebouncedTextInput textName;

    public SpringCloudAppInfoBasicPanel(@Nullable final SpringCloudCluster cluster) {
        super(cluster);
        $$$setupUI$$$();
        this.init();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(
                this.getSelectorSubscription(),
                this.getSelectorCluster(),
                this.getTextName()
        );
    }
}
