package com.microsoft.azure.toolkit.intellij.sjad.terminals;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class UnixTerminal extends AbstractTerminal {

    private final String pluginPath;

    public UnixTerminal(String pluginPath) {
        super();
        this.pluginPath = pluginPath;
    }

    @Override
    public String getAliasAzdCommand() {
        final File azdTool = FileUtils.getFile(pluginPath, "azd", "unix", "azd");
        return String.format("alias azd=\"%s\"", azdTool.getAbsolutePath());
    }

    @Override
    public String getAzdConfigDirCommand() {
        final File azdDir = FileUtils.getFile(pluginPath, "azd");
        return String.format("export AZD_CONFIG_DIR=\"%s\"", azdDir.getAbsolutePath());
    }
}
