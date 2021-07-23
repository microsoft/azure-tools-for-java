/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.azure.core.util.Configuration;
import com.intellij.util.net.HttpConfigurable;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;

import java.net.InetSocketAddress;

public class ProxyUtils {
    public static void initProxy() {
        final HttpConfigurable instance = HttpConfigurable.getInstance();
        if (instance != null && instance.USE_HTTP_PROXY) {
            String source = "intellij";
            AzureMessager.getMessager().info(AzureString.format("Use {0} proxy: {1}:{2}",
                    source, instance.PROXY_HOST, Integer.toString(instance.PROXY_PORT)));

            // set proxy for azure-identity according to https://docs.microsoft.com/en-us/azure/developer/java/sdk/proxying
            Azure.az().config().setHttpProxy(new InetSocketAddress(instance.PROXY_HOST, instance.PROXY_PORT));
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_HTTP_PROXY,
                    String.format("http://%s:%s", instance.PROXY_HOST, instance.PROXY_PORT));
        }
    }
}
