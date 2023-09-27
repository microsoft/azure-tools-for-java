package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceDraft;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AzureContainerAppsEnvironmentComboBox extends AzureComboBox<ContainerAppsEnvironment> {
    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private Region region;
    private final List<ContainerAppsEnvironment> draftItems = new LinkedList<>();

    @Override
    public String getLabel() {
        return "Container Apps Environment";
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof ContainerAppsEnvironment) {
            final ContainerAppsEnvironment environment = (ContainerAppsEnvironment) item;
            return environment.isDraftForCreating() ? "(New) " + environment.getName() : environment.getName();
        }
        return super.getItemText(item);
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    public void setRegion(Region region) {
        if (Objects.equals(region, this.region)) {
            return;
        }
        this.region = region;
        if (region == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    public void setResourceGroup(ResourceGroup resourceGroup) {
        if (Objects.equals(resourceGroup, this.region)) {
            return;
        }
        this.resourceGroup = resourceGroup;
        if (resourceGroup == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(@Nullable ContainerAppsEnvironment val, Boolean fixed) {
        if (Objects.nonNull(val) && val.isDraftForCreating() && !val.exists()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nullable
    @Override
    protected ContainerAppsEnvironment doGetDefaultValue() {
        return CacheManager.getUsageHistory(ContainerAppsEnvironment.class)
                .peek(g -> Objects.isNull(subscription) || Objects.equals(subscription.getId(), g.getSubscriptionId()));
    }

    @Nonnull
    @Override
    protected List<? extends ContainerAppsEnvironment> loadItems() {
        Stream<AzureContainerAppsServiceSubscription> stream = Azure.az(AzureContainerApps.class).list().stream();
        if (Objects.nonNull(this.subscription)) {
            stream = stream.filter(s -> s.getSubscriptionId().equalsIgnoreCase(this.subscription.getId()));
        }
        final List<ContainerAppsEnvironment> remoteEnvironments = stream.flatMap(s -> s.environments().list().stream())
                .filter(env -> env.getFormalStatus().isConnected())
                .filter(env -> Objects.equals(env.getRegion(), this.region))
                .sorted(Comparator.comparing(ContainerAppsEnvironment::getName)).collect(Collectors.toList());
        final List<ContainerAppsEnvironment> environments = new ArrayList<>(remoteEnvironments);
        final ContainerAppsEnvironment draftItem = this.draftItems.stream()
                .filter(i -> StringUtils.equalsIgnoreCase(this.subscription.getId(), i.getSubscriptionId()) &&
                        Objects.equals(i.getRegion(), this.region))
                .filter(i -> !remoteEnvironments.contains(i)) // filter out the draft item which has been created
                .findFirst().orElseGet(this::getEnvironmentDraft);
        if (Objects.nonNull(draftItem)) {
            if (CollectionUtils.isEmpty(environments) || (getValue() != null && getValue().isDraftForCreating())) {
                super.setValue(draftItem);
            }
            environments.add(draftItem);
        }
        return environments;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureContainerApps.class).containerApps(s.getId()).refresh());
        super.refreshItems();
    }

    @Nullable
    private ContainerAppsEnvironmentDraft getEnvironmentDraft() {
        if (ObjectUtils.anyNull(region, resourceGroup, subscription)) {
            return null;
        }
        final String defaultEnvironmentName = Utils.generateRandomResourceName(String.format("ace-%s", resourceGroup.getName()), 32);
        final ContainerAppsEnvironmentDraft result = Azure.az(AzureContainerApps.class).environments(subscription.getId()).create(defaultEnvironmentName, resourceGroup.getName());
        final ContainerAppsEnvironmentDraft.Config config = new ContainerAppsEnvironmentDraft.Config();
        config.setName(defaultEnvironmentName);
        config.setSubscription(subscription);
        config.setRegion(region);
        config.setResourceGroup(resourceGroup);
        config.setLogAnalyticsWorkspace(getWorkspaceDraft());
        result.setConfig(config);
        return result;
    }

    private LogAnalyticsWorkspaceDraft getWorkspaceDraft() {
        if (ObjectUtils.anyNull(region, resourceGroup, subscription)) {
            return null;
        }
        final String defaultName = Utils.generateRandomResourceName(String.format("workspace-%s", resourceGroup.getName()), 63);
        final LogAnalyticsWorkspaceDraft result = Azure.az(AzureLogAnalyticsWorkspace.class).logAnalyticsWorkspaces(subscription.getId()).create(defaultName, resourceGroup.getName());
        result.setRegion(region);
        return result;
    }
}
