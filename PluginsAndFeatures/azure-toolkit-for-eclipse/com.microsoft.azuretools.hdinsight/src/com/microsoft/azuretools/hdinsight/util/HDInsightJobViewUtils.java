/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azuretools.hdinsight.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

import com.microsoft.azure.hdinsight.spark.jobs.JobViewHttpServer;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.core.utils.PluginUtil;

public class HDInsightJobViewUtils {
    private static final int BUFFER_SIZE = 4096;
    private static final String HTML_ZIP_FILE_NAME = "hdinsight_jobview_html.zip";
    private static final String HDINSIGHT_JOB_VIEW_JAR_NAME = "hdinsight-job-view.jar";
    private static final String HDINSIGIHT_FOLDER_NAME = "com.microsoft.azuretools.hdinsight";
    private static final String HDINSIGHT_JOBVIEW_EXTRACT_FLAG = "com.microsoft.azuretools.hdinsight.html.extract";

    public static void closeJobViewHttpServer() {
        JobViewHttpServer.close();
    }

    public static void checkInitlize() {
        extractJobViewResource();
        JobViewHttpServer.initialize();
    }

     private static void extractJobViewResource() {
            URL url = HDInsightJobViewUtils.class.getResource("/resources/" + HTML_ZIP_FILE_NAME);
            URL hdinsightJobViewJarUrl = HDInsightJobViewUtils.class.getResource("/resources/" + HDINSIGHT_JOB_VIEW_JAR_NAME);
            if(url == null || hdinsightJobViewJarUrl == null) {
                DefaultLoader.getUIHelper().showError("Cann't find Spark job view resources", "Job view");
                return;
            }
            File indexRootFile = new File(PluginUtil.pluginFolder, HDINSIGIHT_FOLDER_NAME);
            if(indexRootFile.exists()) {
                FileUtils.deleteQuietly(indexRootFile);
            }
            File htmlRootFile = new File(indexRootFile.getPath(), "html");
            htmlRootFile.mkdirs();
            File htmlToFile = new File(htmlRootFile.getAbsolutePath(), HTML_ZIP_FILE_NAME);
            File hdinsightJobViewToFile = new File(indexRootFile, HDINSIGHT_JOB_VIEW_JAR_NAME);
            try {
                FileUtils.copyURLToFile(url, htmlToFile);
                FileUtils.copyURLToFile(hdinsightJobViewJarUrl, hdinsightJobViewToFile);
                HDInsightJobViewUtils.unzip(htmlToFile.getAbsolutePath(), htmlToFile.getParent());
                DefaultLoader.getIdeHelper().setProperty(HDINSIGHT_JOBVIEW_EXTRACT_FLAG, "true");
            } catch (IOException e) {
                DefaultLoader.getUIHelper().showError("Extract Job View Folder error:" + e.getMessage(), "Job view");
            }
     }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new java.io.FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

     public static void unzip(String zipFilePath, String destDirectory) throws IOException {
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
}
