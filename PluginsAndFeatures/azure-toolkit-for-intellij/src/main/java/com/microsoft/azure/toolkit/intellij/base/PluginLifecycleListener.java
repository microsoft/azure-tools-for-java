/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.base;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
import com.microsoft.azure.toolkit.ide.common.auth.IdeAzureAccount;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.DefaultMachineStore;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore;
import com.microsoft.azure.toolkit.intellij.containerregistry.AzureDockerSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.DeprecatedWebAppOnLinuxDeployConfigurationFactory;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.util.List;
import java.util.logging.FileHandler;

@Slf4j
public class PluginLifecycleListener implements AppLifecycleListener, PluginStateListener {
    public static final String PLUGIN_ID = CommonConst.PLUGIN_ID;
    private static final String AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ";
    private static final String AZURE_TOOLS_FOLDER_DEPRECATED = "AzureToolsForIntelliJ";
    private static final FileHandler logFileHandler = null;

    @Override
    public void appFrameCreated(@Nonnull List<String> commandLineArgs) {
        try {
            AzureRxTaskManager.register();
            final String azureJson = String.format("%s%s%s", CommonConst.PLUGIN_PATH, File.separator, "azure.json");
            AzureStoreManager.register(new DefaultMachineStore(azureJson), IntelliJSecureStore.getInstance());
            initProxy();
            initializeTelemetry();
            // workaround fixes for web app on linux run configuration
            AzureDockerSupportConfigurationType.registerConfigurationFactory("Web App for Containers", DeprecatedWebAppOnLinuxDeployConfigurationFactory::new);
            IdeAzureAccount.getInstance().restoreSignin(); // restore sign in
        } catch (final Throwable t) {
            log.error(t.getMessage(), t);
        }
    }

    private static void initializeTelemetry() {
        final AzureConfiguration config = Azure.az().config();
        final String oldVersion = config.getVersion();
        final String newVersion = CommonConst.PLUGIN_VERSION;
        if (StringUtils.isBlank(oldVersion)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.install-plugin.version", newVersion));
        } else if (StringUtils.isNotBlank(oldVersion) && !newVersion.equalsIgnoreCase(oldVersion)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.upgrade-plugin.from|to", oldVersion, newVersion));
        }
        AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.load-plugin.version", newVersion));
    }

    private static void initProxy() {
        final HttpConfigurable instance = HttpConfigurable.getInstance();
        if (instance != null && instance.USE_HTTP_PROXY) {
            final ProxyInfo proxy = ProxyInfo.builder()
                .source("intellij")
                .host(instance.PROXY_HOST)
                .port(instance.PROXY_PORT)
                .username(instance.getProxyLogin())
                .password(instance.getPlainProxyPassword())
                .build();
            Azure.az().config().setProxyInfo(proxy);
            ProxyManager.getInstance().applyProxy();
        }
        final CertificateManager certificateManager = CertificateManager.getInstance();
        Azure.az().config().setSslContext(certificateManager.getSslContext());
        HttpsURLConnection.setDefaultSSLSocketFactory(certificateManager.getSslContext().getSocketFactory());
    }

    @Override
    public void install(@Nonnull IdeaPluginDescriptor ideaPluginDescriptor) {
        if (ideaPluginDescriptor.getPluginId().getIdString().equalsIgnoreCase(CommonConst.PLUGIN_ID)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.install-plugin.version", CommonConst.PLUGIN_VERSION));
        }
    }

    @Override
    public void uninstall(@Nonnull IdeaPluginDescriptor ideaPluginDescriptor) {
        if (ideaPluginDescriptor.getPluginId().getIdString().equalsIgnoreCase(CommonConst.PLUGIN_ID)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.uninstall-plugin.version", CommonConst.PLUGIN_VERSION));
        }
    }
}
