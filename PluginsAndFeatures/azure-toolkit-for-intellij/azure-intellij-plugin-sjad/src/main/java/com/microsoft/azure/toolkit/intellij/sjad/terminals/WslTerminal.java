package com.microsoft.azure.toolkit.intellij.sjad.terminals;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class WslTerminal extends AbstractTerminal {

    private final String pluginPath;

    public WslTerminal(String pluginPath) {
        super();
        this.pluginPath = pluginPath;
    }

    @Override
    public String getAliasAzdCommand() {
        final File azdTool = FileUtils.getFile(pluginPath, "azd", "unix", "azd");
        return String.format("alias azd=\"%s\"", convertToWslPath(azdTool.getAbsolutePath()));
    }

    @Override
    public String getAzdConfigDirCommand() {
        final File azdDir = FileUtils.getFile(pluginPath, "azd");
        return String.format("export AZD_CONFIG_DIR=\"%s\"", convertToWslPath(azdDir.getAbsolutePath()));
    }

    private String convertToWslPath(String winPath) {
        final String path = winPath.replace('\\', '/');
        // If the path starts with a drive letter (e.g. "C:"), convert it.
        if (path.length() > 1 && path.charAt(1) == ':') {
            return "/mnt/" + Character.toLowerCase(path.charAt(0)) + path.substring(2);
        }
        return path;
    }
}
