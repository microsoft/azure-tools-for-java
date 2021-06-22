/*
 * Copyright (c) 2020 JetBrains s.r.o.
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

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.NotNull;

/**
 * Displays UI to display the code templates for the registered Azure AD applications.
 * <p>
 * ComponentNotRegistered is suppressed, because IntelliJ isn't finding the reference in resources/META-INF.
 */
@SuppressWarnings("ComponentNotRegistered")
public class ShowApplicationTemplatesAction extends AnAction {
    @Override
    @AzureOperation(name = "connector|aad.show_application_templates", type = AzureOperation.Type.ACTION)
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        var subscriptions = Azure.az(AzureAccount.class).getSubscriptions();
        if (subscriptions.size() == 1) {
            // One subscription? No popup needed.
            AzureTaskManager.getInstance().runLater(() -> showDialog(project, subscriptions.get(0)));
        } else {
            // Multiple subscriptions? Popup.
            AzureTaskManager.getInstance().runLater(new ChooseSubscriptionsTask(project, subscriptions, selected -> {
                AzureTaskManager.getInstance().runLater(() -> showDialog(project, selected));
            }));
        }
    }

    private void showDialog(@NotNull Project project, @NotNull Subscription subscription) {
        var client = AzureUtils.createGraphClient(subscription);
        client.applications().buildRequest().getAsync().thenAccept(page -> {
            AzureTaskManager.getInstance().runLater(() -> {
                if (page.getCurrentPage().isEmpty()) {
                    AzureMessager.getMessager().warning(
                            MessageBundle.message("templateDialog.noApplicationsWarnings.title"),
                            MessageBundle.message("templateDialog.noApplicationsWarnings.text"));
                } else {
                    var dialog = new AzureApplicationTemplateDialog(project, client, subscription, null);
                    dialog.show();
                }
            });
        });
    }
}
