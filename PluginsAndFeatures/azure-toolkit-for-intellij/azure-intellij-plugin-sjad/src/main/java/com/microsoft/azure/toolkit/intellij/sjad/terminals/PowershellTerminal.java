package com.microsoft.azure.toolkit.intellij.sjad.terminals;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class PowershellTerminal extends AbstractTerminal {

    private final String pluginPath;

    public PowershellTerminal(String pluginPath) {
        super();
        this.pluginPath = pluginPath;
    }

    @Override
    public String getAliasAzdCommand() {
        final File azdTool = FileUtils.getFile(pluginPath, "azd", "windows", "azd.exe");
        return String.format("Set-Alias azd \"%s\"", azdTool.getAbsolutePath());
    }

    @Override
    public String getAzdConfigDirCommand() {
        final File azdDir = FileUtils.getFile(pluginPath, "azd");
        return String.format("$env:AZD_CONFIG_DIR = \"%s\"", azdDir.getAbsolutePath());
    }
}
