/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.redis.CreateRedisCacheForm;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;

@Name("New Redis Cache")
public class CreateRedisCacheAction extends NodeActionListener {
    private static final String ERROR_CREATING_REDIS_CACHE = "Error creating Redis cache";
    private RedisCacheModule redisCacheModule;

    public CreateRedisCacheAction(RedisCacheModule redisModule) {
        this.redisCacheModule = redisModule;
    }

    @Override
    public AzureActionEnum getAction() {
        return AzureActionEnum.CREATE;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        Project project = (Project) redisCacheModule.getProject();
        AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project).subscribe((isSuccess) -> {
            this.doActionPerformed(e, isSuccess, project);
        });
    }

    private void doActionPerformed(NodeActionEvent e, boolean isLoggedIn, Project project) {
        try {
            if (!isLoggedIn) {
                return;
            }
            if (!AzureLoginHelper.isAzureSubsAvailableOrReportError(ERROR_CREATING_REDIS_CACHE)) {
                return;
            }
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
        } catch (Throwable ex) {
            AzurePlugin.log(ERROR_CREATING_REDIS_CACHE, ex);
            throw new RuntimeException("Error creating Redis Cache", ex);
        }
    }

    @Override
    protected @Nullable String getIconPath() {
        return "AddEntity.svg";
    }
}
