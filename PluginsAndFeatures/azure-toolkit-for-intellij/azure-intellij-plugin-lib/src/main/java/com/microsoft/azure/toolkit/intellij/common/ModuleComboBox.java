/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class ModuleComboBox extends AzureComboBox<Module> {
    
    private final Project project;

    public ModuleComboBox(Project project) {
        super(true);
        this.project = project;
    }

    @Override
    protected List<? extends Module> loadItems() throws Exception {
        return Arrays.asList(ModuleManager.getInstance(project).getModules());
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Module) {
            return ((Module) item).getName();
        } else {
            return StringUtils.EMPTY;
        }
    }

}
