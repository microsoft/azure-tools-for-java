/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.azure.core.implementation.http.HttpClientProviders;
import com.azure.core.management.AzureEnvironment;
import com.google.gson.Gson;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.exception.ExceptionUtils;
import com.microsoft.azure.cosmosspark.CosmosSparkClusterOpsCtrl;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterOps;
import com.microsoft.azure.hdinsight.common.HDInsightHelperImpl;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
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
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.WebAppOnLinuxDeployConfigurationFactory;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.core.mvp.ui.base.AppSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelperFactory;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.service.ServiceManager;
import com.microsoft.intellij.helpers.IDEHelperImpl;
import com.microsoft.intellij.helpers.MvpUIHelperImpl;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.secure.IdeaTrustStrategy;
import com.microsoft.intellij.serviceexplorer.NodeActionsMap;
import com.microsoft.intellij.util.IntelliJCommandUtils;
import com.microsoft.intellij.util.NetworkDiagnose;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginComponent;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import lombok.extern.slf4j.Slf4j;
import lombok.Lombok;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.TrustStrategy;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;
import rx.internal.util.PlatformDependent;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;

import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.OPERATION_NAME;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.SERVICE_NAME;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PROXY;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;

@Slf4j
public class AzureActionsListener implements AppLifecycleListener, PluginComponent {
    public static final String PLUGIN_ID = CommonConst.PLUGIN_ID;
    private static final String AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ";
    private static final String AZURE_TOOLS_FOLDER_DEPRECATED = "AzureToolsForIntelliJ";
    private static final FileHandler logFileHandler = null;

    private PluginSettings settings;

    static {
        // fix the class load problem for intellij plugin
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(AzureActionsListener.class.getClassLoader());
            HttpClientProviders.createInstance();
            Azure.az(AzureAccount.class);
            Hooks.onErrorDropped(ex -> {
                final Throwable cause = findExceptionInExceptionChain(ex, Arrays.asList(InterruptedException.class, UnknownHostException.class));
                if (cause instanceof InterruptedException) {
                    log.info(ex.getMessage());
                } else if (cause instanceof UnknownHostException) {
                    NetworkDiagnose.checkAzure(AzureEnvironment.AZURE).publishOn(Schedulers.parallel()).subscribe(sites -> {
                        final Map<String, String> properties = new HashMap<>();
                        properties.put(SERVICE_NAME, SYSTEM);
                        properties.put(OPERATION_NAME, "network_diagnose");
                        properties.put("sites", sites);
                        properties.put(PROXY, Boolean.toString(StringUtils.isNotBlank(Azure.az().config().getProxySource())));
                        AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
                    });
                } else {
                    throw Lombok.sneakyThrow(ex);
                }
            });
        } catch (final Throwable e) {
            log.error(e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "platform/common.init_plugin")
    public void appFrameCreated(@Nonnull List<String> commandLineArgs) {
        try {
            DefaultLoader.setPluginComponent(this);
            DefaultLoader.setUiHelper(new UIHelperImpl());
            DefaultLoader.setIdeHelper(new IDEHelperImpl());
            AzureTaskManager.register(new IntellijAzureTaskManager());
            CommandUtils.register(new IntelliJCommandUtils());
            AzureRxTaskManager.register();
            AzureStoreManager.register(new DefaultMachineStore(PluginHelper.getTemplateFile("azure.json")),
                IntellijStore.getInstance(), IntelliJSecureStore.getInstance());
            AzureInitializer.initialize();
            AzureMessager.setDefaultMessager(new IntellijAzureMessager());
            IntellijAzureActionManager.register();
            Node.setNode2Actions(NodeActionsMap.NODE_ACTIONS);
            SchedulerProviderFactory.getInstance().init(new AppSchedulerProvider());
            MvpUIHelperFactory.getInstance().init(new MvpUIHelperImpl());

            HDInsightLoader.setHHDInsightHelper(new HDInsightHelperImpl());
            // workaround fixes for web app on linux run configuration
            AzureDockerSupportConfigurationType.registerConfigurationFactory("Web App for Containers", DeprecatedWebAppOnLinuxDeployConfigurationFactory::new);
            try {
                loadPluginSettings();
            } catch (final IOException e) {
                PluginUtil.displayErrorDialogAndLog("Error", "An error occurred while attempting to load settings", e);
            }
            if (!AzurePlugin.IS_ANDROID_STUDIO) {
                // enable spark serverless node subscribe actions
                ServiceManager.setServiceProvider(CosmosSparkClusterOpsCtrl.class,
                    new CosmosSparkClusterOpsCtrl(CosmosSparkClusterOps.getInstance()));

                ServiceManager.setServiceProvider(TrustStrategy.class, IdeaTrustStrategy.INSTANCE);
                initAuthManage();
                final ActionManager am = ActionManager.getInstance();
                final DefaultActionGroup toolbarGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_MAIN_TOOLBAR);
                toolbarGroup.addAll((DefaultActionGroup) am.getAction("AzureToolbarGroup"));
                final DefaultActionGroup popupGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);
                popupGroup.add(am.getAction("AzurePopupGroup"));
            }
        } catch (final Throwable t) {
            log.error(t.getMessage(), t);
        }
        try {
            PlatformDependent.isAndroid();
        } catch (final Throwable ignored) {
            DefaultLoader.getUIHelper().showError("A problem with your Android Support plugin setup is preventing the"
                + " Azure Toolkit from functioning correctly (Retrofit2 and RxJava failed to initialize)"
                + ".\nTo fix this issue, try disabling the Android Support plugin or installing the "
                + "Android SDK", "Azure Toolkit for IntelliJ");
            // DefaultLoader.getUIHelper().showException("Android Support Error: isAndroid() throws " + ignored
            //         .getMessage(), ignored, "Error Android", true, false);
        }
    }

    private void initAuthManage() {
        try {
            final String baseFolder = FileUtil.getDirectoryWithinUserHome(AZURE_TOOLS_FOLDER).toString();
            final String deprecatedFolder = FileUtil.getDirectoryWithinUserHome(AZURE_TOOLS_FOLDER_DEPRECATED).toString();
            CommonSettings.setUpEnvironment(baseFolder, deprecatedFolder);
        } catch (final IOException ex) {
            log.error("initAuthManage()", ex);
        }
    }

    @Override
    public PluginSettings getSettings() {
        return settings;
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    private void loadPluginSettings() throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
            AzureActionsListener.class.getResourceAsStream("/settings.json")))) {
            final StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            final Gson gson = new Gson();
            settings = gson.fromJson(sb.toString(), PluginSettings.class);
        }
    }

    private static Throwable findExceptionInExceptionChain(Throwable ex, List<Class> classes) {
        for (final Throwable cause : ExceptionUtils.getThrowableList(ex)) {
            for (final Class clz : classes) {
                if (cause != null && clz.isAssignableFrom(cause.getClass())) {
                    return cause;
                }
            }
        }
        return null;
    }
}
