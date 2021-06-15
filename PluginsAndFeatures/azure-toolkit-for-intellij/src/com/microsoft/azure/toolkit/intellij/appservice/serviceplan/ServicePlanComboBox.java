/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.serviceplan;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.appservice.DraftServicePlan;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ServicePlanComboBox extends AzureComboBox<AppServicePlanEntity> {

    private Subscription subscription;
    private final List<DraftServicePlan> localItems = new ArrayList<>();
    private OperatingSystem os;
    private Region region;

    private List<PricingTier> pricingTierList = new ArrayList<>(PricingTier.WEB_APP_PRICING);
    private PricingTier defaultPricingTier = PricingTier.BASIC_B2;

    private Predicate<AppServicePlanEntity> servicePlanFilter;

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    public void setOperatingSystem(OperatingSystem os) {
        if (os == this.os) {
            return;
        }
        this.os = os;
        if (os == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    public void setRegion(Region region) {
        this.region = region;
        if (region == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    public void setValidPricingTierList(@NotNull final List<PricingTier> pricingTierList, @NotNull final PricingTier defaultPricingTier) {
        this.pricingTierList = pricingTierList;
        this.defaultPricingTier = defaultPricingTier;
        this.servicePlanFilter = appServicePlan -> pricingTierList.contains(appServicePlan.getPricingTier());
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        if (item instanceof Draft) {
            return "(New) " + ((AppServicePlanEntity) item).getName();
        }
        return ((AppServicePlanEntity) item).getName();
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "appservice|plan.list.subscription|region|os",
        params = {"this.subscription.getId()", "this.region.getName()", "this.os.name()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends AppServicePlanEntity> loadItems() throws Exception {
        final List<AppServicePlanEntity> plans = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            if (CollectionUtils.isNotEmpty(this.localItems)) {
                plans.addAll(this.localItems.stream()
                    .filter(p -> this.subscription.equals(p.getSubscription()))
                    .collect(Collectors.toList()));
            }
            final List<AppServicePlanEntity> remotePlans = Azure.az(AzureAppService.class)
                .subscription(subscription.getId()).appServicePlans().stream().map(IAppServicePlan::entity)
                .collect(Collectors.toList());
            plans.addAll(remotePlans);
            Stream<AppServicePlanEntity> stream = plans.stream();
            if (Objects.nonNull(this.region)) {
                stream = stream.filter(p -> Objects.equals(p.getRegion(), this.region.getLabel()));
            }
            if (Objects.nonNull(this.os)) {
                stream = stream.filter(p -> p.getOperatingSystem() == this.os);
            }
            if (Objects.nonNull(this.servicePlanFilter)) {
                stream = stream.filter(servicePlanFilter);
            }
            stream = stream.sorted((first, second) -> StringUtils.compare(first.getName(), second.getName()));
            return stream.collect(Collectors.toList());
        }
        return plans;
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
            AllIcons.General.Add, message("appService.servicePlan.create.tooltip"), this::showServicePlanCreationPopup);
    }

    private void showServicePlanCreationPopup() {
        final ServicePlanCreationDialog dialog = new ServicePlanCreationDialog(this.subscription, this.os, this.region, pricingTierList, defaultPricingTier);
        dialog.setOkActionListener((plan) -> {
            this.localItems.add(0, plan);
            dialog.close();
            final List<AppServicePlanEntity> items = this.getItems();
            items.add(0, plan);
            this.setItems(items);
            this.setValue(plan);
        });
        dialog.show();
    }
}
