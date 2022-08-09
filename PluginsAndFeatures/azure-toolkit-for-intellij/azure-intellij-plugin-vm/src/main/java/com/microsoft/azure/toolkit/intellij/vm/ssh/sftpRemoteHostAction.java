package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.project.Project;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.config.unified.SshConfigManager;
import com.jetbrains.plugins.webDeployment.config.AccessType;
import com.jetbrains.plugins.webDeployment.config.GroupedServersConfigManagerImpl;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import com.jetbrains.plugins.webDeployment.ui.WebServerToolWindowFactory;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;


public class sftpRemoteHostAction {
    public static void browseRemoteHost(VirtualMachine vm, @Nonnull Project project) {
        String sshConfigKey = String.format("Azure: %s", vm.name());
        SshConfig curConfig = SshConfigManager.getInstance(project).findConfigByName(sshConfigKey);
        GroupedServersConfigManagerImpl manager = new GroupedServersConfigManagerImpl(project);

        // get webserver config of current machine
        WebServerConfig server = getWebServerConfigBySsh(curConfig, manager, project);
        if (Objects.isNull(server)) {
            // create a new webServer config for current machine
            server = new WebServerConfig(WebServerConfig.getNextId());
            server.initializeNewCreatedServer(false);
            server.setName(vm.getName());
            server.getFileTransferConfig().setAccessType(AccessType.SFTP);
            server.getFileTransferConfig().setPort(AccessType.SFTP.getDefaultPort());
            server.getFileTransferConfig().setSshConfig(SshConfigManager.getInstance(project).findConfigByName(sshConfigKey));
            manager.addServer(server);
        }

        // open remote host plugin tool windows
        AzureTaskManager.getInstance().runLater(() -> {
            ToolWindow toolWindow = WebServerToolWindowFactory.getWebServerToolWindow(project);
            toolWindow.show((Runnable)null);
            toolWindow.activate((Runnable)null);
        });
    }

    private static WebServerConfig getWebServerConfigBySsh(SshConfig ssh, GroupedServersConfigManagerImpl manager, @Nonnull Project project) {
        List<WebServerConfig> webServerConfigList = manager.getFlattenedServers();
        for (int i=0; i<webServerConfigList.size(); i++) {
            if (webServerConfigList.get(i).findSshConfig(project).equals(ssh)) {
                return webServerConfigList.get(i);
            }
        }
        return null;
    }

}
