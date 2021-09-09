/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginInstaller;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.HashSet;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.intellij.azuresdk.dependencesurvey.activity.WorkspaceTaggingActivity;
import com.microsoft.azure.toolkit.intellij.azuresdk.enforcer.AzureSdkEnforcer;
import com.microsoft.azure.toolkit.intellij.common.action.WhatsNewAction;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventListener;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.azurecommons.util.WAEclipseHelperMethods;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.ui.libraries.AILibraryHandler;
import com.microsoft.intellij.ui.libraries.AzureLibrary;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.event.EventListenerList;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_UNINSTALL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SHOW_WHATS_NEW;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;
import static com.microsoft.intellij.AzureConfigInitializer.TELEMETRY;
import static com.microsoft.intellij.AzureConfigInitializer.TELEMETRY_PLUGIN_VERSION;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class AzurePlugin implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.AzurePlugin");
    public static final String PLUGIN_VERSION = CommonConst.PLUGIN_VERSION;
    public static final String AZURE_LIBRARIES_VERSION = "1.0.0";
    public static final String JDBC_LIBRARIES_VERSION = "6.1.0.jre8";
    public static final int REST_SERVICE_MAX_RETRY_COUNT = 7;
    private static PluginStateListener pluginStateListener = null;
    private static final int POP_UP_DELAY = 30;

    // User-agent header for Azure SDK calls
    public static final String USER_AGENT = "Azure Toolkit for IntelliJ, v%s, machineid:%s";

    public static final boolean IS_WINDOWS = SystemInfo.isWindows;

    public static final boolean IS_ANDROID_STUDIO = "AndroidStudio".equals(PlatformUtils.getPlatformPrefix());

    public static String pluginFolder = PluginUtil.getPluginRootDirectory();

    private static final EventListenerList DEPLOYMENT_EVENT_LISTENERS = new EventListenerList();

    public static List<DeploymentEventListener> depEveList = new ArrayList<DeploymentEventListener>();

    private AzureSettings azureSettings;

    private String installationID;

    private Boolean firstInstallationByVersion;

    @Override
    public void runActivity(@NotNull Project project) {
        this.azureSettings = AzureSettings.getSafeInstance(project);
        initializeAIRegistry(project);
        // Showing dialog needs to be run in UI thread
        initializeWhatsNew(project);
        if (!IS_ANDROID_STUDIO) {
            LOG.info("Starting Azure Plugin");
            firstInstallationByVersion = isFirstInstallationByVersion();
            try {
                //this code is for copying componentset.xml in plugins folder
                copyPluginComponents();
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
                clearTempDirectory();
                loadWebappsSettings(project);
                afterInitialization(project);
            } catch (ProcessCanceledException e) {
                throw e;
            } catch (Exception e) {
                /* This is not a user initiated task
                   So user should not get any exception prompt.*/
                LOG.error(AzureBundle.message("expErlStrtUp"), e);
            }
        }
    }

    private void initializeWhatsNew(Project project) {
        EventUtil.executeWithLog(SYSTEM, SHOW_WHATS_NEW,
                operation -> {
                    final AnAction action = ActionManager.getInstance().getAction(WhatsNewAction.ID);
                    final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
                    AzureTaskManager.getInstance().runLater(() -> ActionUtil.invokeAction(action, context, "AzurePluginStartupActivity", null, null));
                },
                error -> {
                    // swallow this exception as shown whats new in startup should not block users
                });
    }

    private void afterInitialization(Project myProject) {
        Observable.timer(POP_UP_DELAY, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .take(1)
                .subscribe(next -> {
                    WorkspaceTaggingActivity.runActivity(myProject);
                    AzureSdkEnforcer.enforce(myProject);
                });
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
            // https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000093184-What-is-com-intellij-openapi-progress-ProcessCanceledException
            // should ignore ProcessCanceledException
            if (Objects.isNull(ExceptionUtil.findCause(ex, ProcessCanceledException.class))) {
                AzurePlugin.log(ex.getMessage(), ex);
            }
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

    private void loadWebappsSettings(Project myProject) {
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

    private void telemetryAI(Project myProject) {
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

    private synchronized boolean isFirstInstallationByVersion() {
        if (firstInstallationByVersion != null) {
            return firstInstallationByVersion;
        }
        String version = AzureStoreManager.getInstance().getAppStore().getProperty(TELEMETRY, TELEMETRY_PLUGIN_VERSION);
        firstInstallationByVersion = StringUtils.equalsIgnoreCase(version, PLUGIN_VERSION);
        // update plugin version;
        AzureStoreManager.getInstance().getAppStore().setProperty(TELEMETRY, TELEMETRY_PLUGIN_VERSION, PLUGIN_VERSION);
        return firstInstallationByVersion;
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
