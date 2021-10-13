/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBoxSimple;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class StorageAccountResourcePanel implements AzureFormJPanel<StorageAccount> {
    protected SubscriptionComboBox subscriptionComboBox;
    protected AzureComboBox<StorageAccount> accountComboBox;
    @Getter
    protected JPanel contentPanel;

    public StorageAccountResourcePanel() {
        this.init();
    }

    private void init() {
        this.subscriptionComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.accountComboBox.refreshItems();
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.accountComboBox.clear();
            }
        });
    }

    @Override
    public void setData(StorageAccount account) {
        Optional.ofNullable(account).ifPresent((a -> {
            this.subscriptionComboBox.setValue(new ItemReference<>(a.subscriptionId(), Subscription::getId));
            this.accountComboBox.setValue(new ItemReference<>(a.name(), StorageAccount::name));
        }));
    }

    @Override
    public StorageAccount getData() {
        return this.accountComboBox.getValue();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(
                this.subscriptionComboBox,
                this.accountComboBox
        );
    }

    protected void createUIComponents() {
        final Supplier<List<? extends StorageAccount>> loader = () -> Optional
                .ofNullable(this.subscriptionComboBox)
                .map(AzureComboBox::getValue)
                .map(Subscription::getId)
                .map(id -> Azure.az(AzureStorageAccount.class).list(id))
                .orElse(Collections.emptyList());
        this.accountComboBox = new AzureComboBoxSimple<>(loader) {
            @Override
            protected String getItemText(Object item) {
                return Optional.ofNullable(item).map(i -> ((StorageAccount) i).name()).orElse(StringUtils.EMPTY);
            }
        };
    }
}
