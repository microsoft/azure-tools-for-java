/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.connection;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.keyvault.AzureKeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class KeyVaultResourcePanel implements AzureFormJPanel<Resource<KeyVault>> {
    protected SubscriptionComboBox subscriptionComboBox;
    protected AzureComboBox<KeyVault> vaultComboBox;
    @Getter
    protected JPanel contentPanel;

    public KeyVaultResourcePanel() {
        this.init();
    }

    private void init() {
        this.vaultComboBox.setRequired(true);
        this.subscriptionComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.vaultComboBox.reloadItems();
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.vaultComboBox.clear();
            }
        });
    }

    @Override
    public void setValue(Resource<KeyVault> accountDef) {
        final KeyVault account = accountDef.getData();
        Optional.ofNullable(account).ifPresent((a -> {
            this.subscriptionComboBox.setValue(a.getSubscription());
            this.vaultComboBox.setValue(a);
        }));
    }

    @Nullable
    @Override
    public Resource<KeyVault> getValue() {
        final KeyVault cache = this.vaultComboBox.getValue();
        final AzureValidationInfo info = this.getValidationInfo(true);
        if (!info.isValid()) {
            return null;
        }
        return KeyVaultResourceDefinition.INSTANCE.define(cache);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(
            this.vaultComboBox,
            this.subscriptionComboBox
        );
    }

    protected void createUIComponents() {
        final Supplier<List<? extends KeyVault>> loader = () -> Optional
                .ofNullable(this.subscriptionComboBox)
                .map(AzureComboBox::getValue)
                .map(Subscription::getId)
                .map(id -> Azure.az(AzureKeyVault.class).keyVaults(id).list())
                .orElse(Collections.emptyList());
        this.vaultComboBox = new AzureComboBox<>(loader) {

            @Nullable
            @Override
            protected KeyVault doGetDefaultValue() {
                return CacheManager.getUsageHistory(KeyVault.class).peek(v -> Objects.isNull(subscriptionComboBox) || Objects.equals(subscriptionComboBox.getValue(), v.getSubscription()));
            }

            @Override
            protected String getItemText(Object item) {
                return Optional.ofNullable(item).map(i -> ((KeyVault) i).getName()).orElse(StringUtils.EMPTY);
            }

            @Override
            protected void refreshItems() {
                Optional.ofNullable(KeyVaultResourcePanel.this.subscriptionComboBox)
                    .map(AzureComboBox::getValue)
                    .map(Subscription::getId)
                    .ifPresent(id -> Azure.az(AzureKeyVault.class).keyVaults(id).refresh());
                super.refreshItems();
            }
        };
    }
}
