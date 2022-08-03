/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImageOffer;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImageSku;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ImageSkuComboBox extends AzureComboBox<VmImageSku> {
    private VmImageOffer offer;

    public void setOffer(VmImageOffer offer) {
        this.offer = offer;
        this.clear();
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof VmImageSku ? ((VmImageSku) item).name() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends VmImageSku> loadItems() throws Exception {
        return Optional.ofNullable(offer).map(VmImageOffer::skus).orElse(Collections.emptyList());
    }
}
