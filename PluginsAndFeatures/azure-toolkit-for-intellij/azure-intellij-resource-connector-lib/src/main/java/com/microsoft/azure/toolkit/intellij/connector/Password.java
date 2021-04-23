/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString(exclude = {"password"})
@Accessors(chain = true, fluent = true)
public class Password {

    private char[] password;
    // for persistence
    private SaveType saveType = SaveType.NEVER;

    public enum SaveType {
        NEVER("Never"),
        UNTIL_RESTART("Until restart"),
        FOREVER("Forever");

        @Getter
        private final String title;

        SaveType(String title) {
            this.title = title;
        }
    }
}
