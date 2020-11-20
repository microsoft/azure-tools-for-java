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

package com.microsoft.azure.toolkit.intellij.webapp;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceComboBoxModel;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class WebAppComboBoxModel extends AppServiceComboBoxModel<WebApp> {

    private String runtime;
    private WebAppSettingModel webAppSettingModel;

    public WebAppComboBoxModel(final ResourceEx<WebApp> resourceEx) {
        super(resourceEx);
        final WebApp webApp = resourceEx.getResource();
        this.runtime = webApp.operatingSystem() == OperatingSystem.WINDOWS ?
                       String.format("%s-%s-%s", "Windows", webApp.javaContainer(), webApp.javaVersion()) :
                       String.format("%s-%s %s", "Linux", webApp.linuxFxVersion().split("\\|")[0],
                                     webApp.linuxFxVersion().split("\\|")[1]);
    }

    public WebAppComboBoxModel(WebAppSettingModel webAppSettingModel) {
        this.resourceId = webAppSettingModel.getWebAppId();
        // In case recover from configuration, get the app name from resource id
        this.appName =
                StringUtils.isEmpty(webAppSettingModel.getWebAppName()) && StringUtils.isNotEmpty(resourceId) ?
                AzureMvpModel.getSegment(resourceId, "sites") :
                webAppSettingModel.getWebAppName();
        this.resourceGroup = webAppSettingModel.getResourceGroup();
        this.os = webAppSettingModel.getOS().name();
        this.runtime = webAppSettingModel.getOS() == OperatingSystem.WINDOWS ?
                       String.format("%s-%s-%s", "Windows", webAppSettingModel.getWebContainer(), webAppSettingModel.getJdkVersion()) :
                       String.format("%s-%s %s", "Linux", webAppSettingModel.getLinuxRuntime().stack(), webAppSettingModel.getLinuxRuntime().version());
        this.runtime = webAppSettingModel.getOS() == OperatingSystem.LINUX ?
                       webAppSettingModel.getLinuxRuntime().toString() : webAppSettingModel.getWebContainer();
        this.subscriptionId = webAppSettingModel.getSubscriptionId();
        this.isNewCreateResource = webAppSettingModel.isCreatingNew();
        this.webAppSettingModel = webAppSettingModel;
    }

}
