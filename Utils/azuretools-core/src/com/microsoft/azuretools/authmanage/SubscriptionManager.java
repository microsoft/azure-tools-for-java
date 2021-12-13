/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage;

import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azuretools.adauth.JsonHelper;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by shch on 10/3/2016.
 */
@Log
public class SubscriptionManager {
    private final Set<ISubscriptionSelectionListener> listeners = new HashSet<>();

    private static final String FILE_NAME_SUBSCRIPTIONS_DETAILS = "subscriptionsDetails.json";

    public void setSubscriptionDetails(List<SubscriptionDetail> subscriptionDetails) {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.setSubscriptionDetails()");
        synchronized (this) {
            saveSubscriptions(subscriptionDetails);
            notifyAllListeners(CollectionUtils.isEmpty(subscriptionDetails));
        }
    }

    public synchronized void cleanSubscriptions() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.cleanSubscriptions()");
        AzureStoreManager.getInstance().getIdeStore().setProperty(TelemetryConstants.ACCOUNT, "subscriptions_json", null);
    }

    @AzureOperation(name = "account.load_subscription_cache", type = AzureOperation.Type.TASK)
    public static List<SubscriptionDetail> loadSubscriptions() {
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
            final SubscriptionDetail[] sda = JsonHelper.deserialize(SubscriptionDetail[].class, json);
            return new ArrayList<>(Arrays.asList(sda));
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    @AzureOperation(name = "account.persist_subscription", type = AzureOperation.Type.TASK)
    private static void saveSubscriptions(List<SubscriptionDetail> sdl) {
        System.out.println("SubscriptionManager.saveSubscriptions()");
        AzureStoreManager.getInstance().getIdeStore().setProperty(TelemetryConstants.ACCOUNT, "subscription_details", JsonHelper.serialize(sdl));
    }

    public synchronized Map<String, SubscriptionDetail> getSubscriptionIdToSubscriptionDetailsMap() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSubscriptionIdToSubscriptionDetailsMap()");
        updateSubscriptionDetailsIfNull();
        return Utils.groupByIgnoreDuplicate(IdentityAzureManager.getInstance().getSubscriptionDetails(), SubscriptionDetail::getSubscriptionId);
    }

    @AzureOperation(name = "account.get_subscription_details", type = AzureOperation.Type.TASK)
    public synchronized List<SubscriptionDetail> getSubscriptionDetails() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSubscriptionDetails()");
        updateSubscriptionDetailsIfNull();
        return IdentityAzureManager.getInstance().getSubscriptionDetails();
    }

    @AzureOperation(name = "account.get_subscription_detail", type = AzureOperation.Type.TASK)
    public synchronized List<SubscriptionDetail> getSelectedSubscriptionDetails() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSelectedSubscriptionDetails()");
        updateSubscriptionDetailsIfNull();

        return IdentityAzureManager.getInstance().getSubscriptionDetails().stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());
    }

    public void updateSubscriptionDetailsIfNull() {
    }

    public synchronized void addListener(ISubscriptionSelectionListener l) {
        listeners.add(l);
    }

    public synchronized void removeListener(ISubscriptionSelectionListener l) {
        listeners.remove(l);
    }

    public void notifySubscriptionListChanged() {
        notifyAllListeners(false);
    }

    protected void notifyAllListeners(boolean isRefresh) {
        for (final ISubscriptionSelectionListener l : listeners) {
            l.update(isRefresh);
        }
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.UPDATE, null));
        }
    }

    public String getSubscriptionTenant(String sid) {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSubscriptionTenant()");
        return Azure.az(AzureAccount.class).account().getSubscription(sid).getTenantId();
    }
}
