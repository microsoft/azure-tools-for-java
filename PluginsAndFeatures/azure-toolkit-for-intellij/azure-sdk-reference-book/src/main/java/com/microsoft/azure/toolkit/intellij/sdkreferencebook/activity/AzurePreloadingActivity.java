/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.sdkreferencebook.activity;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.azure.toolkit.intellij.sdkreferencebook.service.AzureSDKArtifactService;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AzurePreloadingActivity extends PreloadingActivity {
    private static final Logger logger = LoggerFactory.getLogger(AzurePreloadingActivity.class);

    @Override
    public void preload(@org.jetbrains.annotations.NotNull final ProgressIndicator indicator) {
        // Using progress manager as azure task manager is not initialized
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                AzureSDKArtifactService.getInstance().reloadAzureSDKArtifacts();
            } catch (IOException e) {
                logger.warn(ExceptionUtils.getStackTrace(e)); // preload exception should not block plugin
            }
        });
    }
}
