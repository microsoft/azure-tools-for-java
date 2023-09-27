/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.left;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class WorkspaceComboBox extends AzureComboBox<LogAnalyticsWorkspaceConfig> {
    private Subscription subscription;
    private Region region;
    private final List<LogAnalyticsWorkspaceConfig> draftItems = new LinkedList<>();

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
        final String defaultWorkspaceName = String.format("DefaultWorkspace-%s-%s", subscription.getId(), region.getAbbreviation());
        final String finalWorkspaceName = defaultWorkspaceName.length() > 63 ? defaultWorkspaceName.substring(0, 63) : defaultWorkspaceName;
        final Optional<LogAnalyticsWorkspaceConfig> item = this.getItems().stream()
                .filter(config -> Objects.equals(config.getName(), finalWorkspaceName)).findFirst();
        item.ifPresentOrElse(
                (value) -> {
                    this.draftItems.clear();
                    this.reloadItems();
                    this.setValue(value);},
                () -> this.setValue(LogAnalyticsWorkspaceConfig.builder().newCreate(true)
                        .name(finalWorkspaceName).subscriptionId(subscription.getId()).regionName(region.getName()).build()));
    }

    @Nullable
    @Override
    protected LogAnalyticsWorkspaceConfig doGetDefaultValue() {
        return CacheManager.getUsageHistory(LogAnalyticsWorkspaceConfig.class)
                .peek(v -> (!v.isNewCreate() && Objects.equals(subscription.getId(), v.getSubscriptionId())));
    }

    @Override
    public void setValue(LogAnalyticsWorkspaceConfig val) {
        if (Objects.nonNull(val) && val.isNewCreate()) {
            this.draftItems.clear();
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val);
    }

    @Override
    protected String getItemText(Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        final LogAnalyticsWorkspaceConfig workspace = (LogAnalyticsWorkspaceConfig) item;
        if (workspace.isNewCreate()) {
            return String.format("(New) [%s] %s", workspace.getRegionName(), workspace.getName());
        }
        return String.format("[%s] %s", workspace.getRegionName(), workspace.getName());
    }

    @Nonnull
    @Override
    protected List<LogAnalyticsWorkspaceConfig> loadItems() {
        final List<LogAnalyticsWorkspaceConfig> workspaces = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            if (CollectionUtils.isNotEmpty(this.draftItems)) {
                workspaces.addAll(this.draftItems.stream()
                        .filter(p -> Objects.equals(subscription.getId(), p.getSubscriptionId())).collect(Collectors.toList()));
            }
            final List<LogAnalyticsWorkspaceConfig> remoteWorkspaces = Azure.az(AzureLogAnalyticsWorkspace.class)
                    .logAnalyticsWorkspaces(subscription.getId()).list()
                    .stream().sorted(Comparator.comparing(LogAnalyticsWorkspace::getName)).map(workspace ->
                            LogAnalyticsWorkspaceConfig.builder()
                                    .newCreate(false)
                                    .subscriptionId(subscription.getId())
                                    .resourceId(workspace.getId())
                                    .name(workspace.getName())
                                    .regionName(Optional.ofNullable(workspace.getRegion()).map(Region::getName).orElse(StringUtils.EMPTY))
                                    .build()).collect(Collectors.toList());
            workspaces.addAll(remoteWorkspaces);
            return workspaces;
        }
        return workspaces;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureLogAnalyticsWorkspace.class)
                .logAnalyticsWorkspaces(s.getId()).refresh());
        super.refreshItems();
    }
}
