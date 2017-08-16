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
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LinuxWebAppNode extends WebAppNode {
    private final SiteInner siteInner;
    private WebApp webApp;

    /**
     * Constructor.
     *
     * @param parent parent Node.
     * @param app    ResourceEx with Web App instance.
     * @param icon   Node icon path.
     */
    public LinuxWebAppNode(WebAppModule parent, ResourceEx app, String icon) {
        super(((SiteInner) app.getResource()).id(), ((SiteInner) app.getResource()).name(), parent, icon, true);
        siteInner = (SiteInner) app.getResource();
        subscriptionId = app.getSubscriptionId();

        loadActions();
    }

    @Override
    public String getRunState() {
        return webApp == null ? siteInner.state() : webApp.state();
    }

    @Override
    public WebApp getWebApp() {
        if (webApp == null) {
            try {
                return AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, siteInner.id());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return webApp;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.siteInner.location());
        return properties;
    }

    @Override
    public String getWebAppId() {
        return siteInner.id();
    }

    @Override
    public String getWebAppName() {
        return siteInner.name();
    }


    @Override
    public void stopWebApp() {
        if (webApp == null) {
            try {
                webApp = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, siteInner.id());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (webApp != null) {
            webApp.stop();
        }
    }

    @Override
    public void startWebApp() {
        if (webApp == null) {
            try {
                webApp = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, siteInner.id());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (webApp != null) {
            webApp.start();
        }
    }

    @Override
    public void restartWebApp() {
        if (webApp == null) {
            try {
                webApp = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, siteInner.id());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (webApp != null) {
            webApp.restart();
        }
    }
}
