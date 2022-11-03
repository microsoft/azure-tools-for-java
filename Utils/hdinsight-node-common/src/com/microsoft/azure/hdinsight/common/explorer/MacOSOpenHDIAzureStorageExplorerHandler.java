package com.microsoft.azure.hdinsight.common.explorer;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

@Slf4j
public class MacOSOpenHDIAzureStorageExplorerHandler extends AbstractHDIAzureStorageExplorerHandler{
    private static final String MAC_OS_STORAGE_EXPLORER_PATH = "/Contents/MacOS/Microsoft\\ Azure\\ Storage\\ Explorer";

    @Override
    protected String getStorageExplorerExecutableFromOS() {
        return MAC_OS_STORAGE_EXPLORER_PATH;
    }

    @Override
    protected boolean launchStorageExplorerWithUri(@Nonnull final IClusterDetail clusterDetail, @Nonnull final String resourceUrl) {
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(URI.create(resourceUrl));
                return true;
            } catch (IOException e) {
                log.info("failed to launch storage explorer from uri", e);
            }
        }
        return false;
    }

    @Override
    protected void launchStorageExplorer(String explorer, String storageUrl) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        final String[] commands = new String[]{"open", "-a", explorer, storageUrl};
        processBuilder.command(commands);
        try {
            processBuilder.start();
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
        }
    }
}
