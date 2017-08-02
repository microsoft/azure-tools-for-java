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

import org.jdom.Element;

public class WebAppSettingModel {

    private static final String WEB_APP_ID = "WebAppId";
    private static final String SUBSCRIPTION_ID = "SubscriptionId";
    private static final String WEB_APP_URL = "webAppUrl";
    private static final String USING_EXISTING = "usingExisting";
    private static final String DEPLOY_TO_ROOT = "deployToRoot";
    private static final String TARGET_PATH = "targetPath";
    private static final String TARGET_NAME = "targetName";

    private String webAppId = "";
    private String subscriptionId = "";
    private String webAppUrl = "";
    private String targetPath = "";
    private String targetName = "";
    private boolean deployToRoot = true;
    private boolean creatingNew = false;

    public String getWebAppId() {
        return webAppId;
    }

    public String getSubscriptionId() {
        return this.subscriptionId;
    }

    public void setWebAppId(String webAppId) {
        this.webAppId = webAppId;
    }

    public void setSubscriptionId(String subId) {
        this.subscriptionId = subId;
    }

    public String getWebAppUrl() {
        return webAppUrl;
    }

    public void setWebAppUrl(String webAppUrl) {
        this.webAppUrl = webAppUrl;
    }

    public boolean isDeployToRoot() {
        return deployToRoot;
    }

    public void setDeployToRoot(boolean deployToRoot) {
        this.deployToRoot = deployToRoot;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public boolean isCreatingNew() {
        return creatingNew;
    }

    public void setCreatingNew(boolean creatingNew) {
        this.creatingNew = creatingNew;
    }


    public void readExternal(Element element) {
        if (element != null) {
            this.webAppId = element.getAttributeValue(WEB_APP_ID);
            this.subscriptionId = element.getAttributeValue(SUBSCRIPTION_ID);
            this.webAppUrl = element.getAttributeValue(WEB_APP_URL);
            this.creatingNew = Boolean.valueOf(element.getAttributeValue(USING_EXISTING));
            this.deployToRoot = Boolean.valueOf(element.getAttributeValue(DEPLOY_TO_ROOT));
            this.targetPath = element.getAttributeValue(TARGET_PATH);
            this.targetName = element.getAttributeValue(TARGET_NAME);
        }
    }

    public void writeExternal(Element element) {
        element.setAttribute(WEB_APP_ID, this.webAppId);
        element.setAttribute(SUBSCRIPTION_ID, this.subscriptionId);
        element.setAttribute(WEB_APP_URL, this.webAppUrl);
        element.setAttribute(USING_EXISTING, String.valueOf(this.creatingNew));
        element.setAttribute(DEPLOY_TO_ROOT, String.valueOf(this.deployToRoot));
        element.setAttribute(TARGET_PATH, this.targetPath);
        element.setAttribute(TARGET_NAME, this.targetName);
    }
}
