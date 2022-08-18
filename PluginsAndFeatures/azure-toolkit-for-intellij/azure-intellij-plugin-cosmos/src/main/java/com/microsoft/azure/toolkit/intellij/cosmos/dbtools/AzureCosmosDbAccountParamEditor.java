/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.credentialStore.OneTimeString;
import com.intellij.database.dataSource.DataSourceConfigurable;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.FieldSize;
import com.intellij.database.dataSource.url.template.UrlEditorModel;
import com.intellij.database.dataSource.url.ui.ParamEditorBase;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDBAccountAction;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.model.CosmosDBAccountConnectionString;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureCosmosDbAccountParamEditor extends ParamEditorBase<AzureCosmosDbAccountParamEditor.CosmosDbAccountComboBox> {
    public static final String KEY_COSMOS_ACCOUNT_ID = "AZURE_COSMOS_ACCOUNT";
    public static final String NONE = "<NONE>";
    public static final String NO_ACCOUNT_TIPS_TEMPLATE = "<html>No Azure Cosmos DB accounts (%s) found in selected subscriptions, " +
        "create one in <a href=''>Azure Explorer</a> or " +
        "<a href='https://ms.portal.azure.com/#create/Microsoft.DocumentDB'>Azure Portal</a>.</html>";
    public static final String NOT_SIGNIN_TIPS = "<html><a href=\"\">Sign in</a> to select an existing Azure Cosmos DB account.</html>";
    private final DatabaseAccountKind kind;
    @Getter
    @Setter
    private String text = "";
    private CosmosDBAccount account;
    private CosmosDBAccountConnectionString connectionString;
    private boolean updating;

    public AzureCosmosDbAccountParamEditor(@Nonnull DatabaseAccountKind kind, @Nonnull String label, @Nonnull DataInterchange interchange) {
        super(new CosmosDbAccountComboBox(kind), interchange, FieldSize.LARGE, label);
        this.kind = kind;
        final CosmosDbAccountComboBox combox = this.getEditorComponent();
        interchange.addPersistentProperty(KEY_COSMOS_ACCOUNT_ID);
        final String initialAccountId = interchange.getProperty(KEY_COSMOS_ACCOUNT_ID);
        if (StringUtils.isNotBlank(initialAccountId)) {
            combox.setValue(new AzureComboBox.ItemReference<>(i -> i.getId().equals(initialAccountId)));
        }

        interchange.addPropertyChangeListener((evt -> onPropertiesChanged(evt.getPropertyName(), evt.getNewValue())), this);
        combox.addValueChangedListener(this::setAccount);
    }

    @Override
    protected @Nonnull JComponent createComponent(CosmosDbAccountComboBox combox) {
        final JPanel container = new JPanel();
        final BoxLayout layout = new BoxLayout(container, BoxLayout.Y_AXIS);
        container.setLayout(layout);

        combox.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(combox);

        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            final HyperlinkLabel notSignInTips = new HyperlinkLabel();
            notSignInTips.setForeground(UIUtil.getContextHelpForeground());
            notSignInTips.setHtmlText(NOT_SIGNIN_TIPS);
            notSignInTips.setIcon(AllIcons.General.Information);
            notSignInTips.addHyperlinkListener(e -> signInAndReloadItems(combox, notSignInTips));
            notSignInTips.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(notSignInTips);
        }

        final JBLabel noAccountsTips = initNoAccountTipsLabel(combox);
        container.add(noAccountsTips);
        return container;
    }

    @AzureOperation(name = "cosmos.signin_from_dbtools", type = AzureOperation.Type.ACTION)
    private void signInAndReloadItems(CosmosDbAccountComboBox combox, HyperlinkLabel notSignInTips) {
        OperationContext.action().setTelemetryProperty("kind", this.kind.getValue());
        AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle(() -> {
            notSignInTips.setVisible(false);
            combox.reloadItems();
        });
    }

    private JBLabel initNoAccountTipsLabel(CosmosDbAccountComboBox combox) {
        final JBLabel label = new JBLabel(String.format(NO_ACCOUNT_TIPS_TEMPLATE, combox.getKind().getValue())) {
            @Override
            protected @Nonnull HyperlinkListener createHyperlinkListener() {
                return new HyperlinkAdapter() {
                    @Override
                    protected void hyperlinkActivated(HyperlinkEvent e) {
                        if (Objects.nonNull(e.getURL()) && e.getURL().toString().startsWith("https")) {
                            createAccountInPortal(e.getURL());
                        } else {
                            createAccountInIde();
                        }
                    }
                };
            }
        };
        label.setFocusable(false);
        label.setCopyable(true);
        label.setVisible(false);
        label.setVerticalAlignment(SwingConstants.TOP);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setIcon(AllIcons.General.Information);
        combox.setAccountsListener(label::setVisible);
        return label;
    }

    @AzureOperation(name = "cosmos.create_account_portal_from_dbtools", type = AzureOperation.Type.ACTION)
    private void createAccountInPortal(URL url) {
        OperationContext.action().setTelemetryProperty("kind", this.kind.getValue());
        BrowserUtil.browse(url);
    }

    @AzureOperation(name = "cosmos.create_account_ide_from_dbtools", type = AzureOperation.Type.ACTION)
    private void createAccountInIde() {
        OperationContext.action().setTelemetryProperty("kind", this.kind.getValue());
        final Window window = ComponentUtil.getActiveWindow();
        window.setVisible(false);
        window.dispose();
        CreateCosmosDBAccountAction.create(null, null);
    }

    private void onPropertiesChanged(String propertyName, Object newValue) {
        if (!this.updating && Objects.nonNull(this.connectionString) && StringUtils.isNotEmpty((String) newValue)) {
            if (StringUtils.equals(propertyName, "host") && !Objects.equals(this.connectionString.getHost(), newValue) ||
                StringUtils.equals(propertyName, "port") && !Objects.equals(this.connectionString.getPort() + "", newValue)) {
                this.getEditorComponent().setValue((CosmosDBAccount) null);
                this.setAccount(null);
            }
        }
    }

    @AzureOperation(name = "cosmos.select_account_dbtools.account", params = {"account.getName()"}, type = AzureOperation.Type.ACTION)
    private void setAccount(@Nullable CosmosDBAccount account) {
        Optional.ofNullable(account).ifPresent(a -> {
            OperationContext.action().setTelemetryProperty("subscriptionId", a.getSubscriptionId());
            OperationContext.action().setTelemetryProperty("resourceType", a.getFullResourceType());
            OperationContext.action().setTelemetryProperty("kind", a.getKind().getValue());
        });
        if (this.updating || Objects.equals(this.account, account)) {
            return;
        }
        final DataInterchange interchange = this.getInterchange();
        this.account = account;
        if (account == null) {
            this.connectionString = null;
            interchange.putProperty(KEY_COSMOS_ACCOUNT_ID, NONE);
            return;
        }
        this.updating = true;
        // AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle(combox::reloadItems);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            this.connectionString = account.getCosmosDBAccountPrimaryConnectionString();
            final LocalDataSource dataSource = interchange.getDataSource();
            final String host = connectionString.getHost();
            final String port = String.valueOf(connectionString.getPort());
            final String user = connectionString.getUsername();
            final String password = String.valueOf(connectionString.getPassword());
            this.text = connectionString.getConnectionString();
            AzureTaskManager.getInstance().runLater(() -> {
                LocalDataSource.setUsername(dataSource, user);
                interchange.getCredentials().storePassword(dataSource, new OneTimeString(password));
                this.setUseSsl(true);
                interchange.putProperties(consumer -> {
                    consumer.consume(KEY_COSMOS_ACCOUNT_ID, account.getId());
                    consumer.consume("host", host);
                    consumer.consume("user", user);
                    consumer.consume("port", port);
                    this.updating = false;
                });
                this.setUsername(user);
            }, AzureTask.Modality.ANY);
        });
    }

    private void setUsername(String user) {
        final UrlEditorModel model = this.getDataSourceConfigurable().getUrlEditor().getEditorModel();
        model.setParameter("user", user);
        model.commit(true);
    }

    @SneakyThrows
    private void setUseSsl(boolean useSsl) {
        final DataSourceConfigurable configurable = this.getDataSourceConfigurable();
        final JBCheckBox useSSLCheckBox = (JBCheckBox) FieldUtils.readField(configurable.getSshSslPanel(), "myUseSSLJBCheckBox", true);
        useSSLCheckBox.setSelected(useSsl);
    }

    @SneakyThrows
    private DataSourceConfigurable getDataSourceConfigurable() {
        return (DataSourceConfigurable) FieldUtils.readField(this.getInterchange(), "myConfigurable", true);
    }

    @Getter
    @RequiredArgsConstructor
    static class CosmosDbAccountComboBox extends AzureComboBox<CosmosDBAccount> {
        private final DatabaseAccountKind kind;
        private boolean noAccounts;
        private Consumer<Boolean> accountsListener;

        @Nonnull
        @Override
        protected List<CosmosDBAccount> loadItems() {
            if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                return Collections.emptyList();
            }
            final List<CosmosDBAccount> accounts = Azure.az(AzureCosmosService.class).getDatabaseAccounts(kind).stream()
                .filter(m -> !m.isDraftForCreating()).collect(Collectors.toList());
            this.noAccounts = accounts.size() == 0;
            Optional.ofNullable(this.accountsListener).ifPresent(l -> l.consume(this.noAccounts));
            return accounts;
        }

        public void setAccountsListener(@Nonnull Consumer<Boolean> accountsListener) {
            this.accountsListener = accountsListener;
            accountsListener.consume(this.noAccounts);
        }

        @Override
        protected String getItemText(Object item) {
            return Optional.ofNullable(item).map(i -> ((CosmosDBAccount) i)).map(AbstractAzResource::getName).orElse("");
        }
    }
}
