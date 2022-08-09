/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.project.Project;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.config.unified.SshConfigManager;
import com.intellij.ui.content.Content;
import com.jetbrains.plugins.webDeployment.config.AccessType;
import com.jetbrains.plugins.webDeployment.config.GroupedServersConfigManager;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import com.jetbrains.plugins.webDeployment.ui.WebServerToolWindowFactory;
import com.jetbrains.plugins.webDeployment.ui.WebServerToolWindowPanel;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.lang3.reflect.MethodUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;


public class sftpRemoteHostAction {
    public static void browseRemoteHost(VirtualMachine vm, @Nonnull Project project) {
        final String sshConfigKey = String.format("Azure: %s", vm.getName());
        final SshConfig curSshConfig = SshConfigManager.getInstance(project).findConfigByName(sshConfigKey);
        final GroupedServersConfigManager manager = GroupedServersConfigManager.getInstance(project);

        // get webserver config of current machine
        WebServerConfig server = getWebServerConfigBySsh(curSshConfig, manager, project);
        if (Objects.isNull(server)) {
            // create a new webServer config for current machine
            server = createWebServerConfigBySsh(vm.getName(), curSshConfig);
            manager.addServer(server);
        }

        // open remote host plugin tool windows
        final WebServerConfig finalServer = server;
        AzureTaskManager.getInstance().runLater(() -> {
            final ToolWindow toolWindow = WebServerToolWindowFactory.getWebServerToolWindow(project);
            // select current machine's config
            final Content[] contentList = toolWindow.getContentManager().getContents();
            for (final Content content : contentList) {
                if (content.getComponent() instanceof WebServerToolWindowPanel) {
                    try {
                        final WebServerToolWindowPanel panel = ((WebServerToolWindowPanel) content.getComponent());
                        panel.selectInServerByName(project, Objects.requireNonNull(finalServer.getName()), finalServer.getRootPath());
                        MethodUtils.invokeMethod(panel, true, "constructTree");
                    } catch (final NoSuchMethodException | IllegalAccessException |
                                   InvocationTargetException e) {
                        AzureMessager.getMessager().error(e);
                    }
                    break;
                }
            }
            if (!toolWindow.isActive()) {
                toolWindow.show(null);
                toolWindow.activate(null);
            }
        });
    }

    @Nullable
    private static WebServerConfig getWebServerConfigBySsh(SshConfig ssh, GroupedServersConfigManager manager, @Nonnull Project project) {
        final List<WebServerConfig> webServerConfigList = manager.getFlattenedServers();
        for (final WebServerConfig webServerConfig : webServerConfigList) {
            if (Objects.requireNonNull(webServerConfig.findSshConfig(project)).equals(ssh)) {
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

}
