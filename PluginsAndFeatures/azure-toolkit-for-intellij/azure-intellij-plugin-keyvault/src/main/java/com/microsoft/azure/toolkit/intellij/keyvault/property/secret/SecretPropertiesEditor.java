/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.property.secret;

import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.ide.keyvault.KeyVaultActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.component.AzureTextFieldWithCopyButton;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.keyvault.secret.Secret;
import com.microsoft.azure.toolkit.lib.keyvault.secret.SecretVersion;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SecretPropertiesEditor extends AzResourcePropertiesEditor<SecretVersion> {
    public static final String N_A = "N/A";
    private static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.n z").withZone(ZoneId.systemDefault());
    public static final String SECRET_VISIBLE = "SecretVisible";
    public static final String SECRET_VALUE = "SecretValue";
    private JPanel pnlContent;
    private JPanel propertyActionPanel;
    private AzureActionButton<Void> btnRefresh;
    private AzureHideableTitledSeparator propertiesSeparator;
    private JBLabel createdTextField;
    private JBLabel updatedTextField;
    private AzureTextFieldWithCopyButton secretIdentifierTextField;
    private JBTable revisionsTable;
    private JPanel pnlProperties;
    private JLabel lblCreated;
    private JLabel lblUpdated;
    private JLabel lblSecretIdentifier;
    private JPanel pnlSecret;
    private JPanel pnlRoot;
    private AzureActionButton<Void> saveButton;
    private ActionLink resetButton;
    private AzureHideableTitledSeparator secretSeparator;
    private JLabel lblActivationDate;
    private JBLabel activationDateTextField;
    private JLabel lblExpirationDate;
    private JBLabel expirationDateTextField;
    private JLabel lblTags;
    private JBLabel tagsTextField;
    private AzureHideableTitledSeparator settingsSeparator;
    private JPanel pnlSettings;
    private AzureActionButton showSecretButton;
    private AzureTextFieldWithCopyButton txtSecretValue;
    private JLabel lblSecretValue;
    private JLabel lblContentType;
    private JBLabel txtContentType;
    private AzureTextFieldWithCopyButton azureTextFieldWithCopyButton1;

    private final SecretVersion resource;
    private final ZoneId zoneId;

    public SecretPropertiesEditor(@Nonnull Project project, @Nonnull Secret resource, @Nonnull VirtualFile virtualFile) {
        this(project, resource.getCurrentVersion(), virtualFile);
    }

    public SecretPropertiesEditor(@Nonnull Project project, @Nonnull SecretVersion resource, @Nonnull VirtualFile virtualFile) {
        super(virtualFile, resource, project);
        this.resource = resource;
        this.zoneId = ZoneId.systemDefault();
        $$$setupUI$$$();
        init();
        rerender();
    }

    private void init() {
        initListeners();
        this.txtSecretValue.setEditable(false);
        this.secretIdentifierTextField.setEditable(false);

        this.propertiesSeparator.addContentComponent(pnlProperties);
        this.secretSeparator.addContentComponent(pnlSecret);
        this.settingsSeparator.addContentComponent(pnlSettings);
    }

    private void initListeners() {
        this.resetButton.addActionListener(e -> this.reset());
        final Action<Void> refreshAction = new Action<Void>(Action.Id.of("user/keyvault.refresh_properties_view.secret"))
                .withAuthRequired(true)
                .withSource(this.resource)
                .withIdParam(Optional.ofNullable(this.resource).map(AzResource::getName).orElse(StringUtils.EMPTY))
                .withHandler(ignore -> this.refresh());
        this.btnRefresh.setAction(refreshAction);

        this.showSecretButton.setAction(KeyVaultActionsContributor.SHOW_CREDENTIAL_VERSION, this.resource);
    }

    private void setEnabled(boolean enabled) {
        this.resetButton.setVisible(enabled);
        this.saveButton.setEnabled(enabled);
    }

    private void refreshToolbar() {
        // get status from app instead of draft since status of draft is not correct
        final AzResource.FormalStatus formalStatus = this.resource.getFormalStatus();
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final boolean normal = formalStatus.isRunning() || formalStatus.isStopped();
            this.setEnabled(normal);
            if (normal) {
                manager.runOnPooledThread(() -> {
                    final boolean modified = this.isModified(); // checking modified is slow
                    manager.runLater(() -> {
                        this.resetButton.setVisible(modified);
                        this.saveButton.setEnabled(modified);
                    });
                });
            } else {
                this.resetButton.setVisible(false);
                this.saveButton.setVisible(false);
            }
        });
    }

    @Override
    public boolean isModified() {
        // todo: support properties update for keyvault
        return false;
    }

    private void reset() {
        this.rerender();
    }

    private void refresh() {
        AzureTaskManager.getInstance().runInBackground("Refreshing...", () -> {
            this.resource.refresh();
            this.rerender();
        });
    }

    @Override
    protected void rerender() {
        AzureTaskManager.getInstance().runLater(() -> {
            this.refreshToolbar();
            this.setData(this.resource);
        });
    }

    private void setData(@Nonnull final SecretVersion secret) {
        final SecretProperties properties = secret.getProperties();
        if (Objects.isNull(properties)) {
            return;
        }
        // properties
        createdTextField.setText(Optional.ofNullable(properties.getCreatedOn()).map(date -> date.format(dateTimeFormatter)).orElse(N_A));
        updatedTextField.setText(Optional.ofNullable(properties.getUpdatedOn()).map(date -> date.format(dateTimeFormatter)).orElse(N_A));
        secretIdentifierTextField.setText(properties.getId());
        // settings
        activationDateTextField.setText(Optional.ofNullable(properties.getNotBefore()).map(date -> date.format(dateTimeFormatter)).orElse(N_A));
        expirationDateTextField.setText(Optional.ofNullable(properties.getExpiresOn()).map(date -> date.format(dateTimeFormatter)).orElse(N_A));
        final String labels = Optional.ofNullable(properties.getTags())
                .filter(MapUtils::isNotEmpty)
                .map(tags -> tags.entrySet().stream()
                        .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(", "))).orElse(N_A);
        tagsTextField.setText(labels);
        // secret
        txtContentType.setText(StringUtils.firstNonBlank(properties.getContentType(), N_A));
        // buttons
        this.showSecretButton.setEnabled(properties.isEnabled());
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    @Override
    public @Nonnull JComponent getComponent() {
        return pnlRoot;
    }
}
