/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by shch on 10/3/2016.
 */
@Log
public class SubscriptionManager {
    private final Set<ISubscriptionSelectionListener> listeners = new HashSet<>();

    private static final String FILE_NAME_SUBSCRIPTIONS_DETAILS = "subscriptionsDetails.json";

    public void setSubscriptionDetails(List<Subscription> subscriptionDetails) {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.setSubscriptionDetails()");
        synchronized (this) {
            try {
                saveSubscriptions(subscriptionDetails);
                notifyAllListeners(CollectionUtils.isEmpty(subscriptionDetails));
            } catch (final IOException e) {
                final String error = "Failed to update local subscriptions cache while updating";
                final String action = "Retry later";
                throw new AzureToolkitRuntimeException(error, e, action);
            }
        }
    }

    public synchronized void cleanSubscriptions() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.cleanSubscriptions()");
        AzureStoreManager.getInstance().getIdeStore().setProperty(TelemetryConstants.ACCOUNT, "subscriptions_json", null);
    }

    @AzureOperation(name = "account.load_subscription_cache", type = AzureOperation.Type.TASK)
    public static List<Subscription> loadSubscriptions() {
        System.out.println("SubscriptionManager.loadSubscriptions()");
        try {
            String json = AzureStoreManager.getInstance().getIdeStore().getProperty(TelemetryConstants.ACCOUNT, "subscription_details");
            if (StringUtils.isBlank(json)) {
                final FileStorage file = new FileStorage(FILE_NAME_SUBSCRIPTIONS_DETAILS, CommonSettings.getSettingsBaseDir());
                final byte[] data = file.read();
                json = new String(data, StandardCharsets.UTF_8);
                file.removeFile();
                AzureStoreManager.getInstance().getIdeStore().setProperty(TelemetryConstants.ACCOUNT, "subscriptions_json", json);
            }
            if (json.isEmpty()) {
                System.out.println("subscription details is empty");
                return Collections.emptyList();
            }
            final Subscription[] sda = JsonHelper.deserialize(Subscription[].class, json);
            return new ArrayList<>(Arrays.asList(sda));
        } catch (final IOException e) {
            final String error = "Failed to load local cached subscriptions";
            final String action = "Retry later or logout to clear local cached subscriptions";
            throw new AzureToolkitRuntimeException(error, e);
        }
    }

    @AzureOperation(name = "account.persist_subscription", type = AzureOperation.Type.TASK)
    private static void saveSubscriptions(List<Subscription> sdl)
            throws IOException {
        System.out.println("SubscriptionManager.saveSubscriptions()");
        AzureStoreManager.getInstance().getIdeStore().setProperty(TelemetryConstants.ACCOUNT, "subscription_details", JsonHelper.serialize(sdl));
    }

    public synchronized Map<String, Subscription> getSubscriptionIdToSubscriptionDetailsMap() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSubscriptionIdToSubscriptionDetailsMap()");
        updateSubscriptionDetailsIfNull();
        return Utils.groupByIgnoreDuplicate(IdentityAzureManager.getInstance().getSubscriptionDetails(), Subscription::getId);
    }

    @AzureOperation(name = "account.get_subscription_details", type = AzureOperation.Type.TASK)
    public synchronized List<Subscription> getSubscriptionDetails() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSubscriptionDetails()");
        updateSubscriptionDetailsIfNull();
        return IdentityAzureManager.getInstance().getSubscriptionDetails();
    }

    @AzureOperation(name = "account.get_subscription_detail", type = AzureOperation.Type.TASK)
    public synchronized List<Subscription> getSelectedSubscriptionDetails() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSelectedSubscriptionDetails()");
        updateSubscriptionDetailsIfNull();

        final List<Subscription> selectedSubscriptions =
                IdentityAzureManager.getInstance().getSubscriptionDetails().stream().filter(Subscription::isSelected).collect(Collectors.toList());

        return selectedSubscriptions;
    }

    public void updateSubscriptionDetailsIfNull() {
    }

    public synchronized void addListener(ISubscriptionSelectionListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public synchronized void removeListener(ISubscriptionSelectionListener l) {
        listeners.remove(l);
    }

    public void notifySubscriptionListChanged() {
        notifyAllListeners(false);
    }

    protected void notifyAllListeners(boolean isRefresh) {
        for (ISubscriptionSelectionListener l : listeners) {
            l.update(isRefresh);
        }
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.UPDATE, null));
        }
    }
}
