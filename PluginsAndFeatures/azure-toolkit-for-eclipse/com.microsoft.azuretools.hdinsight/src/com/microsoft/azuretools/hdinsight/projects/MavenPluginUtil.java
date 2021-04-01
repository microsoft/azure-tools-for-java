/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.hdinsight.projects;

import com.microsoft.azuretools.core.utils.PluginUtil;

public class MavenPluginUtil {
    // Forward link is not allowed here
    private static final String m2ePluginMarketplaceURL = "http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1774116";
    private static final String m2ePluginSymbolicName = "org.eclipse.m2e.lifecyclemapping.defaults";
    // FWLink for http://www.eclipse.org/m2e/
    private static final String m2eManualInstalURL = "https://go.microsoft.com/fwlink/?linkid=861450";

    public static boolean checkMavenPluginInstallation() {
        return PluginUtil.checkPlugInInstallation(m2ePluginSymbolicName);
    }

    public static void installMavenPlugin() {
        PluginUtil.forceInstallPluginUsingMarketplace(m2ePluginSymbolicName, m2ePluginMarketplaceURL, m2eManualInstalURL);
    }
}
