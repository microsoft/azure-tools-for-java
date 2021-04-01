/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.po;

import com.microsoft.azure.toolkit.intellij.link.base.ResourceType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public abstract class BaseResourcePO {

    private String id;
    private String resourceId;
    private ResourceType type;

    public String getBusinessUniqueKey() {
        return type + "#" + resourceId;
    }
}
