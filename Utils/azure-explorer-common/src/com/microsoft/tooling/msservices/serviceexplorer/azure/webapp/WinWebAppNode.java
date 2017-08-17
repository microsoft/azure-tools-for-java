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

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;

import java.util.HashMap;
import java.util.Map;

public class WinWebAppNode extends WebAppNode {
    private final WebApp webApp;

    /**
     * Constructor.
     *
     * @param parent parent Node.
     * @param app    ResourceEx with Web App instance.
     */
    public WinWebAppNode(WebAppModule parent, ResourceEx<WebApp> app) {
        super(app.getResource().id(), app.getResource().name(), parent, app.getResource().state());
        webApp = app.getResource();
        subscriptionId = app.getSubscriptionId();

        loadActions();
    }

    @Override
    public WebApp getWebApp() {
        return webApp;
    }

    @Override
    public String getRunState() {
        return webApp.inner().state();
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.webApp.regionName());
        return properties;
    }

    @Override
    public String getWebAppId() {
        return webApp.id();
    }

    @Override
    public String getWebAppName() {
        return webApp.name();
    }

    @Override
    public void stopWebApp() {
        webApp.stop();
    }

    @Override
    public void startWebApp() {
        webApp.start();
    }

    @Override
    public void restartWebApp() {
        webApp.restart();
    }
}