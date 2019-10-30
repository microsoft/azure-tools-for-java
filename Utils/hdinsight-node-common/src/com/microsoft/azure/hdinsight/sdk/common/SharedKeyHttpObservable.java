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
package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.SharedKeyCredential;
import com.microsoft.azure.storage.core.Utility;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import rx.Observable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SharedKeyHttpObservable extends HttpObservable {
    public static String ApiVersion = "2018-11-09";
    private SharedKeyCredential cred;
    private HeaderGroup defaultHeaders;

    public SharedKeyHttpObservable(String accountName, String accessKey) {
        defaultHeaders = new HeaderGroup();
        defaultHeaders.addHeader(new BasicHeader("x-ms-client-request-id", UUID.randomUUID().toString()));
        defaultHeaders.addHeader(new BasicHeader("x-ms-date", Utility.getGMTTime()));
        defaultHeaders.addHeader(new BasicHeader("x-ms-version", ApiVersion));
        defaultHeaders.addHeader(new BasicHeader("authorization", ""));
        defaultHeaders.addHeader(new BasicHeader("Content-Type", "application/json"));

        setDefaultHeaderGroup(defaultHeaders);
        try {
            this.cred = new SharedKeyCredential(accountName, accessKey);
        } catch (IllegalArgumentException ex) {
            log().warn("Create shared key credential encounter exception", ex);
            throw new IllegalArgumentException("Can't create shared key credential.Please check access key");
        }
    }

    public SharedKeyHttpObservable setAuthorization(@NotNull HttpRequestBase req, List<NameValuePair> pairs, List<Header> addOrReplaceHeaders) {
        HeaderGroup headerForAuth = getDefaultHeaderGroup().copy();
        if(addOrReplaceHeaders != null) {
            for(Header header : addOrReplaceHeaders) {
                headerForAuth.updateHeader(header);
            }
        }

        String key = cred.generateSharedKey(req, headerForAuth, pairs);
        getDefaultHeaderGroup().updateHeader(new BasicHeader("authorization", key));
        return this;
    }

    public SharedKeyHttpObservable setContentLength(@NotNull String len) {
        getDefaultHeaderGroup().updateHeader(new BasicHeader("Content-Length", len));
        return this;
    }

    public SharedKeyHttpObservable removeContentLength() {
        getDefaultHeaderGroup().removeHeader(getDefaultHeaderGroup().getFirstHeader("Content-Length"));
        return this;
    }

    public SharedKeyHttpObservable setContentType(@NotNull String type) {
        getDefaultHeaderGroup().updateHeader(new BasicHeader("Content-Type", type));
        return this;
    }

    @Override
    public Observable<CloseableHttpResponse> request(@NotNull final HttpRequestBase httpRequest,
                                                     @Nullable final HttpEntity entity,
                                                     @Nullable final List<NameValuePair> parameters,
                                                     @Nullable final List<Header> addOrReplaceHeaders) {
        HttpEntity entityFromRequest = httpRequest instanceof HttpEntityEnclosingRequestBase
                ? ((HttpEntityEnclosingRequestBase) httpRequest).getEntity()
                : null;

        if (entityFromRequest != null) {
            // Job deployment needs to set content-length to generate shared key
            // httpclient auto adds this header and calculates length when executing
            // so remove this header after key generation otherwise header already exists exp happens
            // MUST follow the order when content length is needed to generate key
            setContentLength(String.valueOf(entityFromRequest.getContentLength()));
            this.setAuthorization(httpRequest, parameters, addOrReplaceHeaders);
            this.removeContentLength();
        } else {
            this.setAuthorization(httpRequest, parameters, addOrReplaceHeaders);
        }

        return super.request(httpRequest, entityFromRequest, parameters, addOrReplaceHeaders);
    }

    @Override
    @Nullable
    public Header[] getDefaultHeaders() throws IOException {
        return defaultHeaders.getAllHeaders();
    }
}