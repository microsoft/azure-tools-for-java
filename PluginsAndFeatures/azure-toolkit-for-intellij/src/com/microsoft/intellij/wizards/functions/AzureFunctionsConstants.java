/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.wizards.functions;

import com.intellij.openapi.util.Key;

public class AzureFunctionsConstants {
    public static final Key<String> WIZARD_TOOL_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".tool");
    public static final Key<String> WIZARD_GROUPID_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".groupId");
    public static final Key<String> WIZARD_ARTIFACTID_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".artifactId");
    public static final Key<String> WIZARD_VERSION_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".version");
    public static final Key<String> WIZARD_PACKAGE_NAME_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".packageName");
    public static final Key<String[]> WIZARD_TRIGGERS_KEY = Key.create(AzureFunctionsConstants.class.getPackage().getName() + ".triggers");

}
