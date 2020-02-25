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
 *
 */

package com.microsoft.intellij.util;

public class ValidationUtils {
    private static final String PACKAGE_NAME_REGEX = "[a-zA-Z]([\\.a-zA-Z0-9_])*";
    private static final String GROUP_ARTIFACT_ID_REGEX = "[0-9a-zA-Z]([\\.a-zA-Z0-9\\-_])*";
    private static final String VERSION_REGEX = "[0-9]([\\.a-zA-Z0-9\\-_])*";

    public static boolean isValidJavaPackageName(String packageName) {
        return packageName != null && packageName.matches(PACKAGE_NAME_REGEX);
    }

    public static boolean isValidGroupIdArtifactId(String name) {
        return name != null && name.matches(GROUP_ARTIFACT_ID_REGEX);
    }

    public static boolean isValidVersion(String version) {
        return version != null && version.matches(VERSION_REGEX);
    }
}
