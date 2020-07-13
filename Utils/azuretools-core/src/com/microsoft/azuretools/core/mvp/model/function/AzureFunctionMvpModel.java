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

package com.microsoft.azuretools.core.mvp.model.function;

import com.azure.resourcemanager.Azure;
import com.azure.resourcemanager.appservice.fluent.inner.GeoRegionInner;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.ApplicationLogsConfig;
import com.azure.resourcemanager.appservice.models.FunctionApp;
import com.azure.resourcemanager.appservice.models.FunctionApps;
import com.azure.resourcemanager.appservice.models.FunctionEnvelope;
import com.azure.resourcemanager.appservice.models.LogLevel;
import com.azure.resourcemanager.appservice.models.PricingTier;
import com.azure.resourcemanager.appservice.models.SkuName;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebAppDiagnosticLogs;
import com.azure.resourcemanager.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AppServiceUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AzureFunctionMvpModel {

    private static final String CANNOT_GET_FUNCTION_APP_WITH_ID = "Cannot get Function App with ID: ";
    private final Map<String, List<ResourceEx<FunctionApp>>> subscriptionIdToFunctionApps;

    private AzureFunctionMvpModel() {
        subscriptionIdToFunctionApps = new ConcurrentHashMap<>();
    }

    public static AzureFunctionMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public FunctionApp getFunctionById(String sid, String id) throws IOException {
        FunctionApp app = getFunctionAppsClient(sid).getById(id);
        if (app == null) {
            throw new IOException(CANNOT_GET_FUNCTION_APP_WITH_ID + id); // TODO: specify the type of exception.
        }
        return app;
    }

    public void deleteFunction(String sid, String appId) throws IOException {
        getFunctionAppsClient(sid).deleteById(appId);
        subscriptionIdToFunctionApps.remove(sid);
    }

    public void restartFunction(String sid, String appId) throws IOException {
        getFunctionAppsClient(sid).getById(appId).restart();
    }

    public void startFunction(String sid, String appId) throws IOException {
        getFunctionAppsClient(sid).getById(appId).start();
    }

    public void stopFunction(String sid, String appId) throws IOException {
        getFunctionAppsClient(sid).getById(appId).stop();
    }

    /**
     * List app service plan by subscription id and resource group name.
     */
    public List<AppServicePlan> listAppServicePlanBySubscriptionIdAndResourceGroupName(String sid, String group)
            throws IOException {
        List<AppServicePlan> appServicePlans = new ArrayList<>();

        Azure azure = AuthMethodManager.getInstance().getTrack2AzureClient(sid);
        return IterableUtils.toList(azure.appServicePlans().listByResourceGroup(group));
    }

    /**
     * List app service plan by subscription id.
     */
    public List<AppServicePlan> listAppServicePlanBySubscriptionId(String sid) throws IOException {
        return IterableUtils.toList(AuthMethodManager.getInstance().getTrack2AzureClient(sid).appServicePlans().list());
    }

    /**
     * List all the Function Apps in selected subscriptions.
     *
     * @param forceReload flag indicating whether force to fetch latest data from server
     * @return list of Function App
     */
    public List<ResourceEx<FunctionApp>> listAllFunctions(final boolean forceReload) {
        final List<ResourceEx<FunctionApp>> functions = new ArrayList<>();
        List<Subscription> subs = AzureMvpModel.getInstance().getSelectedSubscriptions();
        if (subs.size() == 0) {
            return functions;
        }

        Flux.fromIterable(subs).flatMap((sd) ->
                Flux.create((subscriber) -> {
                    try {
                        List<ResourceEx<FunctionApp>> functionList = listFunctionsInSubscription(sd.subscriptionId(), forceReload);
                        synchronized (functions) {
                            functions.addAll(functionList);
                        }
                    } catch (IOException e) {
                        // swallow exception and skip error subscription
                    }
                    subscriber.complete();
                }).subscribeOn(Schedulers.elastic()), subs.size()
        ).subscribeOn(Schedulers.elastic()).blockLast();
        return functions;
    }

    public List<FunctionEnvelope> listFunctionEnvelopeInFunctionApp(String sid, String id) throws IOException {
        FunctionApp app = getFunctionById(sid, id);
        return IterableUtils.toList(app.manager().functionApps().listFunctions(app.resourceGroupName(), app.name()));
    }

    public List<Region> getAvailableRegions(String subscriptionId, PricingTier pricingTier) throws IOException {
        if (StringUtils.isEmpty(subscriptionId) || pricingTier == null || pricingTier.toSkuDescription() == null) {
            return Collections.emptyList();
        }
        final SkuName skuName = SkuName.fromString(pricingTier.toSkuDescription().tier());
        final Iterable<GeoRegionInner> geoRegionIterable = AuthMethodManager.getInstance()
                .getTrack2AzureClient(subscriptionId)
                .webApps().manager().inner().getResourceProviders().listGeoRegions(skuName, false, false, false);
        return IterableUtils.toList(geoRegionIterable).stream()
                .map(regionInner -> Region.fromName(regionInner.displayName()))
                .collect(Collectors.toList());
    }

    public boolean getPublishingProfileXmlWithSecrets(String sid, String functionAppId, String filePath) throws IOException {
        final FunctionApp app = getFunctionById(sid, functionAppId);
        return AppServiceUtils.getPublishingProfileXmlWithSecretsForTrack2(app, filePath);
    }

    public void updateWebAppSettings(String sid, String functionAppId, Map<String, String> toUpdate, Set<String> toRemove)
            throws IOException {
        final FunctionApp app = getFunctionById(sid, functionAppId);
        WebAppBase.Update<FunctionApp> update = app.update().withAppSettings(toUpdate);
        for (String key : toRemove) {
            update = update.withoutAppSetting(key);
        }
        update.apply();
    }

    public List<PricingTier> listFunctionPricingTier() throws IllegalAccessException {
        final List<PricingTier> pricingTiers = new ArrayList<>(PricingTier.getAll());
        // Add Premium pricing tiers
        pricingTiers.add(new PricingTier(SkuName.ELASTIC_PREMIUM.toString(), "EP1"));
        pricingTiers.add(new PricingTier(SkuName.ELASTIC_PREMIUM.toString(), "EP2"));
        pricingTiers.add(new PricingTier(SkuName.ELASTIC_PREMIUM.toString(), "EP3"));
        return pricingTiers;
    }

    public static boolean isApplicationLogEnabled(WebAppBase webAppBase) {
        final WebAppDiagnosticLogs config = webAppBase.diagnosticLogsConfig();
        if (config == null || config.inner() == null || config.inner().applicationLogs() == null) {
            return false;
        }
        final ApplicationLogsConfig applicationLogsConfig = config.inner().applicationLogs();
        return (applicationLogsConfig.fileSystem() != null
                && applicationLogsConfig.fileSystem().level() != LogLevel.OFF) ||
                (applicationLogsConfig.azureBlobStorage() != null
                        && applicationLogsConfig.azureBlobStorage().level() != LogLevel.OFF) ||
                (applicationLogsConfig.azureTableStorage() != null
                        && applicationLogsConfig.azureTableStorage().level() != LogLevel.OFF);
    }

    public static void enableApplicationLog(FunctionApp functionApp) {
        functionApp.update().updateDiagnosticLogsConfiguration()
                .withApplicationLogging()
                .withLogLevel(LogLevel.INFORMATION)
                .withApplicationLogsStoredOnFileSystem()
                .parent()
                .apply();
    }

    /**
     * List all Function Apps by subscription id.
     */
    @NotNull
    private List<ResourceEx<FunctionApp>> listFunctionsInSubscription(final String subscriptionId, final boolean forceReload)
            throws IOException {
        if (!forceReload && subscriptionIdToFunctionApps.get(subscriptionId) != null) {
            return subscriptionIdToFunctionApps.get(subscriptionId);
        }

        final Azure azure = AuthMethodManager.getInstance().getTrack2AzureClient(subscriptionId);
        // Remove filter function app logic as track2 has split webapp with function
        // Need verify the load time cost and decide whether to deprecated wrapper
        List<ResourceEx<FunctionApp>> functions = azure.functionApps()
                .list().stream()
                .map(app -> new ResourceEx<FunctionApp>(app, subscriptionId))
                .collect(Collectors.toList());
        subscriptionIdToFunctionApps.put(subscriptionId, functions);

        return functions;
    }
//    public FunctionApp getFunctionById(String sid, String id) throws IOException {
//        FunctionApp app = getFunctionAppsClient(sid).getById(id);
//        if (app == null) {
//            throw new IOException(CANNOT_GET_FUNCTION_APP_WITH_ID + id); // TODO: specify the type of exception.
//        }
//        return app;
//    }
//
//    public void deleteFunction(String sid, String appId) throws IOException {
//        getFunctionAppsClient(sid).deleteById(appId);
//        subscriptionIdToFunctionApps.remove(sid);
//    }
//
//    public void restartFunction(String sid, String appId) throws IOException {
//        getFunctionAppsClient(sid).getById(appId).restart();
//    }
//
//    public void startFunction(String sid, String appId) throws IOException {
//        getFunctionAppsClient(sid).getById(appId).start();
//    }
//
//    public void stopFunction(String sid, String appId) throws IOException {
//        getFunctionAppsClient(sid).getById(appId).stop();
//    }
//
//    /**
//     * List app service plan by subscription id and resource group name.
//     */
//    public List<AppServicePlan> listAppServicePlanBySubscriptionIdAndResourceGroupName(String sid, String group)
//            throws IOException {
//        List<AppServicePlan> appServicePlans = new ArrayList<>();
//
//        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
//        appServicePlans.addAll(azure.appServices().appServicePlans().listByResourceGroup(group));
//
//        return appServicePlans;
//    }
//
//    /**
//     * List app service plan by subscription id.
//     */
//    public List<AppServicePlan> listAppServicePlanBySubscriptionId(String sid) throws IOException {
//        return AuthMethodManager.getInstance().getAzureClient(sid).appServices().appServicePlans().list();
//    }
//
//    /**
//     * List all the Function Apps in selected subscriptions.
//     *
//     * @param forceReload flag indicating whether force to fetch latest data from server
//     * @return list of Function App
//     */
//    public List<ResourceEx<FunctionApp>> listAllFunctions(final boolean forceReload) {
//        final List<ResourceEx<FunctionApp>> functions = new ArrayList<>();
//        List<Subscription> subs = AzureMvpModel.getInstance().getSelectedSubscriptions();
//        if (subs.size() == 0) {
//            return functions;
//        }
//        Observable.from(subs).flatMap((sd) ->
//                Observable.create((subscriber) -> {
//                    try {
//                        List<ResourceEx<FunctionApp>> functionList = listFunctionsInSubscription(sd.subscriptionId(), forceReload);
//                        synchronized (functions) {
//                            functions.addAll(functionList);
//                        }
//                    } catch (IOException e) {
//                        // swallow exception and skip error subscription
//                    }
//                    subscriber.onCompleted();
//                }).subscribeOn(Schedulers.io()), subs.size()).subscribeOn(Schedulers.io()).toBlocking().subscribe();
//        return functions;
//    }
//
//    public List<FunctionEnvelope> listFunctionEnvelopeInFunctionApp(String sid, String id) throws IOException {
//        FunctionApp app = getFunctionById(sid, id);
//        PagedList<FunctionEnvelope> functions = app.manager().functionApps().listFunctions(app.resourceGroupName(), app.name());
//        functions.loadAll();
//        return new ArrayList<>(functions);
//    }
//
//    public boolean getPublishingProfileXmlWithSecrets(String sid, String functionAppId, String filePath) throws IOException {
//        final FunctionApp app = getFunctionById(sid, functionAppId);
//        return AppServiceUtils.getPublishingProfileXmlWithSecrets(app, filePath);
//    }
//
//    public void updateWebAppSettings(String sid, String functionAppId, Map<String, String> toUpdate, Set<String> toRemove)
//            throws IOException {
//        final FunctionApp app = getFunctionById(sid, functionAppId);
//        WebAppBase.Update<FunctionApp> update = app.update().withAppSettings(toUpdate);
//        for (String key : toRemove) {
//            update = update.withoutAppSetting(key);
//        }
//        update.apply();
//    }
//
//    public List<PricingTier> listFunctionPricingTier() throws IllegalAccessException {
//        final List<PricingTier> pricingTiers = AzureMvpModel.getInstance().listPricingTier();
//        // Add Premium pricing tiers
//        pricingTiers.add(new PricingTier(SkuName.ELASTIC_PREMIUM.toString(), "EP1"));
//        pricingTiers.add(new PricingTier(SkuName.ELASTIC_PREMIUM.toString(), "EP2"));
//        pricingTiers.add(new PricingTier(SkuName.ELASTIC_PREMIUM.toString(), "EP3"));
//        return pricingTiers;
//    }
//
//    public static boolean isApplicationLogEnabled(WebAppBase webAppBase) {
//        final WebAppDiagnosticLogs config = webAppBase.diagnosticLogsConfig();
//        if (config == null || config.inner() == null || config.inner().applicationLogs() == null) {
//            return false;
//        }
//        final ApplicationLogsConfig applicationLogsConfig = config.inner().applicationLogs();
//        return (applicationLogsConfig.fileSystem() != null
//                && applicationLogsConfig.fileSystem().level() != LogLevel.OFF) ||
//                (applicationLogsConfig.azureBlobStorage() != null
//                        && applicationLogsConfig.azureBlobStorage().level() != LogLevel.OFF) ||
//                (applicationLogsConfig.azureTableStorage() != null
//                        && applicationLogsConfig.azureTableStorage().level() != LogLevel.OFF);
//    }
//
//    public static void enableApplicationLog(FunctionApp functionApp) {
//        functionApp.update().updateDiagnosticLogsConfiguration()
//                .withApplicationLogging()
//                .withLogLevel(LogLevel.INFORMATION)
//                .withApplicationLogsStoredOnFileSystem()
//                .parent()
//                .apply();
//    }
//
//    /**
//     * List all Function Apps by subscription id.
//     */
//    @NotNull
//    private List<ResourceEx<FunctionApp>> listFunctionsInSubscription(final String subscriptionId, final boolean forceReload)
//            throws IOException {
//        if (!forceReload && subscriptionIdToFunctionApps.get(subscriptionId) != null) {
//            return subscriptionIdToFunctionApps.get(subscriptionId);
//        }
//
//        final Azure azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId);
//        List<ResourceEx<FunctionApp>> functions = azure.appServices().functionApps()
//                .inner().list().stream().filter(inner -> inner.kind() != null && Arrays.asList(inner.kind().split(",")).contains("functionapp"))
//                .map(inner -> new FunctionAppWrapper(subscriptionId, inner))
//                .map(app -> new ResourceEx<FunctionApp>(app, subscriptionId))
//                .collect(Collectors.toList());
//        subscriptionIdToFunctionApps.put(subscriptionId, functions);
//
//        return functions;
//    }

    private static FunctionApps getFunctionAppsClient(String sid) throws IOException {
        return AuthMethodManager.getInstance().getTrack2AzureClient(sid).functionApps();
    }

    private static final class SingletonHolder {
        private static final AzureFunctionMvpModel INSTANCE = new AzureFunctionMvpModel();
    }
}
