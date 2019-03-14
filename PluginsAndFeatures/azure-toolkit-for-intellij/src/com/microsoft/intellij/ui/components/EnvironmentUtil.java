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
