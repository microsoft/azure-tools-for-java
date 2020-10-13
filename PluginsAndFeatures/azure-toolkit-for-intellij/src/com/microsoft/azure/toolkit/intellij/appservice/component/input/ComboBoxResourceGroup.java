package com.microsoft.azure.toolkit.intellij.appservice.component.input;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ComboBoxResourceGroup extends AzureComboBox<ResourceGroup> {
    private Subscription subscription;

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        return ((ResourceGroup) item).name();
    }

    public void refreshWith(Subscription subscription) {
        this.subscription = subscription;
        this.refreshItems();
    }

    @NotNull
    @Override
    protected List<? extends ResourceGroup> loadItems() throws Exception {
        if (Objects.nonNull(this.subscription)) {
            final String sid = subscription.subscriptionId();
            return AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(sid);
        }
        return Collections.emptyList();
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
