package com.microsoft.azure.appservice.component.form.input;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.List;

public class ComboBoxResourceGroup extends AzureComboBoxSimple<ResourceGroup> {
    public ComboBoxResourceGroup(final DataProvider<? extends List<? extends ResourceGroup>> provider) {
        super(provider);
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.Add, "Create new resource group", this::showResourceGroupCreationPopup);
    }

    private void showResourceGroupCreationPopup() {

    }
}
