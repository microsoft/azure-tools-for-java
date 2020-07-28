/*
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginInstaller;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.util.GetHashMac;
import com.microsoft.azuretools.azurecommons.util.ParserXMLUtility;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.TelemetryUtils;
import com.microsoft.intellij.common.CommonConst;
import com.microsoft.intellij.helpers.CustomerSurveyHelper;
import com.microsoft.intellij.helpers.WhatsNewManager;
import com.microsoft.intellij.ui.libraries.AILibraryHandler;
import com.microsoft.intellij.ui.libraries.AzureLibrary;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureJavaPlugin extends AzurePlugin {

    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.AzureJavaPlugin");

    private static final String HTML_ZIP_FILE_NAME = "/hdinsight_jobview_html.zip";

    private static PluginStateListener pluginStateListener = null;

    @Override
    public void runActivity(@NotNull final Project project) {
        super.runActivity(project);

        CommonSettings.setUserAgent(
                String.format(USER_AGENT, PLUGIN_VERSION, TelemetryUtils.getMachieId(dataFile, message("prefVal"),
                                                                                     message("instID"))));

        initializeAIRegistry(project);
        // Showing dialog needs to be run in UI thread
        initializeFeedbackNotification(project);
        initializeWhatsNew(project);

        if (!IS_ANDROID_STUDIO) {
            try {
                //this code is for copying componentset.xml in plugins folder
                copyPluginComponents();
                initializeTelemetry();
            } catch (Exception e) {
                LOG.error(AzureBundle.message("expErlStrtUp"), e);
            }
        }

    }

    private void initializeWhatsNew(Project project) {
        try {
            WhatsNewManager.INSTANCE.showWhatsNew(false, project);
        } catch (IOException e) {
            // swallow this exception as shown whats new in startup should not block users
            LOG.error(e.getMessage(), e);
        }
    }

    private void initializeFeedbackNotification(Project myProject) {
        CustomerSurveyHelper.INSTANCE.showFeedbackNotification(myProject);
    }

    private synchronized void initializeTelemetry() throws Exception {
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
                    } else if (StringUtils.isEmpty(instID) || !GetHashMac.isValidHashMac(instID)) {
                        upgrade = true;
                        Document doc = ParserXMLUtility.parseXMLFile(dataFile);

                        DataOperations.updatePropertyValue(doc, message("instID"), installationID);
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
            EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_INSTALL, null, null);
        }
        if (upgrade) {
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Upgrade, null, true);
            EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_UPGRADE, null, null);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Load, null, true);
        EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_LOAD, null, null);

        if (pluginStateListener == null) {
            pluginStateListener = new PluginStateListener() {
                @Override
                public void install(@NotNull IdeaPluginDescriptor ideaPluginDescriptor) {
                }

                @Override
                public void uninstall(@NotNull IdeaPluginDescriptor ideaPluginDescriptor) {
                    String pluginId = ideaPluginDescriptor.getPluginId().toString();
                    if (pluginId.equalsIgnoreCase(CommonConst.PLUGIN_ID)) {
                        EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_UNINSTALL, null, null);
                    }
                }
            };
            PluginInstaller.addStateListener(pluginStateListener);
        }
    }

    private void initializeAIRegistry(Project myProject) {
        try {
            AzureSettings.getSafeInstance(myProject).loadAppInsights();
            Module[] modules = ModuleManager.getInstance(myProject).getModules();
            for (Module module : modules) {
                if (module != null && module.isLoaded() && ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))) {
                    String aiXMLPath = String.format("%s%s%s", PluginUtil.getModulePath(module), File.separator, message("aiXMLPath"));
                    if (new File(aiXMLPath).exists()) {
                        AILibraryHandler handler = new AILibraryHandler();
                        handler.parseAIConfXmlPath(aiXMLPath);
                        String key = handler.getAIInstrumentationKey();
                        if (key != null && !key.isEmpty()) {
                            String unknown = message("unknown");
                            List<ApplicationInsightsResource> list =
                                    ApplicationInsightsResourceRegistry.getAppInsightsResrcList();
                            ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(
                                    key, key, unknown, unknown, unknown, unknown, false);
                            if (!list.contains(resourceToAdd)) {
                                ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resourceToAdd);
                            }
                        }
                    }
                }
            }
            AzureSettings.getSafeInstance(myProject).saveAppInsights();
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
        }
    }

    private void setValues(final String dataFile) throws Exception {
        try {
            final Document doc = ParserXMLUtility.parseXMLFile(dataFile);
            String recordedVersion = DataOperations.getProperty(dataFile, message("pluginVersion"));
            if (Utils.whetherUpdateTelemetryPref(recordedVersion)) {
                DataOperations.updatePropertyValue(doc, message("prefVal"), String.valueOf("true"));
            }

            DataOperations.updatePropertyValue(doc, message("pluginVersion"), PLUGIN_VERSION);
            DataOperations.updatePropertyValue(doc, message("instID"), installationID);

            ParserXMLUtility.saveXMLFile(dataFile, doc);
        } catch (Exception ex) {
            LOG.error(message("error"), ex);
        }
    }

    /**
     * Copies Azure Toolkit for IntelliJ
     * related files in azure-toolkit-for-intellij plugin folder at startup.
     */
    private void copyPluginComponents() {
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

        try {
            for (AzureLibrary azureLibrary : AzureLibrary.LIBRARIES) {
                if (azureLibrary.getLocation() != null) {
                    if (!new File(pluginFolder + File.separator + azureLibrary.getLocation()).exists()) {
                        for (String entryName : Utils.getJarEntries(pluginFolder + File.separator + "lib" + File.separator +
                                                                            CommonConst.PLUGIN_NAME + ".jar", azureLibrary.getLocation())) {
                            new File(pluginFolder + File.separator + entryName).getParentFile().mkdirs();
                            copyResourceFile(entryName, pluginFolder + File.separator + entryName);
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    static class ExtractHdiJobViewException extends IOException {
        ExtractHdiJobViewException(String message, Throwable cause) {
            super(message, cause);
        }
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
        BufferedOutputStream bos = new BufferedOutputStream(new java.io.FileOutputStream(filePath));
        byte[] bytesIn = new byte[1024 * 10];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
