/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.bicep.activities;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginInstaller;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.ui.EditorNotifications;
import com.microsoft.azure.toolkit.ide.common.dotnet.DotnetRuntimeHandler;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.plugins.textmate.configuration.BundleConfigBean;
import org.jetbrains.plugins.textmate.configuration.TextMateSettings;
import org.jetbrains.plugins.textmate.configuration.TextMateSettings.TextMateSettingsState;
import org.jetbrains.plugins.textmate.configuration.TextMateUserBundlesSettings;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.ProcessBuilderServerDefinition;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class BicepStartupActivity implements ProjectActivity, PluginStateListener {
    public static final String BICEP_LANGSERVER = "bicep-langserver";
    public static final String BICEP_LANG_SERVER_DLL = "Bicep.LangServer.dll";
    public static final String STDIO = "--stdio";
    public static final String BICEP = "bicep";

    @Override
    @ExceptionNotification
    @AzureOperation(name = "platform/bicep.startup_language_server")
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        final File bicep = FileUtils.getFile(CommonConst.PLUGIN_PATH, "bicep", BICEP_LANGSERVER, BICEP_LANG_SERVER_DLL);
        final String dotnet = Azure.az().config().getDotnetRuntimePath();
        final boolean isDotnetReady = StringUtils.isNotEmpty(dotnet) && DotnetRuntimeHandler.isDotnetRuntimeInstalled(dotnet);
        final boolean isBicepReady = bicep != null && bicep.exists();
        if (!(isBicepReady && isDotnetReady)) {
            if (!isDotnetReady) {
                AzureEventBus.on("dotnet_runtime.updated", new AzureEventBus.EventListener(e -> registerLanguageServerDefinition(project)));
            }
            return null;
        }
        PluginInstaller.addStateListener(this);
        registerLanguageServerDefinition(project);
        return null;
    }

    public static void registerLanguageServerDefinition(@Nonnull Project project) {
        if(project.isDisposed()){
            return;
        }
        EditorNotifications.getInstance(project).updateAllNotifications();
        final File bicep = FileUtils.getFile(CommonConst.PLUGIN_PATH, "bicep", BICEP_LANGSERVER, BICEP_LANG_SERVER_DLL);
        final String dotnet = Azure.az().config().getDotnetRuntimePath();
        final boolean isDotnetReady = StringUtils.isNotEmpty(dotnet) && DotnetRuntimeHandler.isDotnetRuntimeInstalled(dotnet);
        if (isDotnetReady) {
            final ProcessBuilder process = SystemUtils.IS_OS_WINDOWS ?
                new ProcessBuilder("powershell.exe", "./dotnet", bicep.getAbsolutePath(), STDIO) :
                new ProcessBuilder("./dotnet", bicep.getAbsolutePath(), STDIO);
            Optional.of(dotnet)
                .filter(StringUtils::isNotEmpty).map(File::new)
                .filter(File::exists).ifPresent(process::directory);
            IntellijLanguageClient.addServerDefinition(new ProcessBuilderServerDefinition(BICEP, process), project);
        }
    }

    @AzureOperation("boundary/bicep.register_textmate_bundles")
    public static synchronized void registerBicepTextMateBundle() {
        final Path bicepTextmatePath = Path.of(CommonConst.PLUGIN_PATH, "bicep", "textmate", "bicep");
        final Path bicepParamTextmatePath = Path.of(CommonConst.PLUGIN_PATH, "bicep", "textmate", "bicepparam");
        TextMateUserBundlesSettings.getInstance().addBundle(bicepTextmatePath.toString(), "bicep");
        TextMateUserBundlesSettings.getInstance().addBundle(bicepParamTextmatePath.toString(), "bicepparam");
    }

    @AzureOperation("boundary/bicep.unregister_textmate_bundles")
    public static synchronized void unregisterBicepTextMateBundle() {
        final TextMateSettingsState state = TextMateSettings.getInstance().getState();
        if (Objects.nonNull(state)) {
            final Path bicepParamTextmatePath = Path.of(CommonConst.PLUGIN_PATH, "bicep", "textmate", "bicepparam");
            final Collection<BundleConfigBean> bundles = state.getBundles();
            if (bundles.stream().anyMatch(b -> "bicep".equals(b.getName()))) {
                final ArrayList<BundleConfigBean> newBundles = new ArrayList<>(bundles);
                newBundles.removeIf(bundle -> StringUtils.equalsAnyIgnoreCase(bundle.getName(), "bicep", "bicepparam"));
                state.setBundles(newBundles);
            }
        }
    }

    @Override
    public void install(@Nonnull IdeaPluginDescriptor ideaPluginDescriptor) {
        if (ideaPluginDescriptor.getPluginId().getIdString().equalsIgnoreCase(CommonConst.PLUGIN_ID)) {
            registerBicepTextMateBundle();
        }
    }

    @Override
    public void uninstall(@Nonnull IdeaPluginDescriptor ideaPluginDescriptor) {
        if (ideaPluginDescriptor.getPluginId().getIdString().equalsIgnoreCase(CommonConst.PLUGIN_ID)) {
            log.info("-------------------------------------------------------");
            log.info("stopping all language servers at uninstalling plugin " + ideaPluginDescriptor.getName());
            IntellijLanguageClient.stopAllLanguageServers();
            unregisterBicepTextMateBundle();
        }
    }
}
