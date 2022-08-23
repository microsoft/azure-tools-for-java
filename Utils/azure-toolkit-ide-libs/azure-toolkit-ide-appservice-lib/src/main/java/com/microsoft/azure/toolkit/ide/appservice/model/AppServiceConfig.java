/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.model;

import com.azure.core.management.AzureEnvironment;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class AppServiceConfig {
    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");
    protected static final int RG_NAME_MAX_LENGTH = 90;
    protected static final int SP_NAME_MAX_LENGTH = 40;
    @Builder.Default
    private MonitorConfig monitorConfig = MonitorConfig.builder().build();
    private String name;
    private String resourceId;
    private Path application;
    private Subscription subscription;
    private ResourceGroupConfig resourceGroup;
    private AppServicePlanConfig servicePlan;
    private Region region;
    private PricingTier pricingTier;
    @Builder.Default
    private Set<String> appSettingsToRemove = new HashSet<>();
    @Builder.Default
    private Map<String, String> appSettings = new HashMap<>();
    private DeploymentSlotConfig deploymentSlot;

    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> result = new HashMap<>();
        result.put("subscriptionId", Optional.ofNullable(subscription).map(Subscription::getId).orElse(StringUtils.EMPTY));
        result.put("region", Optional.ofNullable(region).map(Region::getName).orElse(StringUtils.EMPTY));
        final PricingTier pricingTier = Optional.ofNullable(getServicePlan()).map(AppServicePlanConfig::getPricingTier).orElse(getPricingTier());
        result.put("pricingTier", Optional.ofNullable(pricingTier).map(PricingTier::getSize).orElse(StringUtils.EMPTY));
        return result;
    }

    public static Region getDefaultRegion() {
        final AzureEnvironment environment = Azure.az(AzureAccount.class).account().getEnvironment();
        if (environment == AzureEnvironment.AZURE) {
            return Region.US_SOUTH_CENTRAL;
        } else if (environment == AzureEnvironment.AZURE_CHINA) {
            return Region.CHINA_NORTH2;
        } else {
            return Azure.az(AzureAccount.class).listRegions().stream().findFirst().orElse(null);
        }
    }

    public String getResourceGroupName() {
        return Optional.ofNullable(resourceGroup).map(ResourceGroupConfig::getName).orElse(StringUtils.EMPTY);
    }

    public String getSubscriptionId() {
        return Optional.ofNullable(subscription).map(Subscription::getId).orElse(StringUtils.EMPTY);
    }

    public abstract Runtime getRuntime();

    public abstract void setRuntime(Runtime runtime);

    public static boolean isSameApp(AppServiceConfig first, AppServiceConfig second) {
        if (Objects.isNull(first) || Objects.isNull(second)) {
            return first == second;
        }
        return StringUtils.equalsIgnoreCase(first.resourceId, second.resourceId) ||
            (StringUtils.equalsIgnoreCase(first.name, second.name) &&
                StringUtils.equalsIgnoreCase(first.getResourceGroupName(), second.getResourceGroupName()) &&
                StringUtils.equalsIgnoreCase(first.getSubscriptionId(), second.getSubscriptionId()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AppServiceConfig that = (AppServiceConfig) o;

        final ResourceId thisId = Optional.ofNullable(this.resourceId).map(ResourceId::fromString).orElse(null);
        final String thisName = Optional.ofNullable(thisId).map(ResourceId::name).orElse(this.getName());
        final String thisRg = Optional.ofNullable(thisId).map(ResourceId::resourceGroupName).orElse(this.getResourceGroupName());
        final String thisSub = Optional.ofNullable(thisId).map(ResourceId::subscriptionId).orElse(this.getSubscriptionId());

        final ResourceId thatId = Optional.ofNullable(that.resourceId).map(ResourceId::fromString).orElse(null);
        final String thatName = Optional.ofNullable(thatId).map(ResourceId::name).orElse(that.getName());
        final String thatRg = Optional.ofNullable(thatId).map(ResourceId::resourceGroupName).orElse(that.getResourceGroupName());
        final String thatSub = Optional.ofNullable(thatId).map(ResourceId::subscriptionId).orElse(that.getSubscriptionId());

        return new EqualsBuilder().append(thisSub, thatSub).append(thisName, thatName).append(thisRg, thatRg).isEquals();
    }

    @Override
    public int hashCode() {
        final ResourceId thisId = Optional.ofNullable(this.resourceId).map(ResourceId::fromString).orElse(null);
        final String thisName = Optional.ofNullable(thisId).map(ResourceId::name).orElse(this.getName());
        final String thisRg = Optional.ofNullable(thisId).map(ResourceId::resourceGroupName).orElse(this.getResourceGroupName());
        final String thisSub = Optional.ofNullable(thisId).map(ResourceId::subscriptionId).orElse(this.getSubscriptionId());
        return new HashCodeBuilder(17, 37).append(thisSub).append(thisRg).append(thisName).toHashCode();
    }
}
