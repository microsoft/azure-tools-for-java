/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.google.gson.Gson;
import com.intellij.ide.AppLifecycleListener;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterOps;
import com.microsoft.azure.hdinsight.common.HDInsightHelperImpl;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.core.mvp.ui.base.AppSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelperFactory;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.service.ServiceManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.helpers.IDEHelperImpl;
import com.microsoft.intellij.helpers.MvpUIHelperImpl;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.secure.IdeaTrustStrategy;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginComponent;
import com.microsoft.tooling.msservices.components.PluginSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.TrustStrategy;
import rx.internal.util.PlatformDependent;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.FileHandler;

@Slf4j
public class AzureActionsListener implements AppLifecycleListener, PluginComponent {
    public static final String PLUGIN_ID = CommonConst.PLUGIN_ID;
    private static final String AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ";
    private static final String AZURE_TOOLS_FOLDER_DEPRECATED = "AzureToolsForIntelliJ";
    private static final FileHandler logFileHandler = null;

    private PluginSettings settings;

    @Override
    @ExceptionNotification
    public void appFrameCreated(@Nonnull List<String> commandLineArgs) {
        try {
            DefaultLoader.setPluginComponent(this);
            DefaultLoader.setUiHelper(new UIHelperImpl());
            DefaultLoader.setIdeHelper(new IDEHelperImpl());
            AppInsightsClient.setAppInsightsConfiguration(new AppInsightsConfigurationImpl());
            SchedulerProviderFactory.getInstance().init(new AppSchedulerProvider());
            MvpUIHelperFactory.getInstance().init(new MvpUIHelperImpl());
            HDInsightLoader.setHHDInsightHelper(new HDInsightHelperImpl());
            try {
                loadPluginSettings();
            } catch (final IOException e) {
                PluginUtil.displayErrorDialogAndLog("Error", "An error occurred while attempting to load settings", e);
            }

            ServiceManager.setServiceProvider(TrustStrategy.class, IdeaTrustStrategy.INSTANCE);
            CommonSettings.setUserAgent(Azure.az().config().getUserAgent());
            initAuthManage();
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
}
