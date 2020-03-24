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

package com.microsoft.intellij.ui.components;

import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * This file is based on code in IntelliJ-Community repository, please refer to link below.
 * https://raw.githubusercontent.com/JetBrains/intellij-community/7f151472db08e8db35487f4a0996643f175dba70/platform/util/src/com/intellij/util/EnvironmentUtil.java
 * TODO: We're supposed to remove this file and replace this class with `com.intellij.util.EnvironmentUtil` when IntelliJ upgrade to 2019.1
 */
public class EnvironmentUtil {
    /**
     * Validates environment variable name in accordance to
     * {@code ProcessEnvironment#validateVariable} ({@code ProcessEnvironment#validateName} on Windows).
     *
     * @see #isValidValue(String)
     * @see <a href="http://pubs.opengroup.org/onlinepubs/000095399/basedefs/xbd_chap08.html">Environment Variables in Unix</a>
     * @see <a href="https://docs.microsoft.com/en-us/windows/desktop/ProcThread/environment-variables">Environment Variables in Windows</a>
     */
    @Contract(value = "null -> false", pure = true)
    public static boolean isValidName(@Nullable String name) {
        return name != null && !name.isEmpty() && name.indexOf('\0') == -1 && name.indexOf('=', SystemInfo.isWindows ? 1 : 0) == -1;
    }

    /**
     * Validates environment variable value in accordance to {@code ProcessEnvironment#validateValue}.
     *
     * @see #isValidName(String)
     */
    @Contract(value = "null -> false", pure = true)
    public static boolean isValidValue(@Nullable String value) {
        return value != null && value.indexOf('\0') == -1;
    }
}
