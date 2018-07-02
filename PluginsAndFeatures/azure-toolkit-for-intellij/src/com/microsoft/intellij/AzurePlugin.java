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
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.HashSet;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventListener;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.azurecommons.util.*;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.utils.TelemetryUtils;
import com.microsoft.intellij.common.CommonConst;
import com.microsoft.intellij.feedback.GithubIssue;
import com.microsoft.intellij.feedback.NewGithubIssueAction;
import com.microsoft.intellij.feedback.ReportableSurvey;
import com.microsoft.intellij.ui.libraries.AILibraryHandler;
import com.microsoft.intellij.ui.libraries.AzureLibrary;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import rx.Observable;

import javax.swing.event.EventListenerList;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class AzurePlugin extends AbstractProjectComponent {
    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.AzurePlugin");
    public static final String PLUGIN_VERSION = CommonConst.PLUGIN_VERISON;
    public static final String AZURE_LIBRARIES_VERSION = "1.0.0";
    public static final String JDBC_LIBRARIES_VERSION = "6.1.0.jre8";
    public static final int REST_SERVICE_MAX_RETRY_COUNT = 7;

    // User-agent header for Azure SDK calls
    public static final String USER_AGENT = "Azure Toolkit for IntelliJ, v%s, machineid:%s";

    public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
    public static boolean IS_ANDROID_STUDIO = "AndroidStudio".equals(PlatformUtils.getPlatformPrefix());

    public static String pluginFolder = PluginUtil.getPluginRootDirectory();

    private static final EventListenerList DEPLOYMENT_EVENT_LISTENERS = new EventListenerList();
    public static List<DeploymentEventListener> depEveList = new ArrayList<DeploymentEventListener>();

    private String dataFile = PluginHelper.getTemplateFile(message("dataFileName"));

    private final AzureSettings azureSettings;

    private String _hashmac = GetHashMac.GetHashMac();

    public AzurePlugin(Project project) {
        super(project);
        this.azureSettings = AzureSettings.getSafeInstance(project);
        CommonSettings.setUserAgent(String.format(USER_AGENT, PLUGIN_VERSION,
                TelemetryUtils.getMachieId(dataFile, message("prefVal"), message("instID"))));
    }


    public void projectOpened() {
        initializeAIRegistry();
        initializeFeedbackNotification();
    }

    private void initializeFeedbackNotification() {
        Notification feedbackNotification = new Notification(
                "Azure Toolkit plugin",
                "We're listening",
                "Thanks for helping Microsoft improve Azure Toolkit experience!\nYour feedback is important. Please take a minute to fill out our",
                NotificationType.INFORMATION);

        feedbackNotification.addAction(new NewGithubIssueAction(
                        new GithubIssue<>(new ReportableSurvey("User feedback")).withLabel("Feedback"),
                        "user satisfaction survey"));

        Observable.timer(30, TimeUnit.SECONDS)
                .take(1)
                .subscribe(next -> Notifications.Bus.notify(feedbackNotification));
    }

    public void projectClosed() {
    }

    /**
     * Method is called after plugin is already created and configured. Plugin can start to communicate with
     * other plugins only in this method.
     */
    public void initComponent() {
        if (!IS_ANDROID_STUDIO) {
            LOG.info("Starting Azure Plugin");
            try {
                //this code is for copying componentset.xml in plugins folder
                copyPluginComponents();
                initializeTelemetry();
                clearTempDirectory();
                loadWebappsSettings();
            } catch (Exception e) {
            /* This is not a user initiated task
               So user should not get any exception prompt.*/
                LOG.error(AzureBundle.message("expErlStrtUp"), e);
            }
        }
    }

    private void initializeTelemetry() throws Exception {
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

    private void initializeAIRegistry() {
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
            DataOperations.updatePropertyValue(doc, message("instID"), _hashmac);

            ParserXMLUtility.saveXMLFile(dataFile, doc);
        } catch (Exception ex) {
            LOG.error(message("error"), ex);
        }
    }

    /**
     * Delete %proj% directory from temporary folder during IntelliJ start
     * To fix #2943 : Hang invoking a new Azure project,
     * PML does not delete .cspack.jar everytime new azure project is created.
     * Hence its necessary to delete %proj% directory when plugin with newer version is installed.
     *
     * @throws Exception
     */
    private void clearTempDirectory() throws Exception {
        String tmpPath = System.getProperty("java.io.tmpdir");
        String projPath = String.format("%s%s%s", tmpPath, File.separator, "%proj%");
        File projFile = new File(projPath);
        if (projFile != null) {
            WAEclipseHelperMethods.deleteDirectory(projFile);
        }
    }

    private void loadWebappsSettings() {
        StartupManager.getInstance(myProject).runWhenProjectIsInitialized(
                new Runnable() {
                    @Override
                    public void run() {
                        Module[] modules = ModuleManager.getInstance(myProject).getModules();
                        Set<String> javaModules = new HashSet<String>();
                        for (Module module : modules) {
                            if (ModuleTypeId.JAVA_MODULE.equals(module.getOptionValue(Module.ELEMENT_TYPE))) {
                                javaModules.add(module.getName());
                            }
                        }
                        Set<String> keys = AzureSettings.getSafeInstance(myProject).getPropertyKeys();
                        for (String key : keys) {
                            if (key.endsWith(".webapps")) {
                                String projName = key.substring(0, key.lastIndexOf("."));
                                if (!javaModules.contains(projName)) {
                                    AzureSettings.getSafeInstance(myProject).unsetProperty(key);
                                }
                            }
                        }
                    }
                });
    }

    private void telemetryAI() {
        ModuleManager.getInstance(myProject).getModules();
    }

    public String getComponentName() {
        return "MSOpenTechTools.AzurePlugin";
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
    private void copyPluginComponents() {
        try {
            extractJobViewResource();
            for (AzureLibrary azureLibrary : AzureLibrary.LIBRARIES) {
                if (azureLibrary.getLocation() != null) {
                    if (!new File(pluginFolder + File.separator + azureLibrary.getLocation()).exists()) {
                        for (String entryName : Utils.getJarEntries(pluginFolder + File.separator + "lib" + File.separator + CommonConst.PLUGIN_NAME + ".jar", azureLibrary.getLocation())) {
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

    /**
     * Method copies specified file from plugin resources
     *
     * @param resourceFile
     * @param destFile
     */
    public static void copyResourceFile(String resourceFile, String destFile) {
        try {
            InputStream is = ((PluginClassLoader) AzurePlugin.class.getClassLoader()).findResource(resourceFile).openStream();
            File outputFile = new File(destFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            FileUtil.writeFile(is, fos);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static void fireDeploymentEvent(DeploymentEventArgs args) {
        Object[] list = DEPLOYMENT_EVENT_LISTENERS.getListenerList();

        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == DeploymentEventListener.class) {
                ((DeploymentEventListener) list[i + 1]).onDeploymentStep(args);
            }
        }
    }

    public static void addDeploymentEventListener(DeploymentEventListener listener) {
        DEPLOYMENT_EVENT_LISTENERS.add(DeploymentEventListener.class, listener);
    }

    public static void removeDeploymentEventListener(DeploymentEventListener listener) {
        DEPLOYMENT_EVENT_LISTENERS.remove(DeploymentEventListener.class, listener);
    }

    // todo: move field somewhere?
    public static void removeUnNecessaryListener() {
        for (int i = 0; i < depEveList.size(); i++) {
            removeDeploymentEventListener(depEveList.get(i));
        }
        depEveList.clear();
    }

    public static void log(String message, Throwable ex) {
        LOG.error(message, ex);
    }

    public static void log(String message) {
        LOG.info(message);
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

        URL url = AzurePlugin.class.getResource(HTML_ZIP_FILE_NAME);
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
        BufferedOutputStream bos = new BufferedOutputStream(new java.io.FileOutputStream(filePath));
        byte[] bytesIn = new byte[1024 * 10];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
