package com.microsoft.azure.toolkit.intellij.sjad.terminals;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class CmdTerminal extends AbstractTerminal {

    private final String pluginPath;

    public CmdTerminal(String pluginPath) {
        super();
        this.pluginPath = pluginPath;
    }

    @Override
    public String getAliasAzdCommand() {
        final File azdTool = FileUtils.getFile(pluginPath, "azd", "windows", "azd.exe");
        return String.format("doskey azd=\"%s\" $*", azdTool.getAbsolutePath());
    }

    @Override
    public String getAzdConfigDirCommand() {
        final File azdDir = FileUtils.getFile(pluginPath, "azd");
        return String.format("set AZD_CONFIG_DIR=%s", azdDir.getAbsolutePath());
    }
}
