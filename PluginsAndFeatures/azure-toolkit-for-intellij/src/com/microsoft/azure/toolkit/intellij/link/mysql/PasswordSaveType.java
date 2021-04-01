/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.mysql;

import lombok.Getter;

public enum PasswordSaveType {

    NEVER("Never"),
    UNTIL_RESTART("Until restart"),
    FOEVER("Forever");

    @Getter
    private String name;

    PasswordSaveType(String name) {
        this.name = name;
    }

}
