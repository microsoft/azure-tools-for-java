/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.storage.component.AccountNameTextField;
import com.microsoft.azure.toolkit.intellij.storage.component.KindComboBox;
import com.microsoft.azure.toolkit.intellij.storage.component.PerformanceComboBox;
import com.microsoft.azure.toolkit.intellij.storage.component.RedundancyComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BaseStorageAccountCreationDialog extends AzureDialog<StorageAccountConfig> implements AzureForm<StorageAccountConfig> {

    private static final String DIALOG_TITLE = "Create Storage Account";

    private JPanel rootPanel;
    private SubscriptionComboBox subscriptionComboBox;
    private ResourceGroupComboBox resourceGroupComboBox;
    private AccountNameTextField accountNameTextField;
    private RegionComboBox regionComboBox;
    protected PerformanceComboBox performanceComboBox;
    protected KindComboBox kindComboBox;
    protected RedundancyComboBox redundancyComboBox;
    protected JLabel kindLabel;

    private final StorageAccountConfig config;
    private final Project project;

    public BaseStorageAccountCreationDialog(Project project) {
        super();
        this.project = project;
        this.config = StorageAccountConfig.builder().build();
        this.subscriptionComboBox.setPreferredSize(new Dimension(340, this.subscriptionComboBox.getPreferredSize().height));
        init();
        initListeners();
        setValue(config);
    }

    @Override
    protected void init() {
        super.init();
        this.subscriptionComboBox.setRequired(true);
        this.resourceGroupComboBox.setRequired(true);
        this.accountNameTextField.setRequired(true);
        this.regionComboBox.setRequired(true);
        this.performanceComboBox.setRequired(true);
    }

    protected void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.performanceComboBox.addItemListener(this::onPerformanceChanged);
        this.kindComboBox.addItemListener(this::onKindChanged);
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.resourceGroupComboBox.setSubscription(subscription);
            this.accountNameTextField.setSubscriptionId(subscription.getId());
            this.regionComboBox.setSubscription(subscription);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.resourceGroupComboBox.setSubscription(null);
            this.accountNameTextField.setSubscriptionId(null);
            this.regionComboBox.setSubscription(null);
        }
    }

    private void onPerformanceChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final Performance performance = (Performance) e.getItem();
            this.redundancyComboBox.setPerformance(performance);
            this.kindComboBox.setPerformance(performance);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.kindComboBox.setPerformance(null);
            this.redundancyComboBox.setPerformance(null);
        }
    }

    private void onKindChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final Kind kind = (Kind) e.getItem();
            this.redundancyComboBox.setKind(kind);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.redundancyComboBox.setKind(null);
        }
    }

    @Override
    public StorageAccountConfig getValue() {
        config.setSubscription(subscriptionComboBox.getValue());
        config.setResourceGroup(resourceGroupComboBox.getValue());
        config.setName(accountNameTextField.getText());
        config.setKind(kindComboBox.getValue());
        config.setPerformance(performanceComboBox.getValue());
        config.setRedundancy(redundancyComboBox.getValue());
        config.setRegion(regionComboBox.getValue());
        return config;
    }

    @Override
    public void setValue(StorageAccountConfig config) {
        Optional.ofNullable(config.getName()).ifPresent(e -> accountNameTextField.setValue(e));
        Optional.ofNullable(config.getSubscription()).ifPresent(e -> subscriptionComboBox.setValue(e));
        Optional.ofNullable(config.getResourceGroup()).ifPresent(e -> resourceGroupComboBox.setValue(e));
        Optional.ofNullable(config.getKind()).ifPresent(e -> kindComboBox.setValue(e));
        Optional.ofNullable(config.getPerformance()).ifPresent(e -> performanceComboBox.setValue(e));
        Optional.ofNullable(config.getRedundancy()).ifPresent(e -> redundancyComboBox.setValue(e));
        Optional.ofNullable(config.getRegion()).ifPresent(e -> regionComboBox.setValue(e));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.accountNameTextField,
            this.subscriptionComboBox,
            this.resourceGroupComboBox,
            this.kindComboBox,
            this.performanceComboBox,
            this.redundancyComboBox,
            this.regionComboBox
        };
        return Arrays.asList(inputs);
    }

    @Override
    public AzureForm<StorageAccountConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return DIALOG_TITLE;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return rootPanel;
    }

    private void createUIComponents() {
        this.regionComboBox = new RegionComboBox() {
            protected List<? extends Region> loadItems() {
                if (Objects.nonNull(this.subscription)) {
                    return Azure.az(AzureStorageAccount.class).forSubscription(subscription.getId()).listSupportedRegions();
                }
                return Collections.emptyList();
            }
        };
    }
}
