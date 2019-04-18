/**
 * Copyright (c) 2018 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class AzureJavaPlugin extends AzurePlugin {

    public AzureJavaPlugin(Project project) {
        super(project);
    }

    /**
     * Copies Azure Toolkit for IntelliJ
     * related files in azure-toolkit-for-intellij plugin folder at startup.
     */
    @Override
    protected void copyPluginComponents() {
        try {
            extractJobViewResource();
        } catch (ExtractHdiJobViewException e) {
            Notification hdiSparkJobListNaNotification = new Notification(
                    "Azure Toolkit plugin",
                    e.getMessage(),
                    "The HDInsight cluster Spark Job list feature is not available since " + e.getCause().toString() +
                            " Reinstall the plugin to fix that.",
                    NotificationType.WARNING);

            Notifications.Bus.notify(hdiSparkJobListNaNotification);
        }
        super.copyPluginComponents();
    }

    static class ExtractHdiJobViewException extends IOException {
        ExtractHdiJobViewException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final String HTML_ZIP_FILE_NAME = "/hdinsight_jobview_html.zip";

    private boolean isFirstInstallationByVersion() {
        if (new File(dataFile).exists()) {
            String version = DataOperations.getProperty(dataFile, message("pluginVersion"));
            if (!StringHelper.isNullOrWhiteSpace(version) && version.equals(PLUGIN_VERSION)) {
                return false;
            }
        }
        return true;
    }

    private synchronized void extractJobViewResource() throws ExtractHdiJobViewException {
        File indexRootFile = new File(PluginUtil.getPluginRootDirectory() + File.separator + "com.microsoft.hdinsight");

        if (isFirstInstallationByVersion() || isDebugModel()) {
            if (indexRootFile.exists()) {
                try {
                    FileUtils.deleteDirectory(indexRootFile);
                } catch (IOException e) {
                    throw new ExtractHdiJobViewException("Delete HDInsight job view folder error", e);
                }
            }
        }

        URL url = AzurePlugin.class.getResource(HTML_ZIP_FILE_NAME);
        if (url != null) {
            File toFile = new File(indexRootFile.getAbsolutePath(), HTML_ZIP_FILE_NAME);
            try {
                FileUtils.copyURLToFile(url, toFile);

                // Need to wait for OS native process finished, otherwise, may get the following exception:
                // message=Extract Job View Folder, throwable=java.io.FileNotFoundException: xxx.zip
                // (The process cannot access the file because it is being used by another process)
                int retryCount = 60;
                while (!toFile.renameTo(toFile) && retryCount-- > 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }

                if (!toFile.renameTo(toFile)) {
                    throw new ExtractHdiJobViewException("Copying Job view zip file are not finished",
                            new IOException("The native file system has not finished the file copy for " +
                                    toFile.getPath() + " in 1 minute"));
                }

                unzip(toFile.getAbsolutePath(), toFile.getParent());
            } catch (IOException e) {
                throw new ExtractHdiJobViewException("Extract Job View Folder error", e);
            }
        } else {
            throw new ExtractHdiJobViewException("Can't find HDInsight job view zip package",
                    new FileNotFoundException("The HDInsight Job view zip file " + HTML_ZIP_FILE_NAME + " is not found"));
        }
    }

    private static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                extractFile(zipIn, filePath);
            } else {
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[1024 * 10];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
