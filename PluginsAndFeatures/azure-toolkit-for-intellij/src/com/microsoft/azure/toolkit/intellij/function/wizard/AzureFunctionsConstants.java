/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.wizard;

import com.intellij.openapi.util.Key;

public class AzureFunctionsConstants {
    public static final Key<String> WIZARD_TOOL_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".tool");
    public static final Key<String> WIZARD_GROUPID_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".groupId");
    public static final Key<String> WIZARD_ARTIFACTID_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".artifactId");
    public static final Key<String> WIZARD_VERSION_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".version");
    public static final Key<String> WIZARD_PACKAGE_NAME_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".packageName");
    public static final Key<String[]> WIZARD_TRIGGERS_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".triggers");

}
