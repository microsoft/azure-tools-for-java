package com.microsoft.azure.hdinsight.common.explorer;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class WindowsOpenHDIAzureStorageExplorerHandler extends AbstractHDIAzureStorageExplorerHandler{
    public static final String STORAGE_EXPLORER_REGISTRY_PATH = "storageexplorer\\shell\\open\\command";

    @Override
    protected String getStorageExplorerExecutableFromOS() {
        final String storageExplorerPath = Advapi32Util.registryGetStringValue(WinReg.HKEY_CLASSES_ROOT, STORAGE_EXPLORER_REGISTRY_PATH, "");
        if (StringUtils.isEmpty(storageExplorerPath)) {
            return null;
        }
        // Parse from e.g.: "C:\Program Files (x86)\Microsoft Azure Storage Explorer\StorageExplorer.exe" -- "%1"
        final String[] split = storageExplorerPath.split("\"");
        return split.length > 1 ? split[1] : null;
    }

    @Override
    protected void launchStorageExplorer(String explorer, String storageUrl) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(explorer, storageUrl);
        try {
            processBuilder.start();
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
        }
    }
}
