/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.project.Project;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.remote.AuthType;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.config.unified.SshConfigManager;
import com.intellij.ssh.ui.unified.SshUiData;
import com.intellij.ui.content.Content;
import com.jetbrains.plugins.webDeployment.config.AccessType;
import com.jetbrains.plugins.webDeployment.config.GroupedServersConfigManager;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import com.jetbrains.plugins.webDeployment.config.WebServerGroupingWrap;
import com.jetbrains.plugins.webDeployment.ui.WebServerToolWindowFactory;
import com.jetbrains.plugins.webDeployment.ui.WebServerToolWindowPanel;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.lang3.reflect.MethodUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class sftpRemoteHostAction {
    public static void browseRemoteHost(VirtualMachine vm, @Nonnull Project project) {
        final String sshConfigName = String.format("Azure: %s", vm.getName());
        final SshConfig curSshConfig = getOrCreateSshConfig(project, vm);
        final GroupedServersConfigManager manager = GroupedServersConfigManager.getInstance(project);

        // get webserver config of current machine
        WebServerConfig server = getWebServerConfigBySsh(curSshConfig, manager, project);
        if (Objects.isNull(server)) {
            // create a new webServer config for current machine
            server = createWebServerConfigBySsh(vm.getName(), curSshConfig);
            manager.addServer(server);
        }

        // open remote host plugin tool windows
        final String serverName = server.getName();
        AzureTaskManager.getInstance().runLater(() -> {
            final ToolWindow toolWindow = WebServerToolWindowFactory.getWebServerToolWindow(project);
            if (!toolWindow.isActive()) {
                toolWindow.show(null);
                toolWindow.activate(() -> {
                    // select current machine's config
                    final Content[] contentList = toolWindow.getContentManager().getContents();
                    for (final Content content : contentList) {
                        if (content.getComponent() instanceof WebServerToolWindowPanel) {
                            try {
                                final WebServerToolWindowPanel panel = ((WebServerToolWindowPanel) content.getComponent());
                                final Field webServerCombo = WebServerToolWindowPanel.class.getDeclaredField("myServerCombo");
                                webServerCombo.setAccessible(true);
                                final Pair<WebServerGroupingWrap, WebServerConfig> finalServer = GroupedServersConfigManager.getInstance(project).findByName(serverName);
                                if (!Objects.isNull(finalServer)) {
                                    MethodUtils.invokeMethod(webServerCombo.get(panel), true, "selectServer", finalServer.getSecond());
                                    MethodUtils.invokeMethod(webServerCombo.get(panel), true, "fireChanged");
                                }
                            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                                           NoSuchFieldException e) {
                                AzureMessager.getMessager().error(e);
                            }
                            break;
                        }
                    }
                });
            }
        });
    }

    @Nullable
    private static WebServerConfig getWebServerConfigBySsh(SshConfig ssh, GroupedServersConfigManager manager, @Nonnull Project project) {
        final List<WebServerConfig> webServerConfigList = manager.getFlattenedServers();
        for (final WebServerConfig webServerConfig : webServerConfigList) {
            if (ssh.equals(webServerConfig.findSshConfig(project))) {
                return webServerConfig;
            }
        }
        return null;
    }

    private static WebServerConfig createWebServerConfigBySsh(String serverName, SshConfig ssh) {
        final WebServerConfig serverConfig = new WebServerConfig(WebServerConfig.getNextId());
        serverConfig.initializeNewCreatedServer(false);
        serverConfig.setName(serverName);
        serverConfig.getFileTransferConfig().setAccessType(AccessType.SFTP);
        serverConfig.getFileTransferConfig().setPort(AccessType.SFTP.getDefaultPort());
        serverConfig.getFileTransferConfig().setSshConfig(ssh);
        return serverConfig;
    }

    @Nonnull
    private static SshConfig getOrCreateSshConfig(@Nonnull Project project, VirtualMachine vm) {
        final String sshConfigName = String.format("Azure: %s", vm.getName());
        final SshConfigManager manager = SshConfigManager.getInstance(project);
        SshConfig result = manager.findConfigByName(sshConfigName);
        if (Objects.isNull(result)) {
            result = toSshConfig(vm, sshConfigName);
            final SshUiData uiData = new SshUiData(result, true);
            final SshConfigManager.ConfigsData newConfigs = new SshConfigManager.ConfigsData(Collections.emptyList(), Collections.singletonList(uiData));
            final SshConfigManager.ConfigsData merged = manager.getLastSavedAndCurrentData().createMerged(newConfigs);
            manager.applyData(merged.getCurrentData(), new SshConfigManager.Listener() {
                @Override
                public void sshConfigsChanged() {
                    SshConfigManager.Listener.super.sshConfigsChanged();
                }
            });
        }
        return result;
    }

    @Nonnull
    private static SshConfig toSshConfig(VirtualMachine vm, String name) {
        final SshConfig config = new SshConfig(true);
        config.setCustomName(name);
        config.setId(UUID.nameUUIDFromBytes(name.getBytes()).toString());
        config.setUsername(vm.getAdminUserName());
        config.setAuthType(AuthType.KEY_PAIR);
        config.setHost(vm.getHostIp());
        config.setKeyPath(Paths.get(System.getProperty("user.home"), ".ssh", "id_rsa").toString());
        config.setPort(22);
        return config;
    }

}
