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

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.management.apigeneration.Beta;
import com.microsoft.azure.management.appservice.DeployOptions;
import com.microsoft.azure.management.appservice.DeployType;
import com.microsoft.azure.management.appservice.DeploymentSlots;
import com.microsoft.azure.management.appservice.SupportedTlsVersions;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import rx.Completable;
import rx.Observable;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public class WebAppWrapper extends WebAppBaseWrapper implements WebApp {

    private WebApp webapp;

    public WebAppWrapper(final String subscriptionId, final SiteInner siteInner) {
        super(subscriptionId, siteInner);
    }

    @Override
    public DeploymentSlots deploymentSlots() {
        return getWebApp().deploymentSlots();
    }

    @Override
    @Beta(Beta.SinceVersion.V1_7_0)
    public void warDeploy(final File file) {
        getWebApp().warDeploy(file);
    }

    @Override
    @Beta(Beta.SinceVersion.V1_7_0)
    public Completable warDeployAsync(final File file) {
        return getWebApp().warDeployAsync(file);
    }

    @Override
    @Beta(Beta.SinceVersion.V1_7_0)
    public void warDeploy(final InputStream inputStream) {
        getWebApp().warDeploy(inputStream);
    }

    @Override
    @Beta(Beta.SinceVersion.V1_7_0)
    public Completable warDeployAsync(final InputStream inputStream) {
        return getWebApp().warDeployAsync(inputStream);
    }

    @Override
    @Beta(Beta.SinceVersion.V1_7_0)
    public void warDeploy(final File file, final String s) {
        getWebApp().warDeploy(file, s);
    }

    @Override
    @Beta(Beta.SinceVersion.V1_7_0)
    public Completable warDeployAsync(final File file, final String s) {
        return getWebApp().warDeployAsync(file, s);
    }

    @Override
    @Beta(Beta.SinceVersion.V1_7_0)
    public void warDeploy(final InputStream inputStream, final String s) {
        getWebApp().warDeploy(inputStream, s);
    }

    @Override
    @Beta(Beta.SinceVersion.V1_7_0)
    public Completable warDeployAsync(final InputStream inputStream, final String s) {
        return getWebApp().warDeployAsync(inputStream, s);
    }

    @Override
    public SupportedTlsVersions minTlsVersion() {
        return this.getWebApp().minTlsVersion();
    }

    @Override
    public Map<String, String> getSiteAppSettings() {
        return getWebApp().getSiteAppSettings();
    }

    @Override
    public Observable<Map<String, String>> getSiteAppSettingsAsync() {
        return getWebApp().getSiteAppSettingsAsync();
    }

    @Override
    public WebApp refresh() {
        return this.getWebApp().refresh();
    }

    @Override
    public Observable<WebApp> refreshAsync() {
        return this.getWebApp().refreshAsync();
    }

    @Override
    public WebApp.Update update() {
        return this.getWebApp().update();
    }

    @Override
    protected WebAppBase getWebAppBase() {
        return getWebApp();
    }

    private WebApp getWebApp() {
        final AzureWebAppMvpModel instance = AzureWebAppMvpModel.getInstance();
        if (this.webapp == null) {
            this.webapp = instance.getWebAppById(getSubscriptionId(), inner().id());
        }
        return this.webapp;
    }

    @Override
    public void deploy(DeployType deployType, File file) {
        getWebApp().deploy(deployType, file);
    }

    @Override
    public Completable deployAsync(DeployType deployType, File file) {
        return getWebApp().deployAsync(deployType, file);
    }

    @Override
    public void deploy(DeployType deployType, File file, DeployOptions deployOptions) {
        getWebApp().deploy(deployType, file, deployOptions);
    }

    @Override
    public Completable deployAsync(DeployType deployType, File file, DeployOptions deployOptions) {
        return getWebApp().deployAsync(deployType, file, deployOptions);
    }

    @Override
    public void deploy(DeployType deployType, InputStream inputStream) {
        getWebApp().deploy(deployType, inputStream);
    }

    @Override
    public Completable deployAsync(DeployType deployType, InputStream inputStream) {
        return getWebApp().deployAsync(deployType, inputStream);
    }

    @Override
    public void deploy(DeployType deployType, InputStream inputStream, DeployOptions deployOptions) {
        getWebApp().deploy(deployType, inputStream, deployOptions);
    }

    @Override
    public Completable deployAsync(DeployType deployType, InputStream inputStream, DeployOptions deployOptions) {
        return getWebApp().deployAsync(deployType, inputStream, deployOptions);
    }
}
