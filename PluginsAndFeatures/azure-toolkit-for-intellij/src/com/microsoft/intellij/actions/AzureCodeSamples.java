package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import org.jdesktop.swingx.JXHyperlink;

import java.net.URI;

/**
 * Created by vlashch on 6/10/16.
 */
public class AzureCodeSamples extends AzureAnAction {
    @Override
    public void onActionPerformed(AnActionEvent anActionEvent) {
        JXHyperlink portalLing = new JXHyperlink();
        portalLing.setURI(URI.create("https://azure.microsoft.com/en-us/documentation/samples/?platform=java"));
        portalLing.doClick();
    }
}
