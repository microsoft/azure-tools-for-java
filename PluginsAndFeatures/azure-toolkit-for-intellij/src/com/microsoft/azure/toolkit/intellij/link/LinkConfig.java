/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class LinkConfig<R extends BaseResourceConfig, T extends BaseResourceConfig> {

    private R resource;
    private T module;
    private String envPrefix;

    public LinkConfig(R resource, T target) {
        this.resource = resource;
        this.module = target;
    }

}
