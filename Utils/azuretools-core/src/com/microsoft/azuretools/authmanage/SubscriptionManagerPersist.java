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

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Tenant;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.Pair;
import lombok.extern.java.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Log
public class SubscriptionManagerPersist extends SubscriptionManager {

    public SubscriptionManagerPersist(AzureManager azureManager) {
        super(azureManager);
    }

    @Override
    public void setSubscriptionDetails(List<SubscriptionDetail> subscriptionDetails) {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManagerPersist.setSubscriptionDetails()");
        synchronized (this) {
            final String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
            try {
                saveSubscriptions(subscriptionDetails, subscriptionsDetailsFileName);
            } catch (final IOException e) {
                final String error = "Failed to update local subscriptions cache while updating";
                final String action = "Retry later";
                throw new AzureToolkitRuntimeException(error, e, action);
            }
        }
        super.setSubscriptionDetails(subscriptionDetails);
    }

    @Override
    protected List<SubscriptionDetail> updateAccountSubscriptionList() {
        System.out.println(Thread.currentThread().getId()
                + "SubscriptionManagerPersist.updateAccountSubscriptionList()");
        List<SubscriptionDetail> sdl = null;
        synchronized (this) {
            String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
            sdl = loadSubscriptions(subscriptionsDetailsFileName);
        }

        if (sdl.isEmpty()) {
            return super.updateAccountSubscriptionList();
        }

        // Filter available SubscriptionDetail
        Map<String, SubscriptionDetail> sdmap = new HashMap<>();
        for (SubscriptionDetail sd : sdl) {
            sdmap.put(sd.getSubscriptionId(), sd);
        }

        List<SubscriptionDetail> ret = new ArrayList<>();
        subscriptionIdToSubscriptionMap.clear();
        List<Pair<Subscription, Tenant>> stpl = azureManager.getSubscriptionsWithTenant();
        for (Pair<Subscription, Tenant> stp : stpl) {
            String sid = stp.first().subscriptionId();
            boolean isSelected = (sdmap.get(sid) != null && sdmap.get(sid).isSelected());
            ret.add(new SubscriptionDetail(
                    stp.first().subscriptionId(),
                    stp.first().displayName(),
                    stp.second().tenantId(),
                    isSelected));
            subscriptionIdToSubscriptionMap.put(stp.first().subscriptionId(), stp.first());
        }
        return ret;
    }

    @Override
    public synchronized void cleanSubscriptions() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManagerPersist.cleanSubscriptions()");
        String subscriptionsDetailsFileName = azureManager.getSettings().getSubscriptionsDetailsFileName();
        deleteSubscriptions(subscriptionsDetailsFileName);
        super.cleanSubscriptions();
    }

    public static synchronized void deleteSubscriptions(String subscriptionsDetailsFileName) {
        System.out.println("cleaning " + subscriptionsDetailsFileName + " file");
        FileStorage fs = null;
        try {
            fs = new FileStorage(subscriptionsDetailsFileName, CommonSettings.getSettingsBaseDir());
        } catch (final IOException e) {
            log.warning(subscriptionsDetailsFileName + " is not found when try to clean subscriptions");
        }
        if (Objects.nonNull(fs)) {
            try {
                fs.cleanFile();
            } catch (final IOException e) {
                final String error = "Failed to clear local cached subscriptions";
                throw new AzureToolkitRuntimeException(error, e);
            }
        }
    }

    private static List<SubscriptionDetail> loadSubscriptions(String subscriptionsDetailsFileName) {
        System.out.println("SubscriptionManagerPersist.loadSubscriptions()");

        //subscriptionDetails.clear();
        try {
            final FileStorage file = new FileStorage(subscriptionsDetailsFileName, CommonSettings.getSettingsBaseDir());
            final byte[] data = file.read();
            final String json = new String(data, StandardCharsets.UTF_8);
            if (json.isEmpty()) {
                System.out.println(subscriptionsDetailsFileName + " file is empty");
                return Collections.emptyList();
            }
            final SubscriptionDetail[] sda = JsonHelper.deserialize(SubscriptionDetail[].class, json);
            return new ArrayList<>(Arrays.asList(sda));
        } catch (final IOException e) {
            final String error = "Failed to load local cached subscriptions";
            final String action = "Retry later or logout to clear local cached subscriptions";
            throw new AzureToolkitRuntimeException(error, e);
        }
    }

    private static void saveSubscriptions(List<SubscriptionDetail> sdl, String subscriptionsDetailsFileName)
            throws IOException {
        System.out.println("SubscriptionManagerPersist.saveSubscriptions()");
        String sd = JsonHelper.serialize(sdl);
        FileStorage subscriptionsDetailsFileStorage = new FileStorage(subscriptionsDetailsFileName,
                CommonSettings.getSettingsBaseDir());
        subscriptionsDetailsFileStorage.write(sd.getBytes(StandardCharsets.UTF_8));
    }

}
