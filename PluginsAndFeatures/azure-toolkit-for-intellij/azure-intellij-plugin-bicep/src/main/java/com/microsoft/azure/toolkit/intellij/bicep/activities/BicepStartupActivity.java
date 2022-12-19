/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.bicep.activities;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.intellij.bicep.highlight.ZipResourceUtils;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class BicepStartupActivity implements StartupActivity.DumbAware {
    public static final String TEXTMATE_ZIP = "/textmate.zip";
    protected static final Logger LOG = Logger.getInstance(BicepStartupActivity.class);

    @Override
    @ExceptionNotification
    @AzureOperation(name = "platform/bicep.prepare_textmate_bundles")
    public void runActivity(@Nonnull Project project) {
        ZipResourceUtils.copyTextMateBundlesFromJar(TEXTMATE_ZIP, "textmate");
    }
}
