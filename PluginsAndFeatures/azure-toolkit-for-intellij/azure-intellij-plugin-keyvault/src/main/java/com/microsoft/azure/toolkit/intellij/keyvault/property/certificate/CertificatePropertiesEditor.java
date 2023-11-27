/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.property.certificate;

import com.azure.security.keyvault.certificates.models.CertificatePolicy;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.certificates.models.SubjectAlternativeNames;
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
import com.microsoft.azure.toolkit.lib.keyvault.certificate.Certificate;
import com.microsoft.azure.toolkit.lib.keyvault.certificate.CertificateVersion;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CertificatePropertiesEditor extends AzResourcePropertiesEditor<CertificateVersion> {
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
    private AzureTextFieldWithCopyButton identifierTextField;
    private JBTable revisionsTable;
    private JPanel pnlProperties;
    private JLabel lblCreated;
    private JLabel lblUpdated;
    private JPanel pnlSecret;
    private JPanel pnlRoot;
    private AzureActionButton<Void> saveButton;
    private ActionLink resetButton;
    private AzureHideableTitledSeparator certificateSeparator;
    private JLabel lblActivationDate;
    private JBLabel activationDateTextField;
    private JLabel lblExpirationDate;
    private JBLabel expirationDateTextField;
    private JLabel lblTags;
    private JBLabel tagsTextField;
    private AzureHideableTitledSeparator settingsSeparator;
    private JPanel pnlSettings;
    private AzureActionButton showSecretButton;
    private AzureTextFieldWithCopyButton txtSubject;
    private JLabel lblSecretValue;
    private AzureTextFieldWithCopyButton txtIssuer;
    private AzureTextFieldWithCopyButton txtSerialNumber;
    private JLabel lblIssuer;
    private JLabel lblSerialNumber;
    private JLabel lblAlternativeName;
    private JLabel lblThumbprint;
    private JLabel lblKeyIdentifier;
    private JLabel lblSecret;
    private AzureTextFieldWithCopyButton txtAlternativeName;
    private AzureTextFieldWithCopyButton txtThumbprint;
    private AzureTextFieldWithCopyButton txtKeyIdentifier;
    private AzureTextFieldWithCopyButton txtSecretIdentifier;
    private JLabel lblIdentifier;
    private AzureActionButton btnDownload;
    private AzureTextFieldWithCopyButton azureTextFieldWithCopyButton1;

    private final CertificateVersion resource;
    private final ZoneId zoneId;

    public CertificatePropertiesEditor(@Nonnull Project project, @Nonnull Certificate resource, @Nonnull VirtualFile virtualFile) {
        this(project, resource.getCurrentVersion(), virtualFile);
    }

    public CertificatePropertiesEditor(@Nonnull Project project, @Nonnull CertificateVersion resource, @Nonnull VirtualFile virtualFile) {
        super(virtualFile, resource, project);
        this.resource = resource;
        this.zoneId = ZoneId.systemDefault();
        $$$setupUI$$$();
        init();
        rerender();
    }

    private void init() {
        initListeners();
        this.txtSubject.setEditable(false);
        this.identifierTextField.setEditable(false);
        this.txtSubject.setEditable(false);
        this.txtIssuer.setEditable(false);
        this.txtAlternativeName.setEditable(false);
        this.txtThumbprint.setEditable(false);
        this.txtKeyIdentifier.setEditable(false);
        this.txtSecretIdentifier.setEditable(false);

        this.propertiesSeparator.addContentComponent(pnlProperties);
        this.certificateSeparator.addContentComponent(pnlSecret);
        this.settingsSeparator.addContentComponent(pnlSettings);
    }

    private void initListeners() {
        this.resetButton.addActionListener(e -> this.reset());
        final Action<Void> refreshAction = new Action<Void>(Action.Id.of("user/keyvault.refresh_properties_view.secret"))
                .withAuthRequired(true)
                .withSource(this.resource)
                .withIdParam(this.resource.getName())
                .withHandler(ignore -> this.refresh());
        this.btnRefresh.setAction(refreshAction);

        this.btnDownload.setAction(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL_VERSION, this.resource);
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

    private void setData(@Nonnull final CertificateVersion secret) {
        final CertificateProperties properties = secret.getProperties();
        if (Objects.isNull(properties)) {
            return;
        }
        // properties
        createdTextField.setText(Optional.ofNullable(properties.getCreatedOn()).map(date -> date.format(dateTimeFormatter)).orElse(N_A));
        updatedTextField.setText(Optional.ofNullable(properties.getUpdatedOn()).map(date -> date.format(dateTimeFormatter)).orElse(N_A));
        identifierTextField.setText(properties.getId());
        // settings
        activationDateTextField.setText(Optional.ofNullable(properties.getNotBefore()).map(date -> date.format(dateTimeFormatter)).orElse(N_A));
        expirationDateTextField.setText(Optional.ofNullable(properties.getExpiresOn()).map(date -> date.format(dateTimeFormatter)).orElse(N_A));
        final String labels = Optional.ofNullable(properties.getTags())
                .map(tags -> tags.entrySet().stream()
                        .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(", "))).orElse(N_A);
        tagsTextField.setText(labels);
        // certificate
        AzureTaskManager.getInstance().runInBackground("Loading data", () -> {
            final CertificatePolicy policy = secret.getPolicy();
            AzureTaskManager.getInstance().runLater(() -> renderCertificate(properties, policy));
        });
    }

    private void renderCertificate(@Nullable final CertificateProperties properties, @Nullable final CertificatePolicy policy) {
        txtSubject.setText(Optional.ofNullable(policy).map(CertificatePolicy::getSubject).orElse(N_A));
        final String issuer = Optional.ofNullable(policy).map(CertificatePolicy::getIssuerName).orElse(N_A);
        txtIssuer.setText(StringUtils.equalsIgnoreCase(issuer, "Self") ? txtSubject.getText() : issuer);
        final String alternativeNames = Optional.ofNullable(policy)
                .map(CertificatePolicy::getSubjectAlternativeNames)
                .map(SubjectAlternativeNames::getUserPrincipalNames)
                .filter(CollectionUtils::isNotEmpty)
                .map(names -> String.join(", ", names))
                .orElse(N_A);
        txtAlternativeName.setText(alternativeNames);
        final String thumbprint = new String(Hex.encodeHex(properties.getX509Thumbprint())).toUpperCase(Locale.ROOT);
        txtThumbprint.setText(thumbprint);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    @Override
    public @Nonnull JComponent getComponent() {
        return pnlRoot;
    }
}
