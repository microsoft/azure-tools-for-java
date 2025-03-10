package com.microsoft.azure.toolkit.intellij.sjad.terminals;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class BashTerminal extends AbstractTerminal {

    private final String pluginPath;

    public BashTerminal(String pluginPath) {
        super();
        this.pluginPath = pluginPath;
    }

    @Override
    public String getAliasAzdCommand() {
        // Note that the Git Bash on Windows still uses .exe binary
        final File azdTool = FileUtils.getFile(pluginPath, "azd", "windows", "azd.exe");
        return String.format("alias azd=\"%s\"", convertToBashPath(azdTool.getAbsolutePath()));
    }

    @Override
    public String getAzdConfigDirCommand() {
        final File azdDir = FileUtils.getFile(pluginPath, "azd");
        return String.format("export AZD_CONFIG_DIR=\"%s\"", convertToBashPath(azdDir.getAbsolutePath()));
    }

    private String convertToBashPath(String winPath) {
        final String path = winPath.replace('\\', '/');
        // If path starts with a drive letter (like "C:"), convert it to "/c"
        if (path.length() > 1 && path.charAt(1) == ':') {
            return "/" + Character.toLowerCase(path.charAt(0)) + path.substring(2);
        }
        return path;
    }
}
