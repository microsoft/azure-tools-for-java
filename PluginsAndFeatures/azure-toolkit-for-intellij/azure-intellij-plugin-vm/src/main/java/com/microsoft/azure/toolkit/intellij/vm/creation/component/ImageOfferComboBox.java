/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImageOffer;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureImagePublisher;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ImageOfferComboBox extends AzureComboBox<AzureImageOffer> {
    private AzureImagePublisher publisher;

    public void setPublisher(AzureImagePublisher publisher) {
        this.publisher = publisher;
        this.clear();
        refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof AzureImageOffer ? ((AzureImageOffer) item).name() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends AzureImageOffer> loadItems() throws Exception {
        return Optional.ofNullable(publisher).map(AzureImagePublisher::offers).orElse(Collections.emptyList());
    }
}
