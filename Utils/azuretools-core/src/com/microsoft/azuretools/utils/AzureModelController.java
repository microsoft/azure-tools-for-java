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
package com.microsoft.azuretools.utils;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.ISubscriptionSelectionListener;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.WebAppUtils.WebAppDetails;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by vlashch on 1/9/17.
 */
public class AzureModelController {
    private final static Logger LOGGER = Logger.getLogger(AzureModelController.class.getName());
    private static ISubscriptionSelectionListener subscriptionSelectionListener = new ISubscriptionSelectionListener() {
        @Override
        public void update(boolean isRefresh) {
            if (isRefresh) {
                clearAll();
                return;
            }

            IProgressTask pt = new ProgressTask(CommonSettings.getUiFactory().getProgressTaskImpl());
            pt.work(new IWorker() {
                @Override
                public void work(IProgressIndicator pi) {
                    try {
                        subscriptionSelectionChanged(pi);
                    } catch (Exception e) {
                        e.printStackTrace();
                        LOGGER.log(Level.SEVERE, "IWorker.work", e);
                    }
                }

                @Override
                public String getName() {
                    return "Update Azure Local Cache Progress";
                }
            });
        }
    };

    private static void clearAll() {
        System.out.println("AzureModelController.clearAll: set null to all the maps.");
        AzureModel azureModel = AzureModel.getInstance();
        azureModel.setSubscriptionToResourceGroupMap(null);
        azureModel.setResourceGroupToWebAppMap(null);
        azureModel.setResourceGroupToAppServicePlanMap(null);

        // TODO: notify subscribers

        AzureUIRefreshCore.removeAll();
    }

    private static synchronized void subscriptionSelectionChanged(IProgressIndicator progressIndicator) throws IOException, AuthException {
        System.out.println("AzureModelController.subscriptionSelectionChanged: starting");
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            System.out.println("AzureModelController.subscriptionSelectionChanged: azureManager == null -> return");
            return;
        }

        SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
        AzureModel azureModel = AzureModel.getInstance();

        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = azureModel.getSubscriptionToResourceGroupMap();
        if (srgMap == null) {
            System.out.println("AzureModelController.subscriptionSelectionChanged: srgMap == null -> return");
            return;
        }
        Map <String, Subscription> sidToSubscriptionMap = azureModel.getSidToSubscriptionMap();
        if (sidToSubscriptionMap == null) {
            System.out.println("AzureModelController.subscriptionSelectionChanged: sidToSubscriptionMap == null -> return");
            return;
        }

        Map<ResourceGroup, List<WebApp>> rgwaMap = azureModel.getResourceGroupToWebAppMap();
        Map<ResourceGroup, List<AppServicePlan>> rgspMap = azureModel.getResourceGroupToAppServicePlanMap();

        System.out.println("AzureModelController.subscriptionSelectionChanged: getting subscription details...");
        List<SubscriptionDetail> sdl = subscriptionManager.getSubscriptionDetails();
        if (sdl == null) {
            System.out.println("AzureModelController.subscriptionSelectionChanged: sdl == null -> return");
            return;
        }

        for (SubscriptionDetail sd : sdl) {
            if (!srgMap.containsKey(sd)) {
                if (!sd.isSelected()) continue;

                if(progressIndicator != null && progressIndicator.isCanceled()) {
                    progressIndicator.setText("Cancelling...");
                    clearAll();
                    return;
                    // FIXME: throw exception?
                }

                Azure azure = azureManager.getAzure(sd.getSubscriptionId());
                // subscription locations
                List<Subscription> sl = azureManager.getSubscriptions();
                System.out.println("Updating subscription locations");
                Subscription subscription = sidToSubscriptionMap.get(sd.getSubscriptionId());
                if(progressIndicator != null) progressIndicator.setText(String.format("Updating subscription '%s' locations...", subscription.displayName()));
                List<Location> locl = subscription.listLocations();
                Collections.sort(locl, new Comparator<Location>() {
                    @Override
                    public int compare(Location lhs, Location rhs) {
                        return lhs.displayName().compareTo(rhs.displayName());
                    }
                });

                Map<SubscriptionDetail, List<Location>> sdlocMap = azureModel.getSubscriptionToLocationMap();
                sdlocMap.put(sd, locl);

                // resource group maps
                List<ResourceGroup> rgList = azure.resourceGroups().list();
                srgMap.put(sd, rgList);
                updateResGrDependency(azure, rgList, progressIndicator, rgwaMap, rgspMap);
            } else {
                // find and modify the key
                for (SubscriptionDetail sdk : srgMap.keySet()) {
                    if (sdk.equals(sd)) {
                        sdk.setSelected(sd.isSelected());
                    }
                }
            }
        }
    }

    static class RgDepParams {
        ResourceGroup rg;
        List<WebApp> wal;
        List<AppServicePlan> aspl;

        public RgDepParams(ResourceGroup rg, List<WebApp> wal, List<AppServicePlan> aspl) {
            this.rg = rg;
            this.wal = wal;
            this.aspl = aspl;
        }
    }

    private static void updateResGrDependency(Azure azure,
            List<ResourceGroup> rgList,
            IProgressIndicator progressIndicator,
            Map<ResourceGroup, List<WebApp>> rgwaMap,
            Map<ResourceGroup, List<AppServicePlan>> rgspMap) {

        if (progressIndicator != null) progressIndicator.setText("Reading App Services...");
        int tasksSize = rgList.size();
        if (tasksSize == 0) return;
        Observable.from(rgList).flatMap(new Func1<ResourceGroup, Observable<? extends RgDepParams>>() {
            @Override
            public Observable<? extends RgDepParams> call(ResourceGroup rg) {
                return Observable.create(new Observable.OnSubscribe<RgDepParams>() {
                    @Override
                    public void call(Subscriber<? super RgDepParams> subscriber) {
                        List<WebApp> wal = azure.webApps().listByResourceGroup(rg.name());
                        List<AppServicePlan> aspl = azure.appServices().appServicePlans().listByResourceGroup(rg.name())
                                .stream().collect(Collectors.toList());
                        subscriber.onNext(new RgDepParams(rg, wal, aspl));
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io());
            }
        }, tasksSize)
        .subscribeOn(Schedulers.newThread())
        .toBlocking()
        .subscribe(new Action1<RgDepParams>() {
            @Override
            public void call(RgDepParams params) {
                synchronized (rgwaMap) {
                    synchronized (rgspMap) {
                        rgwaMap.put(params.rg, params.wal);
                        rgspMap.put(params.rg, params.aspl);
                    }
                }
            }
        });

//        for (ResourceGroup rg : rgList) {
//            if (progressIndicator != null && progressIndicator.isCanceled()) {
//                clearAll();
//                return;
//            }
//            //System.out.println("rg : " + rg);
//            if (progressIndicator != null) progressIndicator.setText("Reading resource group '" + rg.name() + "'");
//            List<WebApp> waList = azure.webApps().listByGroup(rg.name());
//            rgwaMap.put(rg, waList);
//            List<AppServicePlan> aspl = azure.appServices().appServicePlans().listByGroup(rg.name());
//            rgspMap.put(rg, aspl);
//        }
    }

    public static synchronized void updateSubscriptionMaps(IProgressIndicator progressIndicator) throws IOException, CanceledByUserException, AuthException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) { return; }

        if (progressIndicator != null && progressIndicator.isCanceled()) {
            clearAll();
            throw new CanceledByUserException();
        }
        AzureModel azureModel = AzureModel.getInstance();

        // to get regions we nees subscription objects
        if (progressIndicator != null) progressIndicator.setText("Reading subscription list...");
        List<Subscription> sl = azureManager.getSubscriptions();
        // convert to map to easier find by sid
        Map <String, Subscription> sidToSubscriptionMap = azureModel.createSidToSubscriptionMap();
        for (Subscription s : sl) {
            sidToSubscriptionMap.put(s.subscriptionId(), s);
        }
        azureModel.setSidToSubscriptionMap(sidToSubscriptionMap);
        Map<SubscriptionDetail, List<Location>> sdlocMap = azureModel.createSubscriptionToRegionMap();
        Map<SubscriptionDetail, List<ResourceGroup>> sdrgMap = azureModel.createSubscriptionToResourceGroupMap();

        SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
        subscriptionManager.addListener(subscriptionSelectionListener);

        List<SubscriptionDetail> sdl = subscriptionManager.getSubscriptionDetails();
        if (sdl.size() > 0) {
            Observable.from(sdl).flatMap((sd) ->
                Observable.create((subscriber) -> {
                    try {
                        if (progressIndicator != null && progressIndicator.isCanceled()) {
                            clearAll();
                            throw new CanceledByUserException();
                        }
                        if (sd.isSelected()) {
                            if (progressIndicator != null) {
                                progressIndicator.setText("Reading subscription");
                            }
                            Azure azure = azureManager.getAzure(sd.getSubscriptionId());

                            List<ResourceGroup> rgList = azure.resourceGroups().list();
                            sdrgMap.put(sd, rgList);

                            List<Location> locl = sidToSubscriptionMap.get(sd.getSubscriptionId()).listLocations();
                            Collections.sort(locl, Comparator.comparing(Location::displayName));
                            sdlocMap.put(sd, locl);
                        }
                        subscriber.onCompleted();
                    } catch (Exception e) {
                        Exceptions.propagate(e);
                    }
                }).subscribeOn(Schedulers.io()), sdl.size()).subscribeOn(Schedulers.io()).toBlocking().subscribe();
        }
        azureModel.setSubscriptionToResourceGroupMap(sdrgMap);
        azureModel.setSubscriptionToLocationMap(sdlocMap);
    }

    public static synchronized void updateResourceGroupMaps(IProgressIndicator progressIndicator) throws IOException, CanceledByUserException, AuthException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) { return;}

        updateSubscriptionMaps(progressIndicator);
        AzureModel azureModel = AzureModel.getInstance();
        Map<ResourceGroup, List<WebApp>> rgwaMap = azureModel.createResourceGroupToWebAppMap();
        Map<ResourceGroup, List<AppServicePlan>> rgspMap = azureModel.createResourceGroupToAppServicePlanMap();
        Set<SubscriptionDetail> sdSet = azureModel.getSubscriptionToResourceGroupMap().keySet();
        if (sdSet.size() == 0) {
            azureModel.setResourceGroupToWebAppMap(rgwaMap);
            azureModel.setResourceGroupToAppServicePlanMap(rgspMap);
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(sdSet.size());
        Observable.from(sdSet).flatMap((sd) ->
            Observable.create((subscriber) -> {
                try {
                    List<ResourceGroup> rgList = azureModel.getSubscriptionToResourceGroupMap().get(sd);
                    if (rgList.size() > 0) {
                        Azure azure = azureManager.getAzure(sd.getSubscriptionId());
                        updateResGrDependency(azure, rgList, progressIndicator, rgwaMap, rgspMap);
                    }
                    subscriber.onCompleted();
                } catch (Exception e) {
                    Exceptions.propagate(e);
                } finally {
                    countDownLatch.countDown();
                }
            }).subscribeOn(Schedulers.io()), sdSet.size()).subscribeOn(Schedulers.io()).subscribe();

        Thread cancelCheckThread = new Thread(() -> {
            try {
                while (true) {
                    if (progressIndicator != null && progressIndicator.isCanceled()) {
                        long count = countDownLatch.getCount();
                        for (long i = 0; i < count; i++) {
                            countDownLatch.countDown();
                        }
                        clearAll();
                        break;
                    }
                    if (countDownLatch.getCount() <= 0) {
                        break;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
            }
        });
        cancelCheckThread.setDaemon(true);
        cancelCheckThread.start();

        try {
            countDownLatch.await();
        } catch (Exception ignore) {
        }

        if (progressIndicator != null && progressIndicator.isCanceled()) {
            throw new CanceledByUserException();
        }

        azureModel.setResourceGroupToWebAppMap(rgwaMap);
        azureModel.setResourceGroupToAppServicePlanMap(rgspMap);
    }

    public static void addNewResourceGroup(SubscriptionDetail sd, ResourceGroup rg) {
        AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(sd).add(rg);
        // TODO:notify subscribers
    }

    public static void addNewWebAppToJustCreatedResourceGroup(ResourceGroup rg, WebApp webApp) {
        // presume addNewResourceGroup goes first
        List<WebApp> l = new LinkedList<>();
        l.add(webApp);
        AzureModel.getInstance().getResourceGroupToWebAppMap().put(rg, l);
        // TODO:notify subscribers
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.ADD, new WebAppDetails(rg, webApp, null, null, null)));
        }
    }

    public static void addNewWebAppToExistingResourceGroup(ResourceGroup rg, WebApp webApp) {
        AzureModel.getInstance().getResourceGroupToWebAppMap().get(rg).add(webApp);
        // TODO:notify subscribers
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.ADD, new WebAppDetails(rg, webApp, null, null, null)));
        }
    }

    public static void removeWebAppFromResourceGroup(ResourceGroup rg, WebApp webApp) {
        AzureModel.getInstance().getResourceGroupToWebAppMap().get(rg).remove(webApp);
        // TODO:notify subscribers
        if (AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REMOVE, new WebAppDetails(rg, webApp, null, null, null)));
        }
    }

    public static void removeAppServicePlanFromResourceGroup(ResourceGroup rg, AppServicePlan appServicePlan) {
        AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg).remove(appServicePlan);
    }

    public static void addNewAppServicePlanToJustCreatedResourceGroup(ResourceGroup rg, AppServicePlan appServicePlan) {
        // presume addNewResourceGroup call goes first
        List<AppServicePlan> l = new LinkedList<>();
        if (appServicePlan != null) {
            l.add(appServicePlan);
        }
        AzureModel.getInstance().getResourceGroupToAppServicePlanMap().put(rg, l);
        // TODO:notify subscribers
        System.out.println("WEBAPP - IN AzureModelController::addNewAppServicePlanToJustCreatedResourceGroup");
    }

    public static void addNewAppServicePlanToExistingResourceGroup(ResourceGroup rg, AppServicePlan appServicePlan) {
        // presume addNewResourceGroup call goes first
        AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg).add(appServicePlan);
        // TODO:notify subscribers
        System.out.println("WEBAPP - IN AzureModelController::addNewAppServicePlanToExistingResourceGroup");
    }

}
