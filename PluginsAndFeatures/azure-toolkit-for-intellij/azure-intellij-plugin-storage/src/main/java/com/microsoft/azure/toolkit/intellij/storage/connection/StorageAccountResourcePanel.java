/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.intellij.ui.components.JBPasswordField;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore;
import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.SignInHyperLinkLabel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.ConnectionStringStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import lombok.Getter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.function.Supplier;

import static com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition.*;

public class StorageAccountResourcePanel implements AzureFormJPanel<Resource<IStorageAccount>> {
    protected SubscriptionComboBox subscriptionComboBox;
    protected AzureComboBox<IStorageAccount> accountComboBox;
    @Getter
    protected JPanel contentPanel;
    private JPanel pnlAzure;
    private JRadioButton btnAzure;
    private JRadioButton btnLocal;
    private JRadioButton btnConnectionString;
    private JLabel lblSubScription;
    private JLabel lblEnvironment;
    private JLabel lblAccount;
    private JPanel pnlConnectionString;
    private JBPasswordField passwordConnectionString;
    private JLabel lblConnectionString;
    private SignInHyperLinkLabel signInHyperLinkLabel;
    private AzurePasswordFieldInput txtConnectionString;

    public StorageAccountResourcePanel() {
        this.init();
    }

    private void init() {
        this.txtConnectionString = new AzurePasswordFieldInput(this.passwordConnectionString);

        btnAzure.addItemListener(ignore -> onSelectEnvironment());
        btnLocal.addItemListener(ignore -> onSelectEnvironment());
        btnConnectionString.addItemListener(ignore -> onSelectEnvironment());

        this.onSelectEnvironment();

        this.subscriptionComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.accountComboBox.reloadItems();
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.accountComboBox.clear();
            }
        });

        lblSubScription.setLabelFor(subscriptionComboBox);
        lblEnvironment.setLabelFor(btnAzure);
        lblAccount.setLabelFor(accountComboBox);
        lblConnectionString.setLabelFor(txtConnectionString);
        txtConnectionString.setLabel("Connection string");
    }

    private void onSelectEnvironment() {
        pnlAzure.setVisible(btnAzure.isSelected());
        accountComboBox.setRequired(btnAzure.isSelected());
        pnlConnectionString.setVisible(btnConnectionString.isSelected());
        txtConnectionString.setRequired(btnConnectionString.isSelected());
        if (btnConnectionString.isSelected()) {
            txtConnectionString.requestFocusInWindow();
        }
        if (Objects.nonNull(accountComboBox.getValidationInfo())) {
            accountComboBox.validateValueAsync();
        }
        if (Objects.nonNull(txtConnectionString.getValidationInfo())) {
            txtConnectionString.validateValueAsync();
        }
    }

    @Override
    public void setValue(Resource<IStorageAccount> accountResource) {
        final IStorageAccount account = accountResource.getData();
        Optional.ofNullable(account).ifPresent((a -> {
            if (a instanceof AzuriteStorageAccount) {
                btnLocal.setSelected(true);
            } else if (a instanceof ConnectionStringStorageAccount) {
                btnConnectionString.setSelected(true);
                this.txtConnectionString.setValue(a.getConnectionString());
            } else {
                btnAzure.setSelected(true);
                this.subscriptionComboBox.setValue(a.getSubscription());
                this.accountComboBox.setValue(a);
            }
        }));
    }

    public void setMethod(int method) {
        switch (method) {
            case METHOD_AZURE:
                btnAzure.setSelected(true);
                break;
            case METHOD_AZURITE:
                btnLocal.setSelected(true);
                break;
            case METHOD_STRING:
                btnConnectionString.setSelected(true);
                break;
            default:
                break;
        }
    }

    @Nullable
    @Override
    public Resource<IStorageAccount> getValue() {
        final AzureValidationInfo info = this.getValidationInfo(true);
        if (!info.isValid()) {
            return null;
        }
        final String connectionString = this.txtConnectionString.getValue();
        final IStorageAccount account = btnAzure.isSelected() ? this.accountComboBox.getValue() :
            btnLocal.isSelected() ? AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT :
                Azure.az(AzureStorageAccount.class).getOrInitByConnectionString(connectionString);
        final String predefinedId = StringUtils.isNotBlank(connectionString) ? DigestUtils.md5Hex(connectionString) : null;
        if (account instanceof ConnectionStringStorageAccount && StringUtils.isNoneBlank(predefinedId, connectionString)) {
            IntelliJSecureStore.getInstance().savePassword(StorageAccountResourceDefinition.class.getName(), predefinedId, null, connectionString);
        }
        return StorageAccountResourceDefinition.INSTANCE.define(account, predefinedId);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(
            this.accountComboBox,
            this.subscriptionComboBox,
            this.txtConnectionString
        );
    }

    protected void createUIComponents() {
        final Supplier<List<? extends IStorageAccount>> loader = () -> Optional
            .ofNullable(this.subscriptionComboBox)
            .map(AzureComboBox::getValue)
            .map(Subscription::getId)
            .map(id -> Azure.az(AzureStorageAccount.class).accounts(id).list())
            .orElse(Collections.emptyList());
        this.accountComboBox = new AzureComboBox<>(loader) {
            @Nullable
            @Override
            protected IStorageAccount doGetDefaultValue() {
                return CacheManager.getUsageHistory(IStorageAccount.class)
                    .peek(v -> Objects.isNull(subscriptionComboBox) || Objects.equals(subscriptionComboBox.getValue(), v.getSubscription()));
            }

            @Override
            protected String getItemText(Object item) {
                return Optional.ofNullable(item).map(i -> ((IStorageAccount) i).getName()).orElse(StringUtils.EMPTY);
            }

            @Override
            protected void refreshItems() {
                Optional.ofNullable(StorageAccountResourcePanel.this.subscriptionComboBox)
                    .map(AzureComboBox::getValue)
                    .map(Subscription::getId)
                    .ifPresent(id -> Azure.az(AzureStorageAccount.class).accounts(id).refresh());
                super.refreshItems();
            }
        };
    }
}
