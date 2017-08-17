package com.microsoft.azuretools.azureexplorer.actions;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;

@Name("Remote debugging...")
public class RemoteDebugAction extends NodeActionListener {

    private WebAppNode webAppNode;

    public RemoteDebugAction(WebAppNode webappNode) {
        this.webAppNode = webappNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        try {
            WebApp webApp = webAppNode.getWebApp();
            // TODO
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
    }
}