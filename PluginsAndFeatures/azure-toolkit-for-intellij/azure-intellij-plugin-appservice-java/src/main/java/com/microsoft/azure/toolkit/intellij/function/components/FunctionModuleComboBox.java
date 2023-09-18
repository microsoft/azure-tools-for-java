/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionModuleComboBox extends AzureComboBox<Module> {
    private final Project project;

    public FunctionModuleComboBox(@Nonnull final Project project) {
        super();
        this.project = project;
    }

    @Nonnull
    @Override
    protected List<? extends Module> loadItems() {
        return Arrays.stream(FunctionUtils.listFunctionModules(project)).collect(Collectors.toList());
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof Module ? ((Module) item).getName() : super.getItemText(item);
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object item) {
        return AllIcons.Nodes.Module;
    }
}
