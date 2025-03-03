package com.microsoft.azure.toolkit.intellij.azd.utils;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.plugins.terminal.TerminalOptionsProvider;

public class TerminalInfo {

    public static boolean isWindows() {
        final String shellPath = TerminalOptionsProvider.getInstance().getShellPath();
        return isCmd() || isPowershell();
    }

    public static boolean isCmd() {
        final String shellPath = TerminalOptionsProvider.getInstance().getShellPath();
        if (StringUtil.isEmpty(shellPath)) {
            return false;
        }
        return shellPath.contains("cmd");
    }

    public static boolean isPowershell() {
        final String shellPath = TerminalOptionsProvider.getInstance().getShellPath();
        if (StringUtil.isEmpty(shellPath)) {
            return false;
        }
        return shellPath.contains("pwsh") || shellPath.contains("powershell");
    }
}
