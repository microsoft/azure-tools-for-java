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

package com.microsoft.azuretools.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import javax.swing.event.EventListenerList;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.google.gson.Gson;
import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs;
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventListener;
import com.microsoft.azuretools.azurecommons.deploy.UploadProgressEventArgs;
import com.microsoft.azuretools.azurecommons.deploy.UploadProgressEventListener;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.core.azureexplorer.helpers.IDEHelperImpl;
import com.microsoft.azuretools.core.azureexplorer.helpers.MvpUIHelperImpl;
import com.microsoft.azuretools.core.mvp.ui.base.AppSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelperFactory;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.core.ui.UIFactory;
import com.microsoft.azuretools.core.ui.views.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.utils.TelemetryUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginComponent;
import com.microsoft.tooling.msservices.components.PluginSettings;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements PluginComponent {
    private static FileHandler logFileHandler = null;

    // The plug-in ID
    public static final String PLUGIN_ID = "com.microsoft.azuretools.core"; //$NON-NLS-1$

    public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;

    // User-agent header for Azure SDK calls
    public static final String USER_AGENT = "Azure Toolkit for Eclipse, v%s, machineid:%s";
    private static final String AZURE_TOOLS_FOLDER = ".AzureToolsForEclipse";
    private static final String AZURE_TOOLS_FOLDER_DEPRECATED = "AzureToolsForEclipse";

    private String pluginInstLoc;
    private String dataFile;

    // The shared instance
    private static Activator plugin;
    // save temporary value of telemetry preference in case of preference page navigation
    public static String prefState = "";

    private PluginSettings settings;
    public static final String CONSOLE_NAME = Messages.consoleName;

    private static final EventListenerList DEPLOYMENT_EVENT_LISTENERS = new EventListenerList();

    private Collection<String> obsoletePackages;

    private static final EventListenerList UPLOAD_PROGRESS_EVENT_LISTENERS = new EventListenerList();
    public static List<DeploymentEventListener> depEveList = new ArrayList<DeploymentEventListener>();

    /**
     * The constructor
     */
    public Activator() {
    }
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        pluginInstLoc = Paths.get(PluginUtil.pluginFolder, File.separator,
                com.microsoft.azuretools.core.utils.Messages.commonPluginID).toString();
        dataFile = Paths.get(pluginInstLoc, File.separator,
                com.microsoft.azuretools.core.utils.Messages.dataFileName).toString();
        DefaultLoader.setPluginComponent(this);
        DefaultLoader.setIdeHelper(new IDEHelperImpl());
        SchedulerProviderFactory.getInstance().init(new AppSchedulerProvider());
        MvpUIHelperFactory.getInstance().init(new MvpUIHelperImpl());
        initAzureToolsCoreLibsSettings();

        // load up the plugin settings
        try {
            loadPluginSettings();
        } catch (IOException e) {
            showException("Azure Core Plugin", "An error occurred while attempting to load settings for the Azure Core plugin.", e);
        }
        findObsoletePackages(context);
        super.start(context);
    }

    private void showException(String title, String msg, Exception ex) {
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    PluginUtil.displayErrorDialogAndLog(null, title, msg, ex);
                }
            });
        } else {
            PluginUtil.displayErrorDialogAndLog(null, title, msg, ex);
        }
    }

    private void initAzureToolsCoreLibsSettings() {
        try {
            CommonSettings.setUserAgent(String.format(USER_AGENT, FrameworkUtil.getBundle(getClass()).getVersion(),
                    TelemetryUtils.getMachieId(dataFile, com.microsoft.azuretools.core.utils.Messages.prefVal,
                            com.microsoft.azuretools.core.utils.Messages.instID)));
            if (CommonSettings.getUiFactory() == null)
                CommonSettings.setUiFactory(new UIFactory());
            final String baseFolder = FileUtil.getDirectoryWithinUserHome(AZURE_TOOLS_FOLDER).toString();
            final String deprecatedFolder = FileUtil.getDirectoryWithinUserHome(AZURE_TOOLS_FOLDER_DEPRECATED).toString();
            CommonSettings.setUpEnvironment(baseFolder, deprecatedFolder);
            initAzureToolsCoreLibsLoggerFileHandler();
        } catch (IOException e) {
            e.printStackTrace();
            log("initAzureToolsCoreLibsSettings@Activator", e);
        }
    }

    private void initAzureToolsCoreLibsLoggerFileHandler() {
        try {
            String loggerFilePath = Paths.get(CommonSettings.getSettingsBaseDir(), Constants.FILE_NAME_CORE_LIB_LOG).toString();
            System.out.println("Logger path:" + loggerFilePath);
            logFileHandler = new FileHandler(loggerFilePath, false);
            java.util.logging.Logger l = java.util.logging.Logger.getLogger("");
            logFileHandler.setFormatter(new SimpleFormatter());
            l.addHandler(logFileHandler);
            // FIXME: use environment variable to set the level
            l.setLevel(Level.INFO);
            l.info("=== Log session started ===");
        } catch (IOException e) {
            e.printStackTrace();
            log("initAzureToolsCoreLibsLoggerFileHandler@Activator", e);
        }
    }

    public Collection<String> getObsoletePackages() {
        return obsoletePackages;
    }

    public boolean isScalaInstallationTipNeeded() {
        boolean isScalaPluginInstalled = PluginUtil.checkPlugInInstallation("org.scala-ide.sdt.core");
        boolean isHDInsightSelected = PluginUtil.checkPlugInInstallation("com.microsoft.azuretools.hdinsight");
        return isHDInsightSelected && !isScalaPluginInstalled;
    }

    private void findObsoletePackages(BundleContext context) {
        /// Fix Issue : https://github.com/Microsoft/azure-tools-for-java/issues/188
        Map<String, String> obsoletePackageMap = new HashMap<String, String>();
        BufferedReader reader = null;
        try {
            Properties prop = new Properties();
            reader = new BufferedReader(new FileReader(getResourceAsFile("/resources/obsolete_packages.properties")));
            prop.load(reader);
            Enumeration<?> e = prop.propertyNames();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                if (!StringUtils.isNullOrWhiteSpace(key)) {
                    obsoletePackageMap.put(key, prop.getProperty(key));
                }
            }
        } catch (IOException ex) {
            log("findObsoletePackages@Activator", ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        obsoletePackages = new HashSet<String>();
        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; ++i) {
            String symbolicName = bundles[i].getSymbolicName().toLowerCase();
            if (obsoletePackageMap.containsKey(symbolicName)) {
                obsoletePackages.add(obsoletePackageMap.get(symbolicName) + "(" + bundles[i].getVersion() + ")");
            }
        }
    }
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    private void loadPluginSettings() throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(getResourceAsFile("/resources/settings.json")));
//            reader = new BufferedReader(
//                    new InputStreamReader(
//                            MSOpenTechToolsApplication.class.getResourceAsStream("/resources/settings.json")));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            Gson gson = new Gson();
            settings = gson.fromJson(sb.toString(), PluginSettings.class);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static File getResourceAsFile(String fileEntry) {
        File file = null;
        try {
            URL url = Activator.getDefault().getBundle().getEntry(fileEntry);
            URL fileURL = FileLocator.toFileURL(url);
            URL resolve = FileLocator.resolve(fileURL);
            file = new File(resolve.getFile());
        } catch (Exception e) {
            Activator.getDefault().log(e.getMessage(), e);
        }
        return file;
    }

    /**
     * Logs a message and exception.
     *
     * @param message
     * @param excp : exception.
     */
    public void log(String message, Throwable excp) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, excp));
    }

    public void log(String message) {
        getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }

    public static String getPrefState() {
        return prefState;
    }

    public static void setPrefState(String prefState) {
        Activator.prefState = prefState;
    }

    @Override
    public PluginSettings getSettings() {
        return settings;
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    public static void removeUnNecessaryListener() {
        for (int i = 0 ; i < depEveList.size(); i++) {
            removeDeploymentEventListener(depEveList.get(i));
        }
        depEveList.clear();
    }

    public void addDeploymentEventListener(DeploymentEventListener listener) {
        DEPLOYMENT_EVENT_LISTENERS.add(DeploymentEventListener.class, listener);
    }

    public static void removeDeploymentEventListener(DeploymentEventListener listener) {
        DEPLOYMENT_EVENT_LISTENERS.remove(DeploymentEventListener.class, listener);
    }

    public void addUploadProgressEventListener(UploadProgressEventListener listener) {
        UPLOAD_PROGRESS_EVENT_LISTENERS.add(UploadProgressEventListener.class, listener);
    }

    public void removeUploadProgressEventListener(UploadProgressEventListener listener) {
        UPLOAD_PROGRESS_EVENT_LISTENERS.remove(UploadProgressEventListener.class, listener);
    }

    public void fireDeploymentEvent(DeploymentEventArgs args) {
        Object[] list = DEPLOYMENT_EVENT_LISTENERS.getListenerList();

        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == DeploymentEventListener.class) {
                ((DeploymentEventListener) list[i + 1]).onDeploymentStep(args);
            }
        }
    }

    public void fireUploadProgressEvent(UploadProgressEventArgs args) {
        Object[] list = UPLOAD_PROGRESS_EVENT_LISTENERS.getListenerList();

        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == UploadProgressEventListener.class) {
                ((UploadProgressEventListener) list[i + 1]).onUploadProgress(args);
            }
        }
    }

    public static MessageConsole findConsole(String name) {
        ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = consolePlugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (name.equals(existing[i].getName()))
                return (MessageConsole) existing[i];
        }
        // no console found, so create a new one
        MessageConsole messageConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { messageConsole });
        return messageConsole;
    }
}
