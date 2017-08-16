package com.microsoft.intellij.serviceexplorer.azure.webapps;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.LinuxWebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WinWebAppNode;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

@Name("Open in Browser")
public class OpenWebAppAction extends NodeActionListener {
    private final WebAppNode webAppNode;

    public OpenWebAppAction(WinWebAppNode webappNode) {
        this.webAppNode = webappNode;
    }

    public OpenWebAppAction(LinuxWebAppNode webappNode) {
        this.webAppNode = webappNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        WebApp webApp = webAppNode.getWebApp();
        try {
            String appServiceLink = "https://" + webApp.defaultHostName();
            Desktop.getDesktop().browse(URI.create(appServiceLink));
        } catch (IOException e1) {
            PluginUtil.displayErrorDialogAndLog(AzureBundle.message("error"), AzureBundle.message("error"), e1);
        }
    }
}