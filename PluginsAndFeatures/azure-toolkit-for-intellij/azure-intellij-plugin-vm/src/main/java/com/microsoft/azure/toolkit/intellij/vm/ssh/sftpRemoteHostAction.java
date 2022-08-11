/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.project.Project;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ui.content.Content;
import com.jetbrains.plugins.webDeployment.config.AccessType;
import com.jetbrains.plugins.webDeployment.config.GroupedServersConfigManager;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import com.jetbrains.plugins.webDeployment.config.WebServerGroupingWrap;
import com.jetbrains.plugins.webDeployment.ui.WebServerToolWindowFactory;
import com.jetbrains.plugins.webDeployment.ui.WebServerToolWindowPanel;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.lang3.reflect.MethodUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;


public class sftpRemoteHostAction {

    @AzureOperation(name = "vm.browse_files_sftp", params = "vm.getName()", type = AzureOperation.Type.ACTION)
    public static void browseRemoteHost(VirtualMachine vm, @Nonnull Project project) {
        final SshConfig curSshConfig = AddSshConfigAction.getOrCreateSshConfig(vm, project);
        final WebServerConfig server = getOrCreateWebServerConfigFromSsh(curSshConfig, project);
        AzureTaskManager.getInstance().runLater(() -> {
            final ToolWindow toolWindow = WebServerToolWindowFactory.getWebServerToolWindow(project);
            toolWindow.show(null);
            toolWindow.activate(() -> selectServerInToolWindow(toolWindow, server.getName(), project));
        });
    }

    private static void selectServerInToolWindow(ToolWindow toolWindow, String serverName, @Nonnull Project project) {
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
    }

    @Nonnull
    private static WebServerConfig getOrCreateWebServerConfigFromSsh(SshConfig ssh, @Nonnull Project project) {
        final GroupedServersConfigManager manager = GroupedServersConfigManager.getInstance(project);
        final List<WebServerConfig> webServerConfigList = manager.getFlattenedServers();
        for (final WebServerConfig webServerConfig : webServerConfigList) {
            if (ssh.equals(webServerConfig.findSshConfig(project))) {
                return webServerConfig;
            }
        }
        // create a new webServer config for current machine
        final WebServerConfig server = createWebServerConfigBySsh(ssh.getName(), ssh);
        manager.addServer(server);
        return server;
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

}
