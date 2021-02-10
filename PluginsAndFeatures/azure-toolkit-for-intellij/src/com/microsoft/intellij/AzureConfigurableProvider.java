/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public class AzureConfigurableProvider extends ConfigurableProvider {
    private final Project myProject;

    public AzureConfigurableProvider(Project project) {
        myProject = project;
    }

    @Override
    public boolean canCreateConfigurable() {
        return !AzurePlugin.IS_ANDROID_STUDIO;
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new AzureConfigurable(myProject);
    }
}
