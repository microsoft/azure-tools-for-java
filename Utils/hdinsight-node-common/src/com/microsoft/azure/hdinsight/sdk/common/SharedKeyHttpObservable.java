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
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SharedKeyHttpObservable extends HttpObservable {
    public static String ApiVersion = "2018-11-09";
    public static String DefaultContentLength = "0";
    private SharedKeyCredential cred;

    public SharedKeyHttpObservable(String accountName, String accessKey) {
        HeaderGroup defaultHeaders = new HeaderGroup();
        defaultHeaders.addHeader(new BasicHeader("x-ms-client-request-id", UUID.randomUUID().toString()));
        defaultHeaders.addHeader(new BasicHeader("x-ms-date", Utility.getGMTTime()));
        defaultHeaders.addHeader(new BasicHeader("x-ms-version", ApiVersion));
        defaultHeaders.addHeader(new BasicHeader("authorization", ""));
        defaultHeaders.addHeader(new BasicHeader("Content-Type", "application/json"));

        setDefaultHeaderGroup(defaultHeaders);
        try {
            this.cred = new SharedKeyCredential(accountName, accessKey);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Cannot init shared key credential");
        }
    }

    public SharedKeyHttpObservable setAuthorization(@NotNull HttpRequestBase req, List<NameValuePair> pairs) {
        String key = cred.generateSharedKey(req, getDefaultHeaderGroup(), pairs);
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

    public List<Header> getHeaderList() {
        return Arrays.asList(getDefaultHeaderGroup().getAllHeaders());
    }
}
