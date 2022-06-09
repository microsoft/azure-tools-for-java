/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;

import java.net.URL;

public interface Task {
    InputComponent getInput();

    default void execute(Context context) throws Exception{
        execute(context, AzureMessager.getMessager());
    }

    default URL getDocUrl() {
        return null;
    }

    void execute(Context context, IAzureMessager messager) throws Exception;
}
