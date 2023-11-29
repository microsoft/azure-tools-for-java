/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StoragePathReference extends PsiReferenceBase<PsiElement> {
    private StorageAccount account;
    private String fullNameWithPrefix;

    public StoragePathReference(@Nonnull PsiElement element, TextRange rangeInElement, boolean soft) {
        super(element, rangeInElement, soft);
    }

    public StoragePathReference(@Nonnull PsiElement element, TextRange rangeInElement, final @Nonnull String fullNameWithPrefix) {
        this(element, rangeInElement, fullNameWithPrefix, null);
    }

    public StoragePathReference(@Nonnull PsiElement element, TextRange rangeInElement,
                                @Nonnull final String fullNameWithPrefix, @Nullable final StorageAccount account) {
        super(element, rangeInElement);
        this.fullNameWithPrefix = fullNameWithPrefix;
        this.account = account;
    }

    public StoragePathReference(@Nonnull PsiElement element, boolean soft) {
        super(element, soft);
    }

    public StoragePathReference(@Nonnull PsiElement element) {
        super(element);
    }

    @Override
    public @Nullable PsiElement resolve() {
        if(!Azure.az(AzureAccount.class).isLoggedIn()){
            return null;
        }
        return Optional.of(this.getElement()).map(ModuleUtil::findModuleForPsiElement)
                .map(m -> Objects.isNull(account) ? StoragePathCompletionProvider.getFile(fullNameWithPrefix, m) :
                        StoragePathCompletionProvider.getFile(fullNameWithPrefix, List.of(account)))
                .map(AzureStorageResourcePsiElement::new)
                .orElse(null);
    }

    class AzureStorageResourcePsiElement extends FakePsiElement implements SyntheticElement {
        private final StorageFile file;

        public AzureStorageResourcePsiElement(final StorageFile file) {
            super();
            this.file = file;
        }

        @Override
        public PsiElement getParent() {
            return myElement;
        }

        @Override
        @AzureOperation("user/connector.navigate_to_storage_resource_from_string_literal")
        public void navigate(boolean requestFocus) {
            final Module module = ModuleUtil.findModuleForPsiElement(getElement());
            StoragePathCompletionProvider.navigateToFile(this.file, module);
        }

        @Override
        public String getPresentableText() {
            return file.getName();
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public @Nullable String getLocationString() {
            final StorageAccount account = StoragePathCompletionProvider.getStorageAccount(this.file);
            if (Objects.nonNull(account)) {
                return this.file.getResourceGroupName() + "/" + account.getName();
            } else {
                return this.file.getResourceGroupName();
            }
        }

        @Override
        public @Nullable Icon getIcon(final boolean open) {
            return IntelliJAzureIcons.getIcon(StoragePathCompletionProvider.getFileIcon(this.file));
        }

        @Override
        public TextRange getTextRange() {
            final TextRange rangeInElement = getRangeInElement();
            final TextRange elementRange = myElement.getTextRange();
            return elementRange != null ? rangeInElement.shiftRight(elementRange.getStartOffset()) : rangeInElement;
        }
    }
}
