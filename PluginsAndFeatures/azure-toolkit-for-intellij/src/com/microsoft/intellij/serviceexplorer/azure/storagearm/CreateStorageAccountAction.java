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

package com.microsoft.intellij.serviceexplorer.azure.storagearm;

import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.forms.CreateArmStorageAccountForm;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;

import static com.microsoft.intellij.common.CommonConst.MUST_SELECT_AN_AZURE_SUBSCRIPTION_FIRST;

@Name("Create Storage Account...")
public class CreateStorageAccountAction extends NodeActionListener {

    public static final String ERROR_CREATING_STORAGE_ACCOUNT = "Error creating storage account";
    private StorageModule storageModule;

    public CreateStorageAccountAction(StorageModule storageModule) {
        this.storageModule = storageModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        Project project = (Project) storageModule.getProject();
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
                return;
            }
            if (AzureMvpModel.getInstance().isSubscriptionSelected()) {
                PluginUtil.displayErrorDialog(ERROR_CREATING_STORAGE_ACCOUNT, MUST_SELECT_AN_AZURE_SUBSCRIPTION_FIRST);
                return;
            }
            CreateArmStorageAccountForm createStorageAccountForm = new CreateArmStorageAccountForm((Project) storageModule.getProject());
            createStorageAccountForm.fillFields(null, null);

            createStorageAccountForm.setOnCreate(new Runnable() {
                @Override
                public void run() {
                    storageModule.load(false);
                }
            });
            createStorageAccountForm.show();
        } catch (Exception ex) {
            AzurePlugin.log(ERROR_CREATING_STORAGE_ACCOUNT, ex);
            DefaultLoader.getUIHelper().showException(ERROR_CREATING_STORAGE_ACCOUNT, ex, "Error Creating Storage Account", false, true);
        }
    }
}
