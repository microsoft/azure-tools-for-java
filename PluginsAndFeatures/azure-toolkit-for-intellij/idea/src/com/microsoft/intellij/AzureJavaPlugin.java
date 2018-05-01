/**
 * Copyright (c) Microsoft Corporation
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

import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.azurecommons.util.GetHashMac;
import com.microsoft.azuretools.azurecommons.util.ParserXMLUtility;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.utils.TelemetryUtils;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class AzureJavaPlugin extends AzurePlugin {

    private String dataFile = PluginHelper.getTemplateFile(message("dataFileName"));
    private String _hashmac = GetHashMac.GetHashMac();

    public AzureJavaPlugin(Project project) {
        super(project);
        CommonSettings.setUserAgent(String.format(USER_AGENT, PLUGIN_VERSION,
                TelemetryUtils.getMachieId(dataFile, message("prefVal"), message("instID"))));
    }

    @Override
    protected void initializeTelemetry() throws Exception {
        boolean install = false;
        boolean upgrade = false;

        if (new File(dataFile).exists()) {
            String version = DataOperations.getProperty(dataFile, message("pluginVersion"));
            if (version == null || version.isEmpty()) {
                upgrade = true;
                // proceed with setValues method as no version specified
                setValues(dataFile);
            } else {
                String curVersion = PLUGIN_VERSION;
                // compare version
                if (curVersion.equalsIgnoreCase(version)) {
                    // Case of normal IntelliJ restart
                    // check preference-value & installation-id exists or not else copy values
                    String prefValue = DataOperations.getProperty(dataFile, message("prefVal"));
                    String instID = DataOperations.getProperty(dataFile, message("instID"));
                    if (prefValue == null || prefValue.isEmpty()) {
                        setValues(dataFile);
                    } else if (instID == null || instID.isEmpty() || !GetHashMac.IsValidHashMacFormat(instID)) {
                        upgrade = true;
                        Document doc = ParserXMLUtility.parseXMLFile(dataFile);
                        DataOperations.updatePropertyValue(doc, message("instID"), _hashmac);
                        ParserXMLUtility.saveXMLFile(dataFile, doc);
                    }
                } else {
                    upgrade = true;
                    // proceed with setValues method. Case of new plugin installation
                    setValues(dataFile);
                }
            }
        } else {
            // copy file and proceed with setValues method
            install = true;
            copyResourceFile(message("dataFileName"), dataFile);
            setValues(dataFile);
        }
        AppInsightsClient.setAppInsightsConfiguration(new AppInsightsConfigurationImpl());
        if (install) {
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Install, null, true);
        }
        if (upgrade) {
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Upgrade, null, true);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Load, null, true);
    }

    private void setValues(final String dataFile) throws Exception {
        try {
            final Document doc = ParserXMLUtility.parseXMLFile(dataFile);
            String recordedVersion = DataOperations.getProperty(dataFile, message("pluginVersion"));
            if (Utils.whetherUpdateTelemetryPref(recordedVersion)) {
                DataOperations.updatePropertyValue(doc, message("prefVal"), String.valueOf("true"));
            }

            DataOperations.updatePropertyValue(doc, message("pluginVersion"), PLUGIN_VERSION);
            DataOperations.updatePropertyValue(doc, message("instID"), _hashmac);

            ParserXMLUtility.saveXMLFile(dataFile, doc);
        } catch (Exception ex) {
            LOG.error(message("error"), ex);
        }
    }

    // currently we didn't have a better way to know if it is in debug model.
    // the code suppose we are under debug model if the plugin root path contains 'sandbox' for Gradle default debug path
    private boolean isDebugModel() {
        return PluginUtil.getPluginRootDirectory().contains("sandbox");
    }

    /**
     * Copies Azure Toolkit for IntelliJ
     * related files in azure-toolkit-for-intellij plugin folder at startup.
     */
    @Override
    protected void copyPluginComponents() {
        try {
            extractJobViewResource();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        super.copyPluginComponents();
    }

    /**
     * Method copies specified file from plugin resources
     *
     * @param resourceFile
     * @param destFile
     */
    public static void copyResourceFile(String resourceFile, String destFile) {
        try {
            InputStream is = ((PluginClassLoader) AzureJavaPlugin.class.getClassLoader()).findResource(resourceFile).openStream();
            File outputFile = new File(destFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            FileUtil.writeFile(is, fos);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
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

    private void extractJobViewResource() {
        File indexRootFile = new File(PluginUtil.getPluginRootDirectory() + File.separator + "com.microsoft.hdinsight");

        if (isFirstInstallationByVersion() || isDebugModel()) {
            if (indexRootFile.exists()) {
                try {
                    FileUtils.deleteDirectory(indexRootFile);
                } catch (IOException e) {
                    LOG.error("delete HDInsight job view folder error", e);
                }
            }
        }

        URL url = AzureJavaPlugin.class.getResource(HTML_ZIP_FILE_NAME);
        if (url != null) {
            File toFile = new File(indexRootFile.getAbsolutePath(), HTML_ZIP_FILE_NAME);
            try {
                FileUtils.copyURLToFile(url, toFile);
                unzip(toFile.getAbsolutePath(), toFile.getParent());
            } catch (IOException e) {
                LOG.error("Extract Job View Folder", e);
            }
        } else {
            LOG.error("Can't find HDInsight job view zip package");
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
