/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.function;

import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppBaseWrapper;
import rx.Completable;
import rx.Observable;

import java.util.Map;

public class FunctionAppWrapper extends WebAppBaseWrapper implements FunctionApp {

    public FunctionAppWrapper(String subscriptionId, SiteInner siteInner) {
        super(subscriptionId, siteInner);
    }

    @Override
    public FunctionDeploymentSlots deploymentSlots() {
        return getFunctionApp().deploymentSlots();
    }

    @Override
    public StorageAccount storageAccount() {
        return getFunctionApp().storageAccount();
    }

    @Override
    public String getMasterKey() {
        return getFunctionApp().getMasterKey();
    }

    @Override
    public Observable<String> getMasterKeyAsync() {
        return getFunctionApp().getMasterKeyAsync();
    }

    @Override
    public Map<String, String> listFunctionKeys(String s) {
        return getFunctionApp().listFunctionKeys(s);
    }

    @Override
    public Observable<Map<String, String>> listFunctionKeysAsync(String s) {
        return getFunctionApp().listFunctionKeysAsync(s);
    }

    @Override
    public NameValuePair addFunctionKey(String s, String s1, String s2) {
        return getFunctionApp().addFunctionKey(s, s1, s2);
    }

    @Override
    public Observable<NameValuePair> addFunctionKeyAsync(String s, String s1, String s2) {
        return getFunctionApp().addFunctionKeyAsync(s, s1, s2);
    }

    @Override
    public void removeFunctionKey(String s, String s1) {
        getFunctionApp().removeFunctionKey(s, s1);
    }

    @Override
    public Completable removeFunctionKeyAsync(String s, String s1) {
        return getFunctionApp().removeFunctionKeyAsync(s, s1);
    }

    @Override
    public void triggerFunction(String s, Object o) {
        getFunctionApp().triggerFunction(s, o);
    }

    @Override
    public Completable triggerFunctionAsync(String s, Object o) {
        return getFunctionApp().triggerFunctionAsync(s, o);
    }

    @Override
    public void syncTriggers() {
        getFunctionApp().syncTriggers();
    }

    @Override
    public Completable syncTriggersAsync() {
        return getFunctionApp().syncTriggersAsync();
    }

    @Override
    public FunctionApp refresh() {
        return getFunctionApp().refresh();
    }

    @Override
    public Observable<FunctionApp> refreshAsync() {
        return getFunctionApp().refreshAsync();
    }

    @Override
    public FunctionApp.Update update() {
        return getFunctionApp().update();
    }

    @Override
    public SupportedTlsVersions minTlsVersion() {
        return getFunctionApp().minTlsVersion();
    }

    @Override
    public Map<String, String> getSiteAppSettings() {
        return getFunctionApp().getSiteAppSettings();
    }

    @Override
    public Observable<Map<String, String>> getSiteAppSettingsAsync() {
        return getFunctionApp().getSiteAppSettingsAsync();
    }

    @Override
    protected WebAppBase getWebAppBase() {
        return getFunctionApp();
    }

    private FunctionApp functionApp;

    private FunctionApp getFunctionApp() {
        if (functionApp == null) {
            synchronized (this) {
                if (functionApp == null) {
                    functionApp = AzureFunctionMvpModel.getInstance().getFunctionById(getSubscriptionId(), inner().id());
                }
            }
        }
        return functionApp;
    }
}
