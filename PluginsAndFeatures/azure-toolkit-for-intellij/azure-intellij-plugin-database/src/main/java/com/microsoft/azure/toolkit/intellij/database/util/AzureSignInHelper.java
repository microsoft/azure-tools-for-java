/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.util;

import com.intellij.openapi.project.Project;

public class AzureSignInHelper {
    public static void requireSignedIn(Project project, Runnable runnable) {
        //TODO(andxu): detect login status and run
        runnable.run();
    }
}
