/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.mysql;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = {"password"})
public class PasswordConfig {

    private char[] password;
    // for persistence
    private PasswordSaveType passwordSaveType;

    public static PasswordConfig getDefaultConfig() {
        PasswordConfig config = new PasswordConfig();
        config.setPasswordSaveType(PasswordSaveType.NEVER);
        return config;
    }

}
