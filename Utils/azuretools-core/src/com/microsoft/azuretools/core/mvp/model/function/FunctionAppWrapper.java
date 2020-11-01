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

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlots;
import com.microsoft.azure.management.appservice.NameValuePair;
import com.microsoft.azure.management.appservice.SupportedTlsVersions;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppBaseWrapper;
import rx.Completable;
import rx.Observable;

import java.io.IOException;
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
    protected WebAppBase getWebAppBase() {
        return getFunctionApp();
    }

    private FunctionApp functionApp;

    private FunctionApp getFunctionApp() {
        if (functionApp == null) {
            synchronized (this) {
                if (functionApp == null) {
                    try {
                        functionApp = AzureFunctionMvpModel.getInstance().getFunctionById(getSubscriptionId(), inner().id());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get function instance");
                    }
                }
            }
        }
        return functionApp;
    }
}
