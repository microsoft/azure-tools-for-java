/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.azuresdk.model.module;

import com.intellij.openapi.project.Project;

import javax.swing.*;

public interface ProjectModule {

    String getName();

    Project getProject();

    Icon getIcon();

    public static enum Type {
        Maven,
        Gradle
    }
}
