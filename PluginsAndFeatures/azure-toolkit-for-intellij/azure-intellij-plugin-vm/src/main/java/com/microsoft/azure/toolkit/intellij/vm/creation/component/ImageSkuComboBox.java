/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImageOffer;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImageSku;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ImageSkuComboBox extends AzureComboBox<AzureImageSku> {
    private AzureImageOffer offer;

    public void setOffer(AzureImageOffer offer) {
        this.offer = offer;
        this.clear();
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof AzureImageSku ? ((AzureImageSku) item).name() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends AzureImageSku> loadItems() throws Exception {
        return Optional.ofNullable(offer).map(AzureImageOffer::skus).orElse(Collections.emptyList());
    }
}
