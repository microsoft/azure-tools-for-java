/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link;

import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.intellij.link.base.ResourceType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ModuleResourceConfig extends BaseResourceConfig {

    private Module module;

    public static ModuleResourceConfig getDefaultConfig(Module module) {
        ModuleResourceConfig config = new ModuleResourceConfig();
        config.setModule(module);
        return config;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.IDE_MODULE;
    }

}
