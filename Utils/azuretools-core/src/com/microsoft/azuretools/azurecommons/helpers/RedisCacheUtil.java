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

package com.microsoft.azuretools.azurecommons.helpers;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.exceptions.InvalidFormDataException;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.RedisCacheCreator;

import java.util.LinkedHashMap;

public final class RedisCacheUtil {

    // All kinds of Redis Cache Skus
    private static final String C0_BASIC_SKU = "C0 Basic 250MB/Low network band/Shared infrastructure/256 Connections";
    private static final String C1_BASIC_SKU = "C1 Basic 1GB/Low network band/Dedicated service/1,000 Connections";
    private static final String C2_BASIC_SKU = "C2 Basic 2.5GB/Moderate network band/Dedicated infrastructure/2,000 Connections";
    private static final String C3_BASIC_SKU = "C3 Basic 6GB/Moderate network band/Dedicated infrastructure/5,000 Connections";
    private static final String C4_BASIC_SKU = "C4 Basic 13GB/Moderate network band/Dedicated infrastructure/10,000 Connections";
    private static final String C5_BASIC_SKU = "C5 Basic 26GB/High network band/Dedicated infrastructure/15,000 Connections";
    private static final String C6_BASIC_SKU = "C6 Basic 53GB/Highest network band/Dedicated infrastructure/20,000 Connections";
    private static final String C0_STANDARD_SKU = "C0 Standard 250MB/Replication/Low network band/Shared infrastructure/256 Connections";
    private static final String C1_STANDARD_SKU = "C1 Standard 1GB/Replication/Low network band/Dedicated service/1,000 Connections";
    private static final String C2_STANDARD_SKU = "C2 Standard 2.5MB/Replication/Moderate network band/Dedicated service/2,000 Connections";
    private static final String C3_STANDARD_SKU = "C3 Standard 6GB/Replication/Moderate network band/Dedicated service/5,000 Connections";
    private static final String C4_STANDARD_SKU = "C4 Standard 13GB/Replication/Moderate network band/Dedicated service/10,000 Connections";
    private static final String C5_STANDARD_SKU = "C5 Standard 26GB/Replication/High network band/Dedicated service/15,000 Connections";
    private static final String C6_STANDARD_SKU = "C6 Standard 53GB/Replication/High network band/Dedicated service/20,000 Connections";
    private static final String P1_PREMIUM_SKU = "P1 Premium 6GB/Replication/Moderate network band/All Standard features/Data Persist/Virtual Net/Cluster";
    private static final String P2_PREMIUM_SKU = "P2 Premium 13GB/Replication/Moderate network band/All Standard features/Data Persist/Virtual Net/Cluster";
    private static final String P3_PREMIUM_SKU = "P3 Premium 26GB/Replication/High network band/All Standard features/Data Persist/Virtual Net/Cluster";
    private static final String P4_PREMIUM_SKU = "P4 Premium 53GB/Replication/Highest network band/All Standard features/Data Persist/Virtual Net/Cluster";

    //Alert Messages
    private static final String REQUIRE_SUBSCRIPTION = "Select one subscription for Redis cache creation.";
    private static final String INVALID_REDIS_CACHE_NAME = "Invalid Redis Cache name. The name can only contain letters, numbers and hyphens. "
            + "The first and last characters must each be a letter or a number. Consecutive hyphens are not allowed.";
    private static final String REQUIRE_LOCATION = "Location cannot be null or empty.";
    private static final String REQUIRE_RESOURCE_GROUP = "Resource group cannot be null or empty.";
    private static final String REQUIRE_PRICE_TIER = "Pricing tier cannot be null or empty.";

    public static LinkedHashMap<String, String> initSkus() {
        LinkedHashMap<String, String> skus = new LinkedHashMap<String, String>();
        skus.put(C0_BASIC_SKU, "BASIC0");
        skus.put(C1_BASIC_SKU, "BASIC1");
        skus.put(C2_BASIC_SKU, "BASIC2");
        skus.put(C3_BASIC_SKU, "BASIC3");
        skus.put(C4_BASIC_SKU, "BASIC4");
        skus.put(C5_BASIC_SKU, "BASIC5");
        skus.put(C6_BASIC_SKU, "BASIC6");
        skus.put(C0_STANDARD_SKU, "STD0");
        skus.put(C1_STANDARD_SKU, "STD1");
        skus.put(C2_STANDARD_SKU, "STD2");
        skus.put(C3_STANDARD_SKU, "STD3");
        skus.put(C4_STANDARD_SKU, "STD4");
        skus.put(C5_STANDARD_SKU, "STD5");
        skus.put(C6_STANDARD_SKU, "STD6");
        skus.put(P1_PREMIUM_SKU, "PREMIUM1");
        skus.put(P2_PREMIUM_SKU, "PREMIUM2");
        skus.put(P3_PREMIUM_SKU, "PREMIUM3");
        skus.put(P4_PREMIUM_SKU, "PREMIUM4");
        return skus;
    }

    public static void doValidate(SubscriptionDetail currentSub, String dnsNameValue,
            String selectedRegionValue, String selectedResGrpValue, String selectedPriceTierValue) throws InvalidFormDataException {
        if (currentSub == null) {
            throw new InvalidFormDataException(REQUIRE_SUBSCRIPTION);
        }
        if (dnsNameValue == null || dnsNameValue.isEmpty() || !dnsNameValue.matches("^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$") || dnsNameValue.length() > 63) {
            throw new InvalidFormDataException(INVALID_REDIS_CACHE_NAME);
        }
        if (selectedRegionValue == null || selectedRegionValue.isEmpty()) {
            throw new InvalidFormDataException(REQUIRE_LOCATION);
        }
        if (selectedResGrpValue == null || selectedResGrpValue.isEmpty()) {
            throw new InvalidFormDataException(REQUIRE_RESOURCE_GROUP);
        }
        if (selectedPriceTierValue == null || selectedPriceTierValue.isEmpty()) {
            throw new InvalidFormDataException(REQUIRE_PRICE_TIER);
        }
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(currentSub.getSubscriptionId());
        for (final RedisCache existingRedisCache : azure.redisCaches().list()) {
            if (existingRedisCache.name().equals(dnsNameValue)) {
                throw new InvalidFormDataException("The name " + dnsNameValue + " is not available");
            }
        }
    }

    public static ProcessingStrategy doGetProcessor(Azure azure,
            LinkedHashMap<String, String> skus,
            String dnsNameValue, String selectedRegionValue,
            String selectedResGrpValue,
            String selectedPriceTierValue,
            boolean noSSLPort,
            boolean newResGrp) {
        if (azure != null) {
            RedisCacheCreator creator = new RedisCacheCreator(azure.redisCaches(),
                    dnsNameValue,
                    selectedRegionValue,
                    selectedResGrpValue
                    );
            if (noSSLPort) {
                if (newResGrp && canCreateNewResGrp(azure, selectedResGrpValue)) {
                    //e.g. BASIC0NewNoSSL
                    return creator.CreatorMap().get(skus.get(selectedPriceTierValue) + "NewNoSSL");
                } else {
                    return creator.CreatorMap().get(skus.get(selectedPriceTierValue) + "ExistingNoSSL");
                }
            } else {
                if (newResGrp && canCreateNewResGrp(azure, selectedResGrpValue)) {
                    //e.g. BASIC0NewNoSSL
                    return creator.CreatorMap().get(skus.get(selectedPriceTierValue) + "New");
                } else {
                    return creator.CreatorMap().get(skus.get(selectedPriceTierValue) + "Existing");
                }
            }
        }
        return null;
    }

    public static boolean canCreateNewResGrp(Azure azure, String resGrpName) {
        return (!azure.resourceGroups().checkExistence(resGrpName));
    }
}
