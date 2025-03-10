package com.microsoft.azure.toolkit.intellij.sjad.utils;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.terminal.ui.TerminalWidget;
import com.microsoft.azure.toolkit.intellij.common.CommonConst;
import com.microsoft.azure.toolkit.intellij.sjad.terminals.AbstractTerminal;
import com.microsoft.azure.toolkit.intellij.sjad.terminals.BashTerminal;
import com.microsoft.azure.toolkit.intellij.sjad.terminals.CmdTerminal;
import com.microsoft.azure.toolkit.intellij.sjad.terminals.PowershellTerminal;
import com.microsoft.azure.toolkit.intellij.sjad.terminals.TerminalInfo;
import com.microsoft.azure.toolkit.intellij.sjad.terminals.WslTerminal;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AzdCliUtils {

    private static final Logger logger = Logger.getInstance(AzdCliUtils.class);

    public static void aliasAzdCommand(@NotNull TerminalWidget terminalWidget) {
        logger.info("Create an alias for bundled azd command");
        final String pluginPath = Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.findId(CommonConst.PLUGIN_ID)))
                .getPluginPath().getParent().toString();
        final AbstractTerminal terminal = getSupportedTerminal(pluginPath);
        final String aliasAzdCommand = Objects.requireNonNull(terminal).getAliasAzdCommand();
        terminalWidget.sendCommandToExecute(aliasAzdCommand);
    }

    public static void setAzdConfigDir(@NotNull TerminalWidget terminalWidget) {
        logger.info("Set AZD_CONFIG_DIR env var for alpha.compose");
        final String pluginPath = Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.findId(CommonConst.PLUGIN_ID)))
                .getPluginPath().getParent().toString();
        final AbstractTerminal terminal = getSupportedTerminal(pluginPath);
        final String azdConfigDirCommand = Objects.requireNonNull(terminal).getAzdConfigDirCommand();
        terminalWidget.sendCommandToExecute(azdConfigDirCommand);
    }

    private static AbstractTerminal getSupportedTerminal(String pluginPath) {
        if (SystemInfo.isWindows) {
            if (TerminalInfo.isCmd()) {
                return new CmdTerminal(pluginPath);
            } else if (TerminalInfo.isWsl()) {
                // Wsl
                return new WslTerminal(pluginPath);
            } else if (TerminalInfo.isBash()) {
                // Bash like ones in Windows
                return new BashTerminal(pluginPath);
            } else {
                // Powershell by default
                return new PowershellTerminal(pluginPath);
            }
        } else if (SystemInfo.isLinux || SystemInfo.isMac) {
            return new BashTerminal(pluginPath);
        } else {
            final String osName = System.getProperty("os.name");
            logger.error("Unsupported OS platform: " + osName);
            throw new UnsupportedOperationException("Unsupported OS platform: " + osName);
        }
    }
}
