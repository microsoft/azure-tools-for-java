package com.microsoft.azure.appservice.component.form.input;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.List;

public class ComboBoxServicePlan extends AzureComboBoxSimple<AppServicePlan> {

    public ComboBoxServicePlan(final DataProvider<? extends List<? extends AppServicePlan>> provider) {
        super(provider);
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.Add, "Create new service plan", this::showServicePlanCreationPopup);
    }

    private void showServicePlanCreationPopup() {

    }
}
