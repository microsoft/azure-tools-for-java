/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;

import java.net.URL;

public interface Task {
    InputComponent getInput();

    void execute(Context context) throws Exception;

    default URL getDocUrl() {
        return null;
    }

    default void execute(Context context, IAzureMessager messager) throws Exception {
        final IAzureMessager currentMessager = AzureMessager.getMessager();
        OperationContext.current().setMessager(messager);
        try {
            execute(context);
        } finally {
            OperationContext.current().setMessager(currentMessager);
        }
    }
}
