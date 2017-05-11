/**
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.azurecommons.helpers;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.RedisCacheCreator;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import javax.swing.*;
import java.util.LinkedHashMap;

public final class RedisCacheUtil {

    public static LinkedHashMap<String, String> initSkus() {
    	LinkedHashMap<String, String> skus = new LinkedHashMap<String, String>();
        skus.put("C0 Basic 250MB/Low network band/Shared infrastructure/256 Connections", "BASIC0");
        skus.put("C1 Basic 1GB/Low network band/Dedicated service/1,000 Connections", "BASIC1");
        skus.put("C2 Basic 2.5GB/Moderate network band/Dedicated infrastructure/2,000 Connections", "BASIC2");
        skus.put("C3 Basic 6GB/Moderate network band/Dedicated infrastructure/5,000 Connections", "BASIC3");
        skus.put("C4 Basic 13GB/Moderate network band/Dedicated infrastructure/10,000 Connections", "BASIC4");
        skus.put("C5 Basic 26GB/High network band/Dedicated infrastructure/15,000 Connections", "BASIC5");
        skus.put("C6 Basic 53GB/Highest network band/Dedicated infrastructure/20,000 Connections", "BASIC6");
        skus.put("C0 Standard 250MB/Replication/Low network band/Shared infrastructure/256 Connections", "STD0");
        skus.put("C1 Standard 1GB/Replication/Low network band/Dedicated service/1,000 Connections", "STD1");
        skus.put("C2 Standard 2.5MB/Replication/Moderate network band/Dedicated service/2,000 Connections", "STD2");
        skus.put("C3 Standard 6GB/Replication/Moderate network band/Dedicated service/5,000 Connections", "STD3");
        skus.put("C4 Standard 13GB/Replication/Moderate network band/Dedicated service/10,000 Connections", "STD4");
        skus.put("C5 Standard 26GB/Replication/High network band/Dedicated service/15,000 Connections", "STD5");
        skus.put("C6 Standard 53GB/Replication/High network band/Dedicated service/20,000 Connections", "STD6");
        skus.put("P1 Premium 6GB/Replication/Moderate network band/All Standard features/Data Persist/Virtual Net/Cluster", "PREMIUM1");
        skus.put("P2 Premium 13GB/Replication/Moderate network band/All Standard features/Data Persist/Virtual Net/Cluster", "PREMIUM2");
        skus.put("P3 Premium 26GB/Replication/High network band/All Standard features/Data Persist/Virtual Net/Cluster", "PREMIUM3");
        skus.put("P4 Premium 53GB/Replication/Highest network band/All Standard features/Data Persist/Virtual Net/Cluster", "PREMIUM4");
        return skus;
    }

    public static boolean doValidate(AzureManager azureManager, SubscriptionDetail currentSub, String dnsNameValue, String selectedRegionValue, String selectedResGrpValue, String selectedPriceTierValue) {
    	if (currentSub == null) {
    		JOptionPane.showMessageDialog(null, "Select one subscription for Redis cache creation", "Alert", JOptionPane.ERROR_MESSAGE, null);
    		return false;
    	}
    	if (dnsNameValue == null || dnsNameValue.isEmpty() || !dnsNameValue.matches("^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$")) {
    		JOptionPane.showMessageDialog(null, "Invalid Redis Cache name. The name can only contain letters, numbers and hyphens. The first and last characters must each be a letter or a number. Consecutive hyphens are not allowed.", "Alert", JOptionPane.ERROR_MESSAGE, null);
    		return false;
    	}
    	if (selectedRegionValue == null || selectedRegionValue.isEmpty()) {
    		JOptionPane.showMessageDialog(null, "Location cannot be null or empty", "Alert", JOptionPane.ERROR_MESSAGE, null);
    		return false;
    	}
    	if (selectedResGrpValue == null || selectedResGrpValue.isEmpty()) {
    		JOptionPane.showMessageDialog(null, "Resource group cannot be null or empty", "Alert", JOptionPane.ERROR_MESSAGE, null);
    		return false;
    	}
    	if (selectedPriceTierValue == null || selectedPriceTierValue.isEmpty()) {
    		JOptionPane.showMessageDialog(null, "Pricing tier cannot be null or empty", "Alert", JOptionPane.ERROR_MESSAGE, null);
    		return false;
    	}
    	try {
        	for (RedisCache existingRedisCache : azureManager.getAzure(currentSub.getSubscriptionId()).redisCaches().list()) {
        		if (existingRedisCache.name().equals(dnsNameValue)) {
        			JOptionPane.showMessageDialog(null, "The name " + dnsNameValue + " is not available", "Alert", JOptionPane.ERROR_MESSAGE, null);
        			return false;
        		}
            }
        	return true;
    	} catch (Exception ex) {
            ex.printStackTrace();
        }
		return false;
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
    		if(noSSLPort) {
    			if(newResGrp && canCreateNewResGrp(azure, selectedResGrpValue)) {
    				//e.g. BASIC0NewNoSSL
    				return creator.CreatorMap().get(skus.get(selectedPriceTierValue) + "NewNoSSL");
    			} else {
        			return creator.CreatorMap().get(skus.get(selectedPriceTierValue) + "ExistingNoSSL");
        		}
        	} else {
    			if(newResGrp && canCreateNewResGrp(azure, selectedResGrpValue)) {
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
