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
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventListener;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.azurecommons.util.WAEclipseHelperMethods;
import com.microsoft.intellij.common.CommonConst;
import com.microsoft.intellij.ui.libraries.AILibraryHandler;
import com.microsoft.intellij.ui.libraries.AzureLibrary;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public abstract class AzurePlugin extends AbstractProjectComponent {
    protected static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.AzurePlugin");

    public static final String PLUGIN_VERSION = CommonConst.PLUGIN_VERISON;
    public static final String AZURE_LIBRARIES_VERSION = "1.0.0";
    public static final String JDBC_LIBRARIES_VERSION = "6.1.0.jre8";

    // User-agent header for Azure SDK calls
    public static final String USER_AGENT = "Azure Toolkit for IntelliJ, v%s, machineid:%s";

    public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
    public static boolean IS_ANDROID_STUDIO = "AndroidStudio".equals(PlatformUtils.getPlatformPrefix());

    public static String pluginFolder = PluginUtil.getPluginRootDirectory();

    protected static final EventListenerList DEPLOYMENT_EVENT_LISTENERS = new EventListenerList();
    public static List<DeploymentEventListener> depEveList = new ArrayList<DeploymentEventListener>();

    private final AzureSettings azureSettings;

    public AzurePlugin(Project project) {
        super(project);
        this.azureSettings = AzureSettings.getSafeInstance(project);
    }

    public void projectOpened() {
        initializeAIRegistry();
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

    protected void initializeTelemetry() throws Exception {
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

    public String getComponentName() {
        return "MSOpenTechTools.AzurePlugin";
    }

    /**
     * Copies Azure Toolkit for IntelliJ
     * related files in azure-toolkit-for-intellij plugin folder at startup.
     */
    protected void copyPluginComponents() {
        try {
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
}
