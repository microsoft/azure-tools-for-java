package com.microsoft.azure.appservice.component.form.input;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ComboBoxServicePlan extends AzureComboBox<AppServicePlan> {

    private Subscription subscription;

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return AzureComboBox.EMPTY_ITEM;
        }
        return ((AppServicePlan) item).name();
    }

    public void refreshWith(Subscription subscription) {
        this.subscription = subscription;
        this.refreshItems();
    }

    @NotNull
    @Override
    protected List<? extends AppServicePlan> loadItems() throws Exception {
        if (Objects.nonNull(this.subscription)) {
            final String sid = subscription.subscriptionId();
            return AzureWebAppMvpModel.getInstance().listAppServicePlanBySubscriptionId(sid);
        }
        return Collections.emptyList();
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
