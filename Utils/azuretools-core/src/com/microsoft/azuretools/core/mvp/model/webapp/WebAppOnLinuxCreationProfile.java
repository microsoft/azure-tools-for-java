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
 *
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

public class WebAppOnLinuxCreationProfile {
    private String webAppName;
    private boolean resourceGroupExisting;
    private String resourceGroupName;
    private String regionName;
    private String pricingTierSkuTier;
    private String pricingTierSkuSize;

    public WebAppOnLinuxCreationProfile(String webAppName, boolean resourceGroupExisting, String resourceGroupName, String regionName, String pricingTierSkuTier, String pricingTierSkuSize) {
        this.webAppName = webAppName;
        this.resourceGroupExisting = resourceGroupExisting;
        this.resourceGroupName = resourceGroupName;
        this.regionName = regionName;
        this.pricingTierSkuTier = pricingTierSkuTier;
        this.pricingTierSkuSize = pricingTierSkuSize;
    }

    public String getWebAppName() {
        return webAppName;
    }

    public boolean isResourceGroupExisting() {
        return resourceGroupExisting;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getPricingTierSkuTier() {
        return pricingTierSkuTier;
    }

    public String getPricingTierSkuSize() {
        return pricingTierSkuSize;
    }
}
