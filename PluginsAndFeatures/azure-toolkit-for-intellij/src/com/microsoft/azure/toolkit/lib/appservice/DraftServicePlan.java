/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.appservice.implementation.AppServicePlanInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.lib.common.OperationNotSupportedException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rx.Observable;

import java.util.Map;

@Setter
@Builder
public class DraftServicePlan implements AppServicePlan, Draft {

    @Getter
    private Subscription subscription;
    private String name;
    private Region region;
    private OperatingSystem os;
    private PricingTier tier;

    @Override
    public @Nullable String id() {
        return null;
    }

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
        return null;
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
