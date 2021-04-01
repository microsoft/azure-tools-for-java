/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.base;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
public enum ResourceType {
    IDE_MODULE("Module"),
    AZURE_DATABASE_FOR_MYSQL("Microsoft.DBforMySQL");

    @Getter
    private String name;

    public static ResourceType parseTypeByName(String name) {
        for (ResourceType type : ResourceType.values()) {
            if (StringUtils.equals(type.name, name)) {
                return type;
            }
        }
        return null;
    }

}
