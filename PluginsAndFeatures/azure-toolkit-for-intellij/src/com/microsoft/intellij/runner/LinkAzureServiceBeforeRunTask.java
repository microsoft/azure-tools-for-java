/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.runner;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public class LinkAzureServiceBeforeRunTask extends BeforeRunTask<LinkAzureServiceBeforeRunTask> {

    protected LinkAzureServiceBeforeRunTask(@NotNull Key<LinkAzureServiceBeforeRunTask> providerId, boolean enable) {
        super(providerId);
        setEnabled(enable);
    }
}
