/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.keyvault.KeyVaultActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.keyvault.connection.KeyVaultResourceDefinition;
import com.microsoft.azure.toolkit.intellij.keyvault.creation.certificate.CertificateCreationActions;
import com.microsoft.azure.toolkit.intellij.keyvault.creation.key.KeyCreationActions;
import com.microsoft.azure.toolkit.intellij.keyvault.creation.secret.SecretCreationActions;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.keyvault.AzureKeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.CredentialVersion;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.certificate.Certificate;
import com.microsoft.azure.toolkit.lib.keyvault.certificate.CertificateModule;
import com.microsoft.azure.toolkit.lib.keyvault.key.Key;
import com.microsoft.azure.toolkit.lib.keyvault.key.KeyModule;
import com.microsoft.azure.toolkit.lib.keyvault.secret.Secret;
import com.microsoft.azure.toolkit.lib.keyvault.secret.SecretModule;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.util.function.BiPredicate;

import static com.microsoft.azure.toolkit.intellij.keyvault.creation.KeyVaultCreationActions.createNewKeyVault;
import static com.microsoft.azure.toolkit.intellij.keyvault.creation.KeyVaultCreationActions.getDefaultConfig;

public class IntelliJKeyVaultActionsContributor implements IActionsContributor {

    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof AzureKeyVault,
                (Object ignore, AnActionEvent e) -> createNewKeyVault(getDefaultConfig(null), e.getProject()));
        am.registerHandler(KeyVaultActionsContributor.GROUP_CREATE_KEY_VAULT, (r, e) -> true,
                (ResourceGroup group, AnActionEvent e) -> createNewKeyVault(getDefaultConfig(group), e.getProject()));

        am.<AzResource, AnActionEvent>registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof KeyVault,
            (r, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final ConnectorDialog dialog = new ConnectorDialog(e.getProject());
                dialog.setResource(new AzureServiceResource<>(((KeyVault) r), KeyVaultResourceDefinition.INSTANCE));
                dialog.show();
            }));

        final BiPredicate<CredentialVersion, AnActionEvent> certificateCondition = (r, e) -> r instanceof CredentialVersion;
        am.registerHandler(KeyVaultActionsContributor.DOWNLOAD_CREDENTIAL_VERSION, certificateCondition,
                (CredentialVersion r, AnActionEvent e) -> KeyVaultCredentialActions.downloadCredential(r, e.getProject()));

        am.registerHandler(KeyVaultActionsContributor.SHOW_CREDENTIAL_VERSION, certificateCondition,
                (CredentialVersion r, AnActionEvent e) -> KeyVaultCredentialActions.showCredential(r, e.getProject()));

        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof SecretModule,
                (Object r, AnActionEvent e) -> SecretCreationActions.createNewSecret(((SecretModule) r).getParent(), e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof Secret,
                (Object r, AnActionEvent e) -> SecretCreationActions.createNewSecretVersion((Secret) r, e.getProject()));

        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof CertificateModule,
                (Object r, AnActionEvent e) -> CertificateCreationActions.createNewCertificate(((CertificateModule) r).getParent(), e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof Certificate,
                (Object r, AnActionEvent e) -> CertificateCreationActions.createNewCertificateVersion((Certificate) r, e.getProject()));

        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof KeyModule,
                (Object r, AnActionEvent e) -> KeyCreationActions.createNewKey(((KeyModule) r).getParent(), e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof Key,
                (Object r, AnActionEvent e) -> KeyCreationActions.createNewKeyVersion((Key) r, e.getProject()));
    }

    @Override
    public int getOrder() {
        return KeyVaultActionsContributor.INITIALIZE_ORDER + 1;
    }
}
