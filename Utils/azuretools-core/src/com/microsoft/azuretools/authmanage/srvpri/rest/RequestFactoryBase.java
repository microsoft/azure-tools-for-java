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

package com.microsoft.azuretools.authmanage.srvpri.rest;

import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by vlashch on 8/29/16.
 */
abstract class RequestFactoryBase implements IRequestFactory {
    private final static Logger LOGGER = Logger.getLogger(RequestFactoryBase.class.getName());
    protected String apiVersion;
    protected String urlPrefix;
    protected String tenantId;
    protected String resource;
    protected PromptBehavior promptBehavior = PromptBehavior.Auto;

    public String getApiVersion(){
        if (apiVersion == null) throw new NullPointerException("this.apiVersion is null");
        return apiVersion;
    }

    public String getUrlPattern() {
        return getUrlPrefix() + "%s?%s&" + getApiVersion();
    }

    public String getUrlPatternParamless() {
        return getUrlPrefix() + "%s?" + getApiVersion();
    }

    public String getUrlPrefix() {
        if (urlPrefix == null) throw new NullPointerException("this.urlPrefix is null");
        return urlPrefix;
    }

    abstract AccessTokenAzureManager getPreAccessTokenAzureManager();

    public String getAccessToken() throws IOException {
        if (tenantId == null) throw new IllegalArgumentException("tenantId is null");
        if (resource == null) throw new IllegalArgumentException("resource is null");

        return getPreAccessTokenAzureManager().getAccessToken(tenantId, resource, promptBehavior);
    }
}
