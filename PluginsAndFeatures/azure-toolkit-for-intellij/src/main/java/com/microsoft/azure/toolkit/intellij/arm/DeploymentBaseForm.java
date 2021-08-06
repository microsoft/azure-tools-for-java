/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

public abstract class DeploymentBaseForm extends AzureDialogWrapper {

    public static final String ARM_DOC = "https://azure.microsoft.com/en-us/resources/templates/";

    protected DeploymentBaseForm(@Nullable Project project, boolean canBeParent) {
        super(project, canBeParent);
    }
}
