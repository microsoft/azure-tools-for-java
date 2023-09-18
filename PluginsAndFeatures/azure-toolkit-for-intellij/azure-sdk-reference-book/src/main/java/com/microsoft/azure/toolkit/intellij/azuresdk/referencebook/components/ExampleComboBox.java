/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.components;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExampleEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExamplesEntity;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ExampleComboBox extends AzureComboBox<AzureJavaSdkArtifactExampleEntity> {
    public final static AzureJavaSdkArtifactExampleEntity NONE = new AzureJavaSdkArtifactExampleEntity();
    @Getter
    private AzureJavaSdkArtifactExamplesEntity entity;

    public ExampleComboBox() {
        super();
    }

    public void setEntity(@Nonnull final AzureJavaSdkArtifactExamplesEntity entity) {
        this.entity = entity;
        this.refreshItems();
    }

    @Nonnull
    @Override
    protected List<AzureJavaSdkArtifactExampleEntity> loadItems() {
        return Optional.ofNullable(entity)
            .map(AzureJavaSdkArtifactExamplesEntity::getExamples)
            .orElseGet(() -> Collections.singletonList(NONE));
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    protected String getItemText(Object item) {
        if (item == NONE) {
            return "None";
        } else if (item instanceof AzureJavaSdkArtifactExampleEntity) {
            final String file = ((AzureJavaSdkArtifactExampleEntity) item).getFile();
            final int start = file.lastIndexOf("/");
            final int end = file.lastIndexOf(".java");
            return file.substring(start > 0 ? start + 1 : 0, end > 0 ? end : file.length());
        }
        return super.getItemText(item);
    }

}
