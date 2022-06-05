/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

public interface Task {
    InputComponent getInputComponent();

    default void executeWithUI(Context context) throws Exception {
        execute(context);
    }

    void execute(Context context) throws Exception;
}
