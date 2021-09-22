/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImage;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImageSku;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ImageComboBox extends AzureComboBox<AzureImage> {
    private AzureImageSku imageSku;

    public void setImageSku(AzureImageSku imageSku) {
        this.imageSku = imageSku;
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof AzureImage ? String.format("%s:%s", ((AzureImage) item).getSku(), ((AzureImage) item).getVersion()) : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends AzureImage> loadItems() throws Exception {
        return Optional.ofNullable(imageSku).map(AzureImageSku::images).orElse(Collections.emptyList());
    }
}
