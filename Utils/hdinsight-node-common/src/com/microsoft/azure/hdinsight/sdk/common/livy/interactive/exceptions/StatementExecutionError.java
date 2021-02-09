/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.livy.interactive.exceptions;

import java.util.List;

public class StatementExecutionError extends LivyInteractiveException {
    public StatementExecutionError(String name, String value, List<String> traceback) {
        super(String.format("Statement execution result: [%s] --\n%s\nTraceback:\n%s",
                name, value, String.join("\n", traceback)));
    }
}
