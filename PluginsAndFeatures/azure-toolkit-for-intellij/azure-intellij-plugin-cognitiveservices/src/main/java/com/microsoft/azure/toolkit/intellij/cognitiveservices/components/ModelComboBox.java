/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.components;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.AccountModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ModelComboBox extends AzureComboBox<AccountModel> {
    @Nonnull
    private final CognitiveAccount account;

    public ModelComboBox(@Nonnull final CognitiveAccount account) {
        super(true);
        this.account = account;
        this.setItemsLoader(account::listModels);
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof AccountModel) {
            return String.format("%s (version: %s)", ((AccountModel) item).getName(), ((AccountModel) item).getVersion());
        }
        return super.getItemText(item);
    }

    @Nullable
    @Override
    protected AccountModel doGetDefaultValue() {
        return Optional.ofNullable(super.doGetDefaultValue()).orElseGet(() ->
                this.getItems().stream().filter(AccountModel::isGPTModel).findFirst().orElse(null));
    }
}
