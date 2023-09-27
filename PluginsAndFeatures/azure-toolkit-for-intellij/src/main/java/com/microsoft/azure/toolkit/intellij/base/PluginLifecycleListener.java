/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.base;

import com.azure.core.implementation.http.HttpClientProviders;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
import com.microsoft.azure.toolkit.ide.common.auth.IdeAzureAccount;
import com.microsoft.azure.toolkit.ide.common.experiment.ExperimentationClient;
import com.microsoft.azure.toolkit.ide.common.experiment.ExperimentationService;
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.DefaultMachineStore;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.intellij.common.settings.IntellijStore;
import com.microsoft.azure.toolkit.intellij.common.task.IntellijAzureTaskManager;
import com.microsoft.azure.toolkit.intellij.containerregistry.AzureDockerSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.DeprecatedWebAppOnLinuxDeployConfigurationFactory;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.FileHandler;

import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.TELEMETRY;
import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.TELEMETRY_INSTALLATION_ID;
import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.TELEMETRY_PLUGIN_VERSION;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PROXY;

@Slf4j
public class PluginLifecycleListener implements AppLifecycleListener, PluginStateListener {
    public static final String PLUGIN_ID = CommonConst.PLUGIN_ID;
    private static final String AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ";
    private static final String AZURE_TOOLS_FOLDER_DEPRECATED = "AzureToolsForIntelliJ";
    private static final FileHandler logFileHandler = null;

    static {
        // fix the class load problem for intellij plugin
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(PluginLifecycleListener.class.getClassLoader());
            HttpClientProviders.createInstance();
            Azure.az(AzureAccount.class);
        } catch (final Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Override
    public void appFrameCreated(@Nonnull List<String> commandLineArgs) {
        try {
            AzureTaskManager.register(new IntellijAzureTaskManager());
            AzureRxTaskManager.register();
            final String azureJson = String.format("%s%s%s", CommonConst.PLUGIN_PATH, File.separator, "azure.json");
            AzureStoreManager.register(new DefaultMachineStore(azureJson),
                IntellijStore.getInstance(), IntelliJSecureStore.getInstance());
            initProxy();
            initializeConfig();
            initializeTelemetry();
            AzureMessager.setDefaultMessager(new IntellijAzureMessager());
            IntellijAzureActionManager.register();
            CommandUtils.setEnv(EnvironmentUtil.getEnvironmentMap());
            // workaround fixes for web app on linux run configuration
            AzureDockerSupportConfigurationType.registerConfigurationFactory("Web App for Containers", DeprecatedWebAppOnLinuxDeployConfigurationFactory::new);
            final ActionManager am = ActionManager.getInstance();
            final DefaultActionGroup toolbarGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_MAIN_TOOLBAR);
            toolbarGroup.addAll((DefaultActionGroup) am.getAction("AzureToolbarGroup"));
            final DefaultActionGroup popupGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);
            popupGroup.add(am.getAction("AzurePopupGroup"));
            IdeAzureAccount.getInstance().restoreSignin(); // restore sign in
        } catch (final Throwable t) {
            log.error(t.getMessage(), t);
        }
    }

    private static void initializeTelemetry() {
        final Map<String, String> properties = new HashMap<>();
        final ApplicationInfo info = ApplicationInfo.getInstance();
        final String ideInfo = String.format("%s_%s_%s", info.getVersionName(), info.getFullVersion(), info.getBuild());
        properties.put("SessionId", UUID.randomUUID().toString());
        properties.put("IDE", ideInfo);
        properties.put("AssignmentContext", Optional.ofNullable(ExperimentationClient.getExperimentationService()).map(ExperimentationService::getAssignmentContext).orElse(StringUtils.EMPTY));
        properties.put("Plugin Version", CommonConst.PLUGIN_VERSION);
        properties.put("Installation ID", Azure.az().config().getMachineId());
        if (StringUtils.isNotBlank(Azure.az().config().getProxySource())) {
            properties.put(PROXY, "true");
        }
        AzureTelemeter.addCommonProperties(properties);
        AzureTelemeter.setEventNamePrefix("AzurePlugin.Intellij");

        final String oldVersion = AzureStoreManager.getInstance().getIdeStore().getProperty(TELEMETRY, TELEMETRY_PLUGIN_VERSION);
        final String newVersion = CommonConst.PLUGIN_VERSION;
        if (StringUtils.isBlank(oldVersion)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.install-plugin.version", newVersion));
        } else if (StringUtils.isNotBlank(oldVersion) && !newVersion.equalsIgnoreCase(oldVersion)) {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.upgrade-plugin.from|to", oldVersion, newVersion));
        }
        AzureTelemeter.log(AzureTelemetry.Type.INFO, OperationBundle.description("user/system.load-plugin.version", newVersion));
    }

    private static void initializeConfig() {
        String installId = AzureStoreManager.getInstance().getIdeStore().getProperty(null, TELEMETRY_INSTALLATION_ID);

        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.getHashMac();
        }
        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.hash(PermanentInstallationID.get());
        }

        AzureConfigInitializer.initialize(installId, "Azure Toolkit for IntelliJ", CommonConst.PLUGIN_VERSION);
        if (StringUtils.isNotBlank(Azure.az().config().getCloud())) {
            Azure.az(AzureCloud.class).setByName(Azure.az().config().getCloud());
        }
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
