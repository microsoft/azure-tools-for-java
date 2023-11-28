/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.keyvault;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzModuleNode;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.keyvault.AzureKeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.certificate.Certificate;
import com.microsoft.azure.toolkit.lib.keyvault.certificate.CertificateModule;
import com.microsoft.azure.toolkit.lib.keyvault.certificate.CertificateVersion;
import com.microsoft.azure.toolkit.lib.keyvault.key.Key;
import com.microsoft.azure.toolkit.lib.keyvault.key.KeyModule;
import com.microsoft.azure.toolkit.lib.keyvault.key.KeyVersion;
import com.microsoft.azure.toolkit.lib.keyvault.secret.Secret;
import com.microsoft.azure.toolkit.lib.keyvault.secret.SecretModule;
import com.microsoft.azure.toolkit.lib.keyvault.secret.SecretVersion;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class KeyVaultNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Key Vault";
    private static final String ICON = AzureIcons.KeyVault.MODULE.getIconPath();

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureKeyVault || data instanceof KeyVault || data instanceof Certificate
                || data instanceof CertificateVersion || data instanceof Key || data instanceof KeyVersion
                || data instanceof Secret || data instanceof SecretVersion;
    }

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureKeyVault.class);
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureKeyVault) {
            final Function<AzureKeyVault, List<KeyVault>> function = azureKeyVault ->
                    azureKeyVault.list().stream().flatMap(m -> m.getKeyVaultModule().list().stream()).collect(Collectors.toList());
            return new AzServiceNode<>(az(AzureKeyVault.class))
                    .withIcon(ICON)
                    .withLabel(NAME)
                    .withActions(KeyVaultActionsContributor.SERVICE_ACTIONS)
                    .addChildren(function, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof KeyVault) {
            return new AzResourceNode<>((KeyVault) data)
                    .addInlineAction(ResourceCommonActionsContributor.PIN)
                    .addChildren(KeyVault::getSubModules, (module, keyVaultNode) -> createNode(module, keyVaultNode, manager))
                    .withActions(KeyVaultActionsContributor.KEY_VAULT_ACTIONS);
        } else if (data instanceof CertificateModule) {
            return new AzModuleNode<>((CertificateModule) data)
                    .withIcon(AzureIcons.KeyVault.CERTIFICATES)
                    .withLabel("Certificates")
                    .addChildren(CertificateModule::list, (d, p) -> this.createNode(d, p, manager))
                    .withActions(KeyVaultActionsContributor.MODULE_ACTIONS);
        } else if (data instanceof Certificate) {
            final Function<Certificate, List<CertificateVersion>> function = certificate -> certificate.versions().list().stream()
                    .sorted(Comparator.comparing(CertificateVersion::isCurrentVersion).thenComparing(CertificateVersion::isEnabled).reversed())
                    .collect(Collectors.toList());
            return new AzResourceNode<>((Certificate) data)
                    .withDescription(key -> key.isEnabled() ? "enabled" : "disabled")
                    .addChildren(function, (d, p) -> this.createNode(d, p, manager))
                    .addInlineAction(ResourceCommonActionsContributor.PIN)
                    .withActions(KeyVaultActionsContributor.CERTIFICATE_ACTIONS);
        } else if (data instanceof CertificateVersion) {
            return new AzResourceNode<>((CertificateVersion) data)
                    .withDescription(KeyVaultNodeProvider::getCertificateVersionDescription)
                    .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                    .withActions(KeyVaultActionsContributor.CERTIFICATE_VERSION_ACTIONS);
        } else if (data instanceof SecretModule) {
            return new AzModuleNode<>((SecretModule) data)
                    .withIcon(AzureIcons.KeyVault.SECRETS)
                    .withLabel("Secrets")
                    .addChildren(AbstractAzResourceModule::list, (d, p) -> this.createNode(d, p, manager))
                    .withActions(KeyVaultActionsContributor.MODULE_ACTIONS);
        } else if (data instanceof Secret) {
            final Function<Secret, List<SecretVersion>> function = secret -> secret.versions().list().stream()
                    .sorted(Comparator.comparing(SecretVersion::isCurrentVersion).thenComparing(SecretVersion::isEnabled).reversed())
                    .collect(Collectors.toList());
            return new AzResourceNode<>((Secret) data)
                    .withDescription(key -> key.isEnabled() ? "enabled" : "disabled")
                    .addChildren(function, (d, p) -> this.createNode(d, p, manager))
                    .addInlineAction(ResourceCommonActionsContributor.PIN)
                    .withActions(KeyVaultActionsContributor.SECRET_ACTIONS);
        } else if (data instanceof SecretVersion) {
            return new AzResourceNode<>((SecretVersion) data)
                    .withDescription(KeyVaultNodeProvider::getSecretVersionDescription)
                    .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                    .withActions(KeyVaultActionsContributor.SECRET_VERSION_ACTIONS);
        } else if (data instanceof KeyModule) {
            return new AzModuleNode<>((KeyModule) data)
                    .withIcon(AzureIcons.KeyVault.KEYS)
                    .withLabel("Keys")
                    .addChildren(AbstractAzResourceModule::list, (d, p) -> this.createNode(d, p, manager))
                    .withActions(KeyVaultActionsContributor.MODULE_ACTIONS);
        } else if (data instanceof Key) {
            final Function<Key, List<KeyVersion>> function = key -> key.versions().list().stream()
                    .sorted(Comparator.comparing(KeyVersion::isCurrentVersion).thenComparing(KeyVersion::isEnabled).reversed())
                    .collect(Collectors.toList());
            return new AzResourceNode<>((Key) data)
                    .withDescription(key -> key.isEnabled() ? "enabled" : "disabled")
                    .addChildren(function, (d, p) -> this.createNode(d, p, manager))
                    .addInlineAction(ResourceCommonActionsContributor.PIN)
                    .withActions(KeyVaultActionsContributor.KEY_ACTIONS);
        } else if (data instanceof KeyVersion) {
            return new AzResourceNode<>((KeyVersion) data)
                    .withDescription(KeyVaultNodeProvider::getKeyVersionDescription)
                    .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                    .withActions(KeyVaultActionsContributor.KEY_VERSION_ACTIONS);
        }
        return null;
    }

    private static String getCertificateVersionDescription(CertificateVersion version) {
        final String current = version.isCurrentVersion() ? "current" : null;
        final String enable = BooleanUtils.isTrue(version.isEnabled()) ? "enabled" : "disabled";
        return Stream.of(current, enable).filter(Objects::nonNull).collect(Collectors.joining(", "));
    }

    private static String getSecretVersionDescription(SecretVersion version) {
        final String current = version.isCurrentVersion() ? "current" : null;
        final String enable = BooleanUtils.isTrue(version.isEnabled()) ? "enabled" : "disabled";
        return Stream.of(current, enable).filter(Objects::nonNull).collect(Collectors.joining(", "));
    }

    private static String getKeyVersionDescription(KeyVersion version) {
        final String current = version.isCurrentVersion() ? "current" : null;
        final String enable = BooleanUtils.isTrue(version.isEnabled()) ? "enabled" : "disabled";
        return Stream.of(current, enable).filter(Objects::nonNull).collect(Collectors.joining(", "));
    }
}
