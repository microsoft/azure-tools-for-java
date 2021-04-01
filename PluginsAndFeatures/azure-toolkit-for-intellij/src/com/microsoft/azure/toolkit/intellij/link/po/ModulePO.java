/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.po;


import com.microsoft.azure.toolkit.intellij.link.base.ResourceType;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class ModulePO extends BaseResourcePO {

    public ModulePO(String moduleName) {
        super(UUID.randomUUID().toString().replace("-", StringUtils.EMPTY), moduleName, ResourceType.IDE_MODULE);
    }
}
