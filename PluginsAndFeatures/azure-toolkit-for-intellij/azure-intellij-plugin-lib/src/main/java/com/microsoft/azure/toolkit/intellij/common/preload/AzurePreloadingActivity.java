/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.preload;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.azure.toolkit.lib.common.cache.Preloader;
import lombok.extern.java.Log;

@Log
public class AzurePreloadingActivity extends PreloadingActivity {

    @Override
    public void preload(@org.jetbrains.annotations.NotNull final ProgressIndicator indicator) {
        // Using progress manager as azure task manager is not initialized
        ApplicationManager.getApplication().executeOnPooledThread(Preloader::load);
    }
}
