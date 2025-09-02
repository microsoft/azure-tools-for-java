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
                findBestAvailableGPTModel(this.getItems()));
    }

    /**
     * Find the best available GPT model from the list, prioritizing newer models like gpt-4o-mini
     */
    private AccountModel findBestAvailableGPTModel(List<AccountModel> models) {
        if (models == null || models.isEmpty()) {
            return null;
        }
        
        // Define preferred models in order of preference
        final String[] preferredModels = {
            "gpt-4o-mini",     // Cost-effective, widely available
            "gpt-4o",          // Latest capabilities  
            "gpt-35-turbo",    // Widely available fallback
            "gpt-4-turbo",     // Alternative advanced model
            "gpt-4"            // Fallback GPT-4
        };
        
        // First, try to find models by exact name match
        for (String preferredModelName : preferredModels) {
            for (AccountModel model : models) {
                if (model.getName().toLowerCase().contains(preferredModelName.toLowerCase())) {
                    return model;
                }
            }
        }
        
        // Fallback to original GPT filter
        return models.stream().filter(AccountModel::isGPTModel).findFirst().orElse(null);
    }
}
