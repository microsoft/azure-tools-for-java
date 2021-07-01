/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.azure.core.util.Configuration;
import com.intellij.util.net.HttpConfigurable;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;

import java.net.InetSocketAddress;
import java.util.Objects;

public class ProxyUtils {
    public static void initProxy() {
        final HttpConfigurable instance = HttpConfigurable.getInstance();
        final ProxyManager proxyManager = ProxyManager.getInstance();
        proxyManager.init();
        String source = "system";
        if (instance != null && instance.USE_HTTP_PROXY) {
            proxyManager.configure(instance.PROXY_HOST, instance.PROXY_PORT);
            source = "intellij";
        }
        if (source != null && Objects.nonNull(proxyManager.getProxy())) {
            Log.info(String.format("Use %s proxy: %s:%s", source, TextUtils.cyan(proxyManager.getHttpProxyHost()),
                TextUtils.cyan(Integer.toString(proxyManager.getHttpProxyPort()))));

            // set proxy for azure-identity according to https://docs.microsoft.com/en-us/azure/developer/java/sdk/proxying
            Azure.az().config().setHttpProxy((InetSocketAddress) proxyManager.getProxy().address());
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_HTTP_PROXY,
                String.format("http://%s:%s", proxyManager.getHttpProxyHost(), proxyManager.getHttpProxyPort()));
        }
    }
}
