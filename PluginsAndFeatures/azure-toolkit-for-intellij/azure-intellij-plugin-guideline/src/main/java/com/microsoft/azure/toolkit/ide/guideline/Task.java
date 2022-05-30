/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

public interface Task {
    Step create(Process process);

    InputComponent getInputComponent();

    default void executeWithUI(Context context) {
        execute(context);
    }

    void execute(Context context);
}
