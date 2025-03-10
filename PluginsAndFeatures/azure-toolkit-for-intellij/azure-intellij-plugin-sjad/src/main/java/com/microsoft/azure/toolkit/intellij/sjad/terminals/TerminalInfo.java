package com.microsoft.azure.toolkit.intellij.sjad.terminals;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.plugins.terminal.TerminalOptionsProvider;

public class TerminalInfo {

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

    public static boolean isBash() {
        final String shellPath = TerminalOptionsProvider.getInstance().getShellPath();
        if (StringUtil.isEmpty(shellPath)) {
            return false;
        }
        return shellPath.contains("bash") || shellPath.contains("zsh");
    }

    public static boolean isWsl() {
        final String shellPath = TerminalOptionsProvider.getInstance().getShellPath();
        if (StringUtil.isEmpty(shellPath)) {
            return false;
        }
        return shellPath.contains("wsl");
    }
}
