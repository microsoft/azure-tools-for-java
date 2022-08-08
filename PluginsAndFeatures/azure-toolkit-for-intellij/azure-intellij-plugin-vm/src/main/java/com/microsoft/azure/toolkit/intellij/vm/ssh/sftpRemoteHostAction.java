package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.project.Project;

import com.intellij.openapi.wm.ToolWindow;
import com.jetbrains.plugins.webDeployment.ui.WebServerToolWindowFactory;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;

import javax.annotation.Nonnull;


public class sftpRemoteHostAction {
    public static void browseRemoteHost(VirtualMachine vm, @Nonnull Project project) {
        // open remote host plugin tool windows
        AzureTaskManager.getInstance().runLater(() -> {
            ToolWindow toolWindow = WebServerToolWindowFactory.getWebServerToolWindow(project);
            toolWindow.show((Runnable)null);
            toolWindow.activate((Runnable)null);
        });

        // open deployment configurable window
//        AzureTaskManager.getInstance().runLater(() -> {
//            DeploymentConfigurable configurable = new DeploymentConfigurable(project);
//            ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
//                WebServerConfig server = new WebServerConfig();
//                SshUiData data = server == null ? null : server.getOrCreateSshUiData(project);
//
//                String nameToSelect = server == null ? (project == null ? null : PublishConfig.getInstance(project).getDefaultServerOrGroupName()) : server.getName();
//                System.out.println("wy hello");
//            });
//        });
    }

}
