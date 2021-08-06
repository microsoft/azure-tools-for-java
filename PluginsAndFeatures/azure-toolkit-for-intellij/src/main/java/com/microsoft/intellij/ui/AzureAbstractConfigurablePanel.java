/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

public interface AzureAbstractConfigurablePanel extends AzureAbstractPanel {
    boolean isModified();

    void reset();

    void init();
}
