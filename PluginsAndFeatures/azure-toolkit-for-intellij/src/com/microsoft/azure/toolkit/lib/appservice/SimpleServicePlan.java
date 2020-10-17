package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.appservice.implementation.AppServicePlanInner;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.lib.common.OperationNotSupportedException;
import lombok.Builder;
import lombok.Setter;
import rx.Observable;

import java.util.Map;

@Setter
@Builder
public class SimpleServicePlan implements AppServicePlan {

    private String name;
    private Region region;
    private OperatingSystem os;
    private PricingTier tier;

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Region region() {
        return this.region;
    }

    @Override
    public PricingTier pricingTier() {
        return this.tier;
    }

    @Override
    public OperatingSystem operatingSystem() {
        return this.os;
    }

    @Override
    public String resourceGroupName() {
        throw new OperationNotSupportedException();
    }

    @Override
    public int maxInstances() {
        throw new OperationNotSupportedException();
    }

    @Override
    public int capacity() {
        throw new OperationNotSupportedException();
    }

    @Override
    public boolean perSiteScaling() {
        throw new OperationNotSupportedException();
    }

    @Override
    public int numberOfWebApps() {
        throw new OperationNotSupportedException();
    }

    @Override
    public AppServiceManager manager() {
        throw new OperationNotSupportedException();
    }

    @Override
    public String type() {
        throw new OperationNotSupportedException();
    }

    @Override
    public String regionName() {
        throw new OperationNotSupportedException();
    }

    @Override
    public Map<String, String> tags() {
        throw new OperationNotSupportedException();
    }

    @Override
    public String id() {
        throw new OperationNotSupportedException();
    }

    @Override
    public AppServicePlanInner inner() {
        throw new OperationNotSupportedException();
    }

    @Override
    public String key() {
        throw new OperationNotSupportedException();
    }

    @Override
    public AppServicePlan refresh() {
        throw new OperationNotSupportedException();
    }

    @Override
    public Observable<AppServicePlan> refreshAsync() {
        throw new OperationNotSupportedException();
    }

    @Override
    public Update update() {
        throw new OperationNotSupportedException();
    }
}
