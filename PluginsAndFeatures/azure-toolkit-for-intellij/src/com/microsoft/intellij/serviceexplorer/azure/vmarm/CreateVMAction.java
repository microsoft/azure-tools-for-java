/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.serviceexplorer.azure.vmarm;

import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.wizards.createarmvm.CreateVMWizard;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

import static com.microsoft.intellij.common.CommonConst.MUST_SELECT_AN_AZURE_SUBSCRIPTION_FIRST;

@Name("Create VM")
public class CreateVMAction extends NodeActionListener {
    private static final String ERROR_CREATING_VIRTUAL_MACHINE = "Error creating virtual machine";
    private VMArmModule vmModule;

    public CreateVMAction(VMArmModule vmModule) {
        this.vmModule = vmModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        Project project = (Project) vmModule.getProject();
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
                return;
            }
            if (AzureMvpModel.getInstance().isSubscriptionSelected()) {
                PluginUtil.displayErrorDialog(ERROR_CREATING_VIRTUAL_MACHINE, MUST_SELECT_AN_AZURE_SUBSCRIPTION_FIRST);
                return;
            }
            CreateVMWizard createVMWizard = new CreateVMWizard((VMArmModule) e.getAction().getNode());
            createVMWizard.show();
        } catch (Exception ex) {
            AzurePlugin.log(ERROR_CREATING_VIRTUAL_MACHINE, ex);
            DefaultLoader.getUIHelper().showException(ERROR_CREATING_VIRTUAL_MACHINE, ex, "Error Creating Virtual Machine", false, true);
        }
    }
}
