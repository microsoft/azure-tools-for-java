/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.webapp.webappconfig;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.utils.IProgressIndicator;
import com.microsoft.azuretools.utils.WebAppUtils;

public class WebDeployUtil {

    private static final String CREATE_WEBAPP = "Creating new WebApp...";
    private static final String CREATE_SUCCESSFUL = "WebApp created...";
    private static final String CREATE_FALIED = "Failed to create WebApp. Error: %s ...";
    private static final String DEPLOY_JDK = "Deploying custom JDK...";
    private static final String JDK_SUCCESSFUL = "Custom JDK deployed successfully...";
    private static final String JDK_FAILED = "Failed to deploy custom JDK";


    public static WebApp createWebAppWithMsg(
            WebAppSettingModel webAppSettingModel, IProgressIndicator handler) throws Exception {
        handler.setText(CREATE_WEBAPP);
        WebApp webApp = null;
        try {
            webApp = AzureWebAppMvpModel.getInstance().createWebApp(webAppSettingModel);
        } catch (Exception e) {
            handler.setText(String.format(CREATE_FALIED, e.getMessage()));
            throw new Exception(e);
        }

        if (!WebAppSettingModel.JdkChoice.DEFAULT.toString().equals(webAppSettingModel.getJdkChoice())) {
            handler.setText(DEPLOY_JDK);
            try {
                WebAppUtils.deployCustomJdk(webApp, webAppSettingModel.getJdkUrl(),
                        WebContainer.fromString(webAppSettingModel.getWebContainer()),
                        handler);
                handler.setText(JDK_SUCCESSFUL);
            } catch (Exception e) {
                handler.setText(JDK_FAILED);
                throw new Exception(e);
            }
        }
        handler.setText(CREATE_SUCCESSFUL);

        return webApp;
    }
}
