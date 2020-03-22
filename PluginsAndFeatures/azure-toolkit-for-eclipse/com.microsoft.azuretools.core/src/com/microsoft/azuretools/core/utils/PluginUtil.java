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
package com.microsoft.azuretools.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.epp.mpc.ui.MarketplaceClient;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ProductProperties;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.prefs.Preferences;

import com.microsoft.azuretools.core.Activator;


public class PluginUtil {

    public static final String pluginFolder = getPluginFolderPathUsingBundle();

    public static final String scalaPluginMarketplaceURL = "http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=421";
    public static final String scalaPluginSymbolicName = "org.scala-ide.sdt.core";
    // FWLink for http://scala-ide.org/download/current.html
    public static final String scalaPluginManualInstallURL = "https://go.microsoft.com/fwlink/?linkid=861125";

    private static final String marketplacePluginSymbolicName = "org.eclipse.epp.mpc.ui";
    private static final String marketplacePluginID = "org.eclipse.epp.mpc.feature.group";

    //private static final String COMPONENTSETS_TYPE = "COMPONENTSETS";

    /**
     * @return Template(componentssets.xml)
     */
    public static String getTemplateFile(String fileName) {
        String file = String.format("%s%s%s%s%s", PluginUtil.pluginFolder,
                File.separator, Messages.commonPluginID, File.separator, fileName);
        return file;
    }

    /**
     * This method returns currently selected project in workspace.
     * @return IProject
     */
    public static IProject getSelectedProject() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        ISelectionService service = window.getSelectionService();
        ISelection selection = service.getSelection();
        Object element = null;
        IResource resource;
        IProject selProject = null;
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSel =
                    (IStructuredSelection) selection;
            element = structuredSel.getFirstElement();
        }
        if (element instanceof IProject) {
            resource = (IResource) element;
            selProject = resource.getProject();
        } else if (element instanceof IJavaProject) {
            IJavaProject proj = ((IJavaElement) element).getJavaProject();
            selProject = proj.getProject();
        } else if (element instanceof IResource) {
            resource = (IResource) element;
            selProject = resource.getProject();
        } else {
            IWorkbenchPage page = window.getActivePage();
            IEditorPart editorPart = page.getActiveEditor();
            if (editorPart != null) {
                IFile file = (IFile) editorPart.getEditorInput().getAdapter(IFile.class);
                if (file != null) {
                    selProject = file.getProject();
                }
            }
        }
        return selProject;
    }

    /**
     * This method will display the information message box. It takes three parameters
     *
     * @param shell       parent shell
     * @param title       the text or title of the window.
     * @param message     the message which is to be displayed
     */
    public static void displayInfoDialog(Shell shell , String title , String message ){
        MessageDialog.openInformation(shell, title, message);
    }

    public static void displayInfoDialogWithLink(Shell shell, String title, String message, String messageWithLink) {
        MessageDialogWithLink.openInformation(shell, title, message, messageWithLink);
    }

    /**
     * This method will display the error message box when any error occurs. It takes two parameters
     *
     * @param shell       parent shell
     * @param title       the text or title of the window.
     * @param message     the message which is to be displayed
     */
    public static void displayErrorDialog(Shell shell , String title , String message ){
        MessageDialog.openError(shell, title, message);
    }

    public static void displayErrorDialogWithLink(Shell shell, String title, String message, String messageWithLink) {
        MessageDialogWithLink.openError(shell, title, message, messageWithLink);
    }

    public static void displayErrorDialogAndLog(Shell shell, String title, String message, Throwable e) {
        Activator.getDefault().log(message, e);
        displayErrorDialog(shell, title, message);
    }

    public static void displayErrorDialogWithLinkAndLog(Shell shell, String title, String message, String messageWithLink, Throwable e) {
        Activator.getDefault().log(message, e);
        displayErrorDialogWithLink(shell, title, message, messageWithLink);
    }

    public static void displayErrorDialogWithAzureMsg(Shell shell, String title, String message, Exception e) {
        Activator.getDefault().log(message, e);
        message = message + "\n" + String.format(Messages.azExpMsg, e.getMessage());
        displayErrorDialog(shell, title, message);
    }

    /**
     * Gets preferences object according to node name.
     * @return
     */
    @SuppressWarnings("deprecation")
    public static Preferences getPrefs(String qualifier) {
        Preferences prefs = null;
        if (isHelios()) {
            prefs = new InstanceScope().getNode(qualifier);
        } else {
            prefs = InstanceScope.INSTANCE.getNode(qualifier);
        }
        return prefs;
    }

    /**
     * Method checks version of the eclipse.
     * If its helios then returns true.
     * @return
     */
    private static boolean isHelios() {
        Version version = Platform.getBundle(Messages.bundleName).getVersion();
        int majorVersion = version.getMajor();
        if (majorVersion == 3) { // indigo and helios
            int minorVersion = version.getMinor();
            if (minorVersion < 7) { // helios 3.6 and lower versions
                return true;
            }
        }
        return false;
    }

    /**
     * Gets location of Azure Libraries
     * @throws WACommonException
     */
    public static String getAzureLibLocation() throws WACommonException {
        String libLocation = null;

        try {
            //get bundle for the sdk
            Bundle bundle = Platform.getBundle(Messages.sdkLibBundleName);

            if (bundle == null) {
                throw new WACommonException(Messages.SDKLocErrMsg);
            } else {
                //locate sdk jar in bundle
                URL url = FileLocator.find(bundle,new Path(Messages.sdkLibBaseJar), null);
                if (url == null) {
                    throw new WACommonException(Messages.SDKLocErrMsg);
                } else {
                    //if jar is found then resolve url and get the location
                    url = FileLocator.resolve(url);
                    File loc = new File(url.getPath());
                    libLocation = loc.getParentFile().getAbsolutePath();
                }
            }
        } catch (WACommonException e) {
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
            throw new WACommonException(Messages.SDKLocErrMsg);
        }

        return libLocation;
    }

    public static String getPrefFilePath() {
        String prefFilePath = String.format("%s%s%s%s%s",
                pluginFolder,
                File.separator,
                Messages.waCommonFolderID,
                File.separator,
                "preferencesets.xml");
        return prefFilePath;
    }

    public static String getEncPath() {
        String encPath = String.format("%s%s%s",
                pluginFolder, File.separator,
                Messages.waCommonFolderID);
        return encPath;
    }

    public static String getPluginFolderPathUsingBundle() {
        Bundle bundle = Activator.getDefault().getBundle();
        URL url = bundle.getEntry("/");
        String pluginFolderPath = "";
        try {
            @SuppressWarnings("deprecation")
            URL resolvedURL = Platform.resolve (url);
            File file = new File (resolvedURL.getFile());
            String path = file.getParentFile().getAbsolutePath();

            // Default values for Linux
            String fileTxt = "file:";
            int index = 5;
            if (path.contains(fileTxt)) {
                if (Activator.IS_WINDOWS) {
                    fileTxt = fileTxt + File.separator;
                    index = 6;
                }
                pluginFolderPath = path.substring(path.indexOf(fileTxt) + index);
            } else {
                // scenario when we run source code
                pluginFolderPath = String.format("%s%s%s",
                        Platform.getInstallLocation().getURL().getPath().toString(),
                        File.separator, Messages.pluginFolder);
                if (Activator.IS_WINDOWS) {
                    pluginFolderPath = pluginFolderPath.substring(1);
                }
            }
            Activator.getDefault().log("Plugin folder path:" + pluginFolderPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pluginFolderPath;
    }

    /**
     * Refreshes the workspace.
     */
    public static void refreshWorkspace() {
        try {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();
            root.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            //This is just a try to refresh workspace.
            //User can also refresh the workspace manually.
            //So user should not get any exception prompt.
            Activator.getDefault().log(Messages.resCLExWkspRfrsh, null);
        }
    }

    /**
     * Method opens property dialog with only desired property page.
     *
     * @param nodeId
     *            : Node ID of property page
     * @param nodeLbl
     *            : Property page name
     * @param classObj
     *            : Class object of property page
     * @return
     */
    public static int openPropertyPageDialog(String nodeId, String nodeLbl,
            Object classObj) {
        int retVal = Window.CANCEL; // value corresponding to cancel
        // Node creation
        try {
            PreferenceNode nodePropPg = new PreferenceNode(nodeId, nodeLbl,
                    null, classObj.getClass().toString());
            nodePropPg.setPage((IPreferencePage) classObj);
            nodePropPg.getPage().setTitle(nodeLbl);

            PreferenceManager mgr = new PreferenceManager();
            mgr.addToRoot(nodePropPg);
            // Dialog creation
            PreferenceDialog dialog = new PreferenceDialog(PlatformUI
                    .getWorkbench().getDisplay().getActiveShell(), mgr);
            // make desired property page active.
            dialog.setSelectedNode(nodeLbl);
            dialog.create();
            /*
             * If showing storage accounts preference page, don't show
             * properties for title as its common repository.
             */
            String dlgTitle = "";
            if (nodeLbl.equals(Messages.cmhLblStrgAcc)
                    || nodeLbl.equals(Messages.aiTxt)) {
                dlgTitle = nodeLbl;
            } else {
                dlgTitle = String.format(Messages.cmhPropFor,
                        getSelectedProject().getName());
            }
            dialog.getShell().setText(dlgTitle);
            dialog.open();
            // return whether user has pressed OK or Cancel button
            retVal = dialog.getReturnCode();
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(PluginUtil.getParentShell(),
                    Messages.rolsDlgErr, Messages.projDlgErrMsg, e);
        }
        return retVal;
    }

    /**
     * Method will change cursor type whenever required.
     * @param busy
     * true : Wait cursor
     * false : Normal arrow cursor
     */
    public static void showBusy(final boolean busy) {
        Display.getDefault().syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                Shell shell = Display.getDefault().getActiveShell();
                if (busy) { //show Busy Cursor
                    Cursor cursor = Display.getDefault().getSystemCursor(SWT.CURSOR_WAIT);
                    shell.setCursor(cursor);
                } else {
                    shell.setCursor(null);
                }
            }
        });
    }

    /**
     * Method will change cursor type of particular shell whenever required.
     * @param busy
     * @param shell
     */
    public static void showBusy(final boolean busy, final Shell shell) {
        Display.getDefault().syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                if (busy) { //show Busy Cursor
                    Cursor cursor = Display.getDefault().getSystemCursor(SWT.CURSOR_WAIT);
                    shell.setCursor(cursor);
                } else {
                    shell.setCursor(null);
                }
            }
        });
    }

    public static Shell getParentShell() {
        Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
        if (shell == null) {
            shell = new Shell();
        }
        return shell;
    }

    // TODO
//    public static void createSubscriptionTelemetryEvent(List<Subscription> oldSubList, String eventName) {
        /*try {
            List<Subscription> newSubList = AzureManagerImpl.getManager().getFullSubscriptionList();
            if (newSubList != null && newSubList.size() > 0) {
                for (Subscription sub : newSubList) {
                    if (!oldSubList.contains(sub)) {
                        Bundle bundle = Activator.getDefault().getBundle();
                        if (bundle != null) {
                            AppInsightsCustomEvent.create(eventName, bundle.getVersion().toString());
                        }
                        break;
                    }
                }
            }
        } catch (AzureCmdException e) {
            Activator.getDefault().log(e.getMessage(), e);
        }*/
//    }

    /**
     * This returns the resource has a file.
     *
     * @param fileEntry
     *            : File pointing to resource. null if file doesn't exists
     * @return
     */
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

    public static Image getImage(String imagePath) {
        try {
            URL imgUrl = Activator.getDefault().getBundle().getEntry(imagePath);
            URL imgFileURL = FileLocator.toFileURL(imgUrl);
            URL path = FileLocator.resolve(imgFileURL);
            String imgpath = path.getFile();
            return new Image(null, new FileInputStream(imgpath));
        } catch (Exception e) {
            Activator.getDefault().log("Error loading image", e);
            return null;
        }
    }

    public static boolean forceInstallPluginUsingMarketPlaceAsync(String pluginSymbolicName, String marketplaceURL, String manualInstallURL) {
        getParentShell().getDisplay().getDefault().asyncExec(() -> {
            forceInstallPluginUsingMarketplace(pluginSymbolicName, marketplaceURL, manualInstallURL);
        });
        return true;
    }

    /**
     * This function checks whether an Eclipse plug-in is installed or not.
     * If not, it will firstly check whether Eclipse Marketplace Client plug-in is installed and install it
     * Then it will use Marketplace Client to install the plug-in
     *
     * @param pluginSymbolicName: Symbolic name for the plug-in, for example, org.eclipse.fx.ide.feature for e(fx)clipse, org.scala-ide.sdt.core for Scala
     * @param marketplaceURL: Marketplace installation URL, for example, http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=421 for Scala
     * @return
     */
    public static boolean forceInstallPluginUsingMarketplace(String pluginSymbolicName, String marketplaceURL, String manualInstallURL) {
        boolean isTargetInstalled = checkPlugInInstallation(pluginSymbolicName);
        String manualInstallMessage = " You can also manually install using <a>" + manualInstallURL + "</a>!";
        if (isTargetInstalled) {
            return true;
        }

        boolean isMarketplacePluginInstalled = checkPlugInInstallation(marketplacePluginSymbolicName);
        if (!isMarketplacePluginInstalled) {
            PluginUtil.displayInfoDialogWithLink(getParentShell(), "Install missing plugin", "Start to install Eclipse Marketplace Client plugin which is required to install other missing plugin (" + pluginSymbolicName + ")! Click OK to start.", manualInstallMessage);
            forceInstallPluginUsingP2(marketplacePluginID);
        }

        try {
            PluginUtil.displayInfoDialogWithLink(getParentShell(), "Install missing plugin", "Start to install missing plugin (" + pluginSymbolicName + ")! Click OK to start.", manualInstallMessage);
            SolutionInstallationInfo info = MarketplaceUrlHandler.createSolutionInstallInfo(marketplaceURL);

            MarketplaceUrlHandler.triggerInstall(info);
        } catch (Exception e) {
            String errorMsg = "Error installing " + pluginSymbolicName + "! Please manually install using Eclipse marketplace from: Help -> Eclipse Marketplace. Click OK to continue.";
            PluginUtil.displayErrorDialogWithLinkAndLog(getParentShell(), "Fail to install", errorMsg, manualInstallMessage, e);

            try {
                MarketplaceClient.openMarketplaceWizard(null);
            } catch (Exception e1) {
                errorMsg = "Error installing " + pluginSymbolicName + " using Marketplace Client! Please manually install using Eclipse P2 repository from: Help -> Install New Software. Click OK to continue.";
                PluginUtil.displayErrorDialogWithLinkAndLog(getParentShell(), "Fail to install", errorMsg, manualInstallMessage, e1);

                return false;
            }
        } catch (NoClassDefFoundError e) {
            String errorMsg = "Error installing " + pluginSymbolicName + " using Marketplace Client! Please manually install using Eclipse P2 repository from: Help -> Install New Software. Click OK to continue.";
            PluginUtil.displayErrorDialogWithLinkAndLog(getParentShell(), "Fail to install", errorMsg, manualInstallMessage, e);

            return false;
        }

        return true;
    }

    public static boolean checkPlugInInstallation(String pluginSymbolicName) {
        Bundle[] bundles = Platform.getBundles(pluginSymbolicName, null);
        return bundles != null && bundles.length >= 0;
    }

    /**
     * @param targetVersion: Target minimum Java version
     * @return Return true if Java version is higher than target. By default return true since only Java 1.7 is required
     */
    public static boolean isJavaVersionHigherThanTarget(float targetVersion) {
        try {
            String javaVersion = System.getProperty("java.version");
            Float version = new Float(99);
            if (javaVersion.contains(".") || javaVersion.contains("_")) {
                String[] toParse = javaVersion.split("\\.");

                if (toParse.length >= 2) {
                    version = Float.valueOf(toParse[0] + "." + toParse[1]);

                     return version.floatValue() >= targetVersion;
                }
            } else {
                version = Float.valueOf(javaVersion);

                return version.floatValue() >= targetVersion;
            }
        } catch (Exception ignore) {
        }

        return true;
    }

    private static void forceInstallPluginUsingP2(String pluginGroupID) {
        URI repoURI = getEclipseP2Repository();
        ProvisioningUI provisioningUI = ProvisioningUI.getDefaultUI();

        if (provisioningUI != null && repoURI != null) {
            ProvisioningSession provisioningSession = provisioningUI.getSession();
            IProvisioningAgent provisioningAgent = null;
            if (provisioningSession != null && (provisioningAgent = provisioningSession.getProvisioningAgent()) != null) {
                IMetadataRepositoryManager manager = (IMetadataRepositoryManager)provisioningAgent.getService(IMetadataRepositoryManager.SERVICE_NAME);
                if (manager != null) {
                    try {
                        IMetadataRepository repository = manager.loadRepository(repoURI, null);
                        if (repository != null) {
                            IQueryResult<IInstallableUnit> iqr = repository.query(QueryUtil.createIUQuery(pluginGroupID), null);
                            if (iqr != null) {
                                Collection<IInstallableUnit> iuList = StreamSupport.stream(iqr.spliterator(), false).collect(Collectors.toList());

                                if (iuList.size() > 0) {
                                    InstallOperation io = new InstallOperation(provisioningSession, iuList);
                                    provisioningUI.openInstallWizard(iuList, io, null);

                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        String errorMsg = "Error installing " + pluginGroupID + "! Please manually install using Eclipse P2 repository from: Help -> Install New Software.... Click OK to continue.";
                        PluginUtil.displayErrorDialogAndLog(getParentShell(), "Fail to install", errorMsg, e);
                    }
                }
            }
        }

        String errorMsg = "Error installing " + pluginGroupID + "! In the following installation wizard, please select the right repository and then filter by " + pluginGroupID + "! Click OK to continue.";
        PluginUtil.displayErrorDialogAndLog(getParentShell(), "Fail to install", errorMsg, null);
        provisioningUI.openInstallWizard(null, null, null);
    }

    private static URI getEclipseP2Repository() {
        String repoPrefix = "download.eclipse.org/releases/";

        ProvisioningUI provisioningUI = ProvisioningUI.getDefaultUI();
        if (provisioningUI != null) {
            RepositoryTracker tracker = provisioningUI.getRepositoryTracker();
            if (tracker != null) {
                URI[] sites = tracker.getKnownRepositories(provisioningUI.getSession());
                for (URI site : sites) {
                    if (site.toString().contains(repoPrefix)) {
                        return site;
                    }
                }
            }
        }

        String start = "Version: ";
        String end = " (";
        URI repoSite = null;
        ProductProperties productProperties = new ProductProperties(Platform.getProduct());
        if (productProperties != null) {
            String aboutText = productProperties.getAboutText();
            int startIndex = aboutText.indexOf(start);
            int endIndex = aboutText.indexOf(end);
            String eclipseSimutaneousReleaseVersion = "";
            if (startIndex >= 0 && endIndex >= 0 && endIndex > startIndex) {
                eclipseSimutaneousReleaseVersion = aboutText.substring(startIndex + start.length(), endIndex);
                try {
                    repoSite = new URI("http://" + repoPrefix + eclipseSimutaneousReleaseVersion.toLowerCase());
                } catch (Exception e) {
                    repoSite = null;
                }
            }
        }

        return repoSite;
    }
}
