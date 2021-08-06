/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.vm.createarmvm.CreateVMWizard;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

@Name("Create")
public class CreateVMAction extends NodeActionListener {
    private static final String ERROR_CREATING_VIRTUAL_MACHINE = "Error creating virtual machine";
    private VMArmModule vmModule;

    public CreateVMAction(VMArmModule vmModule) {
        this.vmModule = vmModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        Project project = (Project) vmModule.getProject();
        AzureSignInAction.requireSignedIn(project, () -> this.doActionPerformed(e, true, project));
    }

    @Override
    public AzureActionEnum getAction() {
        return AzureActionEnum.CREATE;
    }

    private void doActionPerformed(NodeActionEvent e, boolean isLoggedIn, Project project) {
        try {
            if (!isLoggedIn) {
                return;
            }
            if (!AzureLoginHelper.isAzureSubsAvailableOrReportError(ERROR_CREATING_VIRTUAL_MACHINE)) {
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
