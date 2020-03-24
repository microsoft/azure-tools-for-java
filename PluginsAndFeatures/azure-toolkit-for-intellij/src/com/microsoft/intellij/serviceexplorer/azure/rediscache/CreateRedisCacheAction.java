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

package com.microsoft.intellij.serviceexplorer.azure.rediscache;

import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.forms.CreateRedisCacheForm;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;

@Name("Create Redis Cache")
public class CreateRedisCacheAction extends NodeActionListener {
    private RedisCacheModule redisCacheModule;

    public CreateRedisCacheAction(RedisCacheModule redisModule) {
        this.redisCacheModule = redisModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        Project project = (Project) redisCacheModule.getProject();
        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) return;
            CreateRedisCacheForm createRedisCacheForm = new CreateRedisCacheForm(project);
            createRedisCacheForm.setOnCreate(new Runnable() {
                @Override
                public void run() {
                    if (redisCacheModule != null) {
                        redisCacheModule.load(false);
                    }
                }
            });
            createRedisCacheForm.show();
        } catch (Exception ex) {
            AzurePlugin.log("Error creating Redis cache", ex);
            DefaultLoader.getUIHelper().showException("Error creating Redis Cache", ex, "Error creating Redis Cache", false, true);
        }
    }
}
