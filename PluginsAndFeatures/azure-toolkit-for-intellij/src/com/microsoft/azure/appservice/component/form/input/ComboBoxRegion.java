package com.microsoft.azure.appservice.component.form.input;

import com.microsoft.azure.management.resources.fluentcore.arm.Region;

import java.util.List;

public class ComboBoxRegion extends AzureComboBoxSimple<Region> {
    public ComboBoxRegion(final DataProvider<? extends List<? extends Region>> provider) {
        super(provider);
    }
}
