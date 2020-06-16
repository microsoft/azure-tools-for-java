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
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by shch on 10/3/2016.
 */
public class SubscriptionManager {
    private final Set<ISubscriptionSelectionListener> listeners = new HashSet<>();
    protected final AzureManager azureManager;

    // for user to select subscr to work with
    private List<SubscriptionDetail> subscriptionDetails; // NOTE: This one should be retired in future.
    private Map<String, SubscriptionDetail> subscriptionIdToSubscriptionDetailMap;
    protected Map<String, Subscription> subscriptionIdToSubscriptionMap = new ConcurrentHashMap<>();

    // to get tid for sid
    private final Map<String, String> sidToTid = new ConcurrentHashMap<>();

    public SubscriptionManager(AzureManager azureManager) {
        this.azureManager = azureManager;
    }

    public synchronized Map<String, SubscriptionDetail> getSubscriptionIdToSubscriptionDetailsMap() throws IOException {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSubscriptionIdToSubscriptionDetailsMap()");
        updateSubscriptionDetailsIfNull();
        return subscriptionIdToSubscriptionDetailMap;
    }

    public synchronized Map<String, Subscription> getSubscriptionIdToSubscriptionMap() throws IOException {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSubscriptionIdToSubscriptionMap()");
        updateSubscriptionDetailsIfNull();
        return subscriptionIdToSubscriptionMap;
    }

    public synchronized List<SubscriptionDetail> getSubscriptionDetails() throws IOException {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSubscriptionDetails()");
        updateSubscriptionDetailsIfNull();
        return subscriptionDetails;
    }

    public synchronized List<SubscriptionDetail> getSelectedSubscriptionDetails() throws IOException {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSelectedSubscriptionDetails()");
        updateSubscriptionDetailsIfNull();

        return getAccountSidList().stream()
                .map(sid -> subscriptionIdToSubscriptionDetailMap.get(sid))
                .collect(Collectors.toList());
    }

    private void updateSubscriptionDetailsIfNull() throws IOException {
        if (subscriptionDetails == null) {
            List<SubscriptionDetail> sdl = updateAccountSubscriptionList();
            doSetSubscriptionDetails(sdl);
        }
    }

    protected List<SubscriptionDetail> updateAccountSubscriptionList() throws IOException {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.updateAccountSubscriptionList()");

        if (azureManager == null) {
            throw new IllegalArgumentException("azureManager is null");
        }

        System.out.println("Getting subscription list from Azure");
        subscriptionIdToSubscriptionMap.clear();
        List<SubscriptionDetail> sdl = new ArrayList<>();
        List<Pair<Subscription, Tenant>> stpl = azureManager.getSubscriptionsWithTenant();
        for (Pair<Subscription, Tenant> stp : stpl) {
            sdl.add(new SubscriptionDetail(
                    stp.first().subscriptionId(),
                    stp.first().displayName(),
                    stp.second().tenantId(),
                    true));
            // WORKAROUND: update sid->subscription map at the same time
            subscriptionIdToSubscriptionMap.put(stp.first().subscriptionId(), stp.first());
        }
        return sdl;
    }

    private synchronized void doSetSubscriptionDetails(List<SubscriptionDetail> subscriptionDetails) throws IOException {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.doSetSubscriptionDetails()");
//        if (subscriptionDetails.isEmpty()) {
//            throw new AuthException("No subscription found in the account");
//        }

        this.subscriptionDetails = subscriptionDetails;
        updateMapAccordingToList(); // WORKAROUND: Update SubscriptionId->SubscriptionDetail Map
        updateSidToTidMap();
    }

    // WORKAROUND: private helper to construct SubscriptionId->SubscriptionDetail map
    private void updateMapAccordingToList() throws IOException {
        Map<String, SubscriptionDetail> sid2sd = new ConcurrentHashMap<>();
        for (SubscriptionDetail sd : this.subscriptionDetails) {
            sid2sd.put(sd.getSubscriptionId(),
                    new SubscriptionDetail(
                            sd.getSubscriptionId(),
                            sd.getSubscriptionName(),
                            sd.getTenantId(),
                            sd.isSelected()));
        }
        this.subscriptionIdToSubscriptionDetailMap = sid2sd;
    }

    public void setSubscriptionDetails(List<SubscriptionDetail> subscriptionDetails) throws IOException {
        System.out.println("SubscriptionManager.setSubscriptionDetails() " + Thread.currentThread().getId());
        doSetSubscriptionDetails(subscriptionDetails);
        notifyAllListeners();
    }

    public synchronized void addListener(ISubscriptionSelectionListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public synchronized void removeListener(ISubscriptionSelectionListener l) {
        listeners.remove(l);
    }

    private void notifyAllListeners() {
        for (ISubscriptionSelectionListener l : listeners) {
            l.update(subscriptionDetails == null);
        }
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.UPDATE, null));
        }
    }

    public synchronized String getSubscriptionTenant(String sid) {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getSubscriptionTenant()");
        return sidToTid.get(sid);
    }

    public synchronized Set<String> getAccountSidList() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.getAccountSidList()");
        return sidToTid.keySet();
    }

    public void cleanSubscriptions() throws IOException {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.cleanSubscriptions()");
        synchronized (this) {
            if (subscriptionDetails != null) {
                subscriptionDetails.clear();
                subscriptionDetails = null;
                sidToTid.clear();
            }
        }
        notifyAllListeners();
    }

    private void updateSidToTidMap() {
        System.out.println(Thread.currentThread().getId() + " SubscriptionManager.updateSidToTidMap()");
        sidToTid.clear();
        for (SubscriptionDetail sd : subscriptionDetails) {
            if (sd.isSelected()) {
                sidToTid.put(sd.getSubscriptionId(), sd.getTenantId());
            }
        }
    }
}
