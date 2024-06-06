/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class ServicePlanComboBox extends AzureComboBox<AppServicePlan> {

    private Subscription subscription;
    private final List<AppServicePlan> draftItems = new LinkedList<>();
    private OperatingSystem os;
    private Region region;
    @Setter
    private ResourceGroup resourceGroup;

    private List<PricingTier> pricingTierList = new ArrayList<>(PricingTier.WEB_APP_PRICING);
    private PricingTier defaultPricingTier = PricingTier.BASIC_B2;

    private Predicate<AppServicePlan> servicePlanFilter;

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        // Clean up app service plan cache when switch subscription
        // todo: leverage event hub to update resource cache automatically
        try {
            CacheManager.evictCache("appservice/{}/plans", subscription.getId());
        } catch (final ExecutionException ignored) {
            // swallow exception while clean up cache
        }
        this.reloadItems();
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
        this.reloadItems();
    }

    public void setRegion(Region region) {
        if (region == this.region) {
            return;
        }
        this.region = region;
        if (region == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(@Nullable AppServicePlan val, Boolean fixed) {
        if (Objects.nonNull(val) && val.isDraftForCreating()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nullable
    @Override
    protected AppServicePlan doGetDefaultValue() {
        return CacheManager.getUsageHistory(AppServicePlan.class)
            .peek(v -> (Objects.isNull(subscription) || Objects.equals(subscription, v.getSubscription()) &&
                (Objects.isNull(region) || Objects.equals(region, v.getRegion())) &&
                (Objects.isNull(os) || os == v.getOperatingSystem())));
    }

    public void setValidPricingTierList(@Nonnull final List<PricingTier> pricingTierList, @Nonnull final PricingTier defaultPricingTier) {
        this.pricingTierList = pricingTierList;
        this.defaultPricingTier = defaultPricingTier;
        this.servicePlanFilter = appServicePlan -> pricingTierList.contains(appServicePlan.getPricingTier());
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        final AppServicePlan plan = (AppServicePlan) item;
        if (plan.isDraftForCreating()) {
            return "(New) " + plan.getName();
        }
        return plan.getName();
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/$appservice.list_plans.subscription|region|os", params = {"this.subscription.getId()", "this.region.getName()", "this.os.name()"})
    protected List<AppServicePlan> loadItems() {
        final List<AppServicePlan> plans = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            if (CollectionUtils.isNotEmpty(this.draftItems)) {
                plans.addAll(this.draftItems.stream()
                    .filter(p -> this.subscription.equals(p.getSubscription()))
                    .collect(Collectors.toList()));
            }
            final List<AppServicePlan> remotePlans = Azure.az(AzureAppService.class).plans(subscription.getId()).list()
                .stream().sorted((first, second) -> StringUtils.compare(first.getName(), second.getName())).toList();
            plans.addAll(remotePlans);
            Stream<AppServicePlan> stream = plans.stream();
            if (Objects.nonNull(this.region)) {
                stream = stream.filter(p -> p instanceof AppServicePlanDraft || Objects.equals(p.getRegion(), this.region));
            }
            if (Objects.nonNull(this.os)) {
                stream = stream.filter(p -> p instanceof AppServicePlanDraft || p.getOperatingSystem() == this.os);
            }
            if (Objects.nonNull(this.servicePlanFilter)) {
                stream = stream.filter(servicePlanFilter);
            }
            return stream.collect(Collectors.toList());
        }
        return plans;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureAppService.class).plans(s.getId()).refresh());
        super.refreshItems();
    }

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("%s (%s)", message("appService.servicePlan.create.tooltip"), KeymapUtil.getKeystrokeText(keyStroke));
        final Extension addEx = Extension.create(AllIcons.General.Add, tooltip, this::showServicePlanCreationPopup);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void showServicePlanCreationPopup() {
        final ServicePlanCreationDialog dialog = new ServicePlanCreationDialog(this.subscription, this.resourceGroup, pricingTierList, defaultPricingTier);
        final Action.Id<AppServicePlanDraft> actionId = Action.Id.of("user/$appservice.create_service_plan.plan");
        dialog.setOkAction(new Action<>(actionId)
            .withLabel("Create")
            .withIdParam(AbstractAzResource::getName)
            .withSource(s -> s)
            .withAuthRequired(false)
            .withHandler(plan -> {
                plan.setRegion(region);
                plan.setOperatingSystem(os);
                this.setValue(plan);
            }));
        dialog.show();
    }
}
