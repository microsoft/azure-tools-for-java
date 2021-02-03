/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.messages;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class AzureBundle extends AbstractBundle {

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return ourInstance.getMessage(key, params);
    }

    @NonNls
    private static final String BUNDLE = "com.microsoft.intellij.ui.messages.messages";
    private static final AzureBundle ourInstance = new AzureBundle();

    private AzureBundle() {
        super(BUNDLE);
    }
}
