/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.rest;

import com.microsoft.azuretools.adauth.PromptBehavior;
import com.microsoft.azuretools.sdkmanage.AzureManagerBase;

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

    abstract AzureManagerBase getPreAccessTokenAzureManager();

    public String getAccessToken() throws IOException {
        if (tenantId == null) throw new IllegalArgumentException("tenantId is null");
        if (resource == null) throw new IllegalArgumentException("resource is null");

        return getPreAccessTokenAzureManager().getAccessToken(tenantId, resource, promptBehavior);
    }
}
