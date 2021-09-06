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
import com.intellij.openapi.diagnostic.Logger;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.exception.ExceptionUtils;
import com.microsoft.azure.cosmosspark.CosmosSparkClusterOpsCtrl;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterOps;
import com.microsoft.azure.hdinsight.common.HDInsightHelperImpl;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.intellij.common.task.IntellijAzureTaskManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.core.mvp.ui.base.AppSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelperFactory;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.service.ServiceManager;
import com.microsoft.intellij.helpers.IDEHelperImpl;
import com.microsoft.intellij.helpers.MvpUIHelperImpl;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.secure.IdeaSecureStore;
import com.microsoft.intellij.secure.IdeaTrustStrategy;
import com.microsoft.intellij.serviceexplorer.NodeActionsMap;
import com.microsoft.intellij.util.NetworkDiagnose;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.ui.UIFactory;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginComponent;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.TrustStrategy;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;
import rx.internal.util.PlatformDependent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import static com.microsoft.azuretools.Constants.FILE_NAME_CORE_LIB_LOG;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PROXY;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.OPERATION_NAME;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.SERVICE_NAME;

@Slf4j
public class AzureActionsListener implements AppLifecycleListener, PluginComponent {
    public static final String PLUGIN_ID = CommonConst.PLUGIN_ID;
    private static final Logger LOG = Logger.getInstance(AzureActionsListener.class);
    private static final String AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ";
    private static final String AZURE_TOOLS_FOLDER_DEPRECATED = "AzureToolsForIntelliJ";
    private static FileHandler logFileHandler = null;

    private PluginSettings settings;


    static {
        // fix the class load problem for intellij plugin
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(AzureActionsListener.class.getClassLoader());
            HttpClientProviders.createInstance();
            Azure.az(AzureAccount.class);
            Hooks.onErrorDropped(ex -> {
                Throwable cause = findExceptionInExceptionChain(ex, Arrays.asList(InterruptedException.class, UnknownHostException.class));
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
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Override
    public void appFrameCreated(@NotNull List<String> commandLineArgs) {
        DefaultLoader.setPluginComponent(this);
        DefaultLoader.setUiHelper(new UIHelperImpl());
        DefaultLoader.setIdeHelper(new IDEHelperImpl());
        AzureTaskManager.register(new IntellijAzureTaskManager());
        AzureRxTaskManager.register();
        AzureMessager.setDefaultMessager(new IntellijAzureMessager());
        IntellijAzureActionManager.register();
        Node.setNode2Actions(NodeActionsMap.NODE_ACTIONS);
        SchedulerProviderFactory.getInstance().init(new AppSchedulerProvider());
        MvpUIHelperFactory.getInstance().init(new MvpUIHelperImpl());

        HDInsightLoader.setHHDInsightHelper(new HDInsightHelperImpl());
        try {
            loadPluginSettings();
        } catch (IOException e) {
            PluginUtil.displayErrorDialogAndLog("Error", "An error occurred while attempting to load settings", e);
        }
        AzureInitializer.initialize();
        if (!AzurePlugin.IS_ANDROID_STUDIO) {
            AzureStoreManager.register(new FileStore(), new IntellijStore(), IdeaSecureStore.getInstance());
            // enable spark serverless node subscribe actions
            ServiceManager.setServiceProvider(CosmosSparkClusterOpsCtrl.class,
                    new CosmosSparkClusterOpsCtrl(CosmosSparkClusterOps.getInstance()));

            ServiceManager.setServiceProvider(TrustStrategy.class, IdeaTrustStrategy.INSTANCE);
            initAuthManage();
            ActionManager am = ActionManager.getInstance();
            DefaultActionGroup toolbarGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_MAIN_TOOLBAR);
            toolbarGroup.addAll((DefaultActionGroup) am.getAction("AzureToolbarGroup"));
            DefaultActionGroup popupGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);
            popupGroup.add(am.getAction("AzurePopupGroup"));
        }
        try {
            PlatformDependent.isAndroid();
        } catch (Throwable ignored) {
            DefaultLoader.getUIHelper().showError("A problem with your Android Support plugin setup is preventing the"
                    + " Azure Toolkit from functioning correctly (Retrofit2 and RxJava failed to initialize)"
                    + ".\nTo fix this issue, try disabling the Android Support plugin or installing the "
                    + "Android SDK", "Azure Toolkit for IntelliJ");
            // DefaultLoader.getUIHelper().showException("Android Support Error: isAndroid() throws " + ignored
            //         .getMessage(), ignored, "Error Android", true, false);
        }
    }

    private void initAuthManage() {
        if (CommonSettings.getUiFactory() == null) {
            CommonSettings.setUiFactory(new UIFactory());
        }
        try {
            final String baseFolder = FileUtil.getDirectoryWithinUserHome(AZURE_TOOLS_FOLDER).toString();
            final String deprecatedFolder = FileUtil.getDirectoryWithinUserHome(AZURE_TOOLS_FOLDER_DEPRECATED).toString();
            CommonSettings.setUpEnvironment(baseFolder, deprecatedFolder);
            initLoggerFileHandler();
        } catch (IOException ex) {
            LOG.error("initAuthManage()", ex);
        }
    }

    private void initLoggerFileHandler() {
        try {
            String loggerFilePath = Paths.get(CommonSettings.getSettingsBaseDir(), FILE_NAME_CORE_LIB_LOG).toString();
            System.out.println("Logger path:" + loggerFilePath);
            logFileHandler = new FileHandler(loggerFilePath, false);
            java.util.logging.Logger l = java.util.logging.Logger.getLogger("");
            logFileHandler.setFormatter(new SimpleFormatter());
            l.addHandler(logFileHandler);
            // TODO: use environment variable to set level
            l.setLevel(Level.INFO);
            l.info("=== Log session started ===");
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("initLoggerFileHandler()", e);
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
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    AzureActionsListener.class.getResourceAsStream("/settings.json")));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            Gson gson = new Gson();
            settings = gson.fromJson(sb.toString(), PluginSettings.class);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static Throwable findExceptionInExceptionChain(Throwable ex, List<Class> classes) {
        for (Throwable cause : ExceptionUtils.getThrowableList(ex)) {
            for (Class clz : classes) {
                if (cause != null && clz.isAssignableFrom(cause.getClass())) {
                    return cause;
                }
            }
        }
        return null;
    }
}
