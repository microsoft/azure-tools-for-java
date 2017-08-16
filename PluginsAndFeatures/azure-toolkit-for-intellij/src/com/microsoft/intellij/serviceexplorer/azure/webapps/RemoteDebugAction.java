/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.serviceexplorer.azure.webapps;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.ijidea.ui.RemoteDebuggingClientDialog;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.LinuxWebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WinWebAppNode;

@Name("Remote Debugging...")
public class RemoteDebugAction extends NodeActionListener {

    private WebAppNode webAppNode;

    public RemoteDebugAction(WinWebAppNode webAppNode) {
        this.webAppNode = webAppNode;
    }

    public RemoteDebugAction(LinuxWebAppNode webAppNode) {
        this.webAppNode = webAppNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        WebApp webApp = webAppNode.getWebApp();
        try {
            // TODO
            RemoteDebuggingClientDialog d = new RemoteDebuggingClientDialog((Project) webAppNode.getProject(), webApp);
            d.show();
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(AzureBundle.message("error"), AzureBundle.message("error"), ex);
        }
    }
}
