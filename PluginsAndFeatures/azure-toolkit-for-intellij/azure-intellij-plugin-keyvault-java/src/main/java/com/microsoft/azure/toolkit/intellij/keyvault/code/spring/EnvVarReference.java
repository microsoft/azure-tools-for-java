/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.code.spring;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.projectexplorer.AbstractAzureFacetNode;
import com.microsoft.azure.toolkit.intellij.keyvault.connection.KeyVaultResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.secret.Secret;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Optional;

public class EnvVarReference extends PsiReferenceBase<PsiElement> {

    public EnvVarReference(@Nonnull PsiElement element, TextRange rangeInElement, boolean soft) {
        super(element, rangeInElement, soft);
    }

    public EnvVarReference(@Nonnull PsiElement element, TextRange rangeInElement) {
        super(element, rangeInElement);
    }

    @Override
    @Nullable
    @SuppressWarnings("ClassEscapesDefinedScope")
    public EnvVarPsiElement resolve() {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return null;
        }
        return Optional.of(this.getElement()).map(ModuleUtil::findModuleForPsiElement)
            .map(AzureModule::from).stream()
            .flatMap(m -> m.getConnections(KeyVaultResourceDefinition.INSTANCE).stream())
            .filter(Connection::isValidConnection)
            .flatMap(c -> Optional.of(c.getResource())
                .map(Resource::getData)
                .map(this::getSecret)
                .map(s -> new EnvVarPsiElement(s, c)).stream())
            .findAny()
            .orElse(null);
    }

    @Nullable
    private Secret getSecret(final KeyVault v) {
        try {
            return v.secrets().get(getValue(), v.getResourceGroupName());
        } catch (final Throwable e) {
            return null;
        }
    }

    class EnvVarPsiElement extends FakePsiElement implements SyntheticElement {
        @Getter
        private final Secret secret;
        private final Connection<?, ?> connection;

        public EnvVarPsiElement(final Secret secret, final Connection<?, ?> connection) {
            super();
            this.secret = secret;
            this.connection = connection;
        }

        @Override
        public PsiElement getParent() {
            return myElement;
        }

        @Override
        @AzureOperation("user/connector.navigate_to_keyvault_secret_from_reference")
        public void navigate(boolean requestFocus) {
            AbstractAzureFacetNode.selectConnectedResource(connection, this.secret.getId(), requestFocus);
        }

        @Override
        @Nonnull
        public String getName() {
            return secret.getName();
        }

        @Override
        public @Nullable String getLocationString() {
            return this.secret.getResourceGroupName() + "/" + this.secret.getParent().getName();
        }

        @Override
        @Nonnull
        public Icon getIcon(final boolean open) {
            final String iconPath = Optional.ofNullable(this.secret)
                    .map(AzureResourceIconProvider::getResourceBaseIconPath)
                    .orElse(AzureIcons.Common.AZURE.getIconPath());
            return Optional.ofNullable(iconPath).filter(StringUtils::isNoneBlank)
                    .map(IntelliJAzureIcons::getIcon).orElse(AllIcons.Providers.Azure);
        }

        @Override
        @Nonnull
        public TextRange getTextRange() {
            final TextRange rangeInElement = getRangeInElement();
            final TextRange elementRange = getParent().getTextRange();
            return elementRange != null ? rangeInElement.shiftRight(elementRange.getStartOffset()) : rangeInElement;
        }
    }
}
