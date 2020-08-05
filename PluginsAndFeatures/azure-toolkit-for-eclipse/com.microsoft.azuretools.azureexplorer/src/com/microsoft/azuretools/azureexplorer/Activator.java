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

package com.microsoft.azuretools.azureexplorer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azuretools.azureexplorer.helpers.UIHelperImpl;
import com.microsoft.azuretools.core.mvp.ui.base.AppSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.microsoft.azuretools.azureexplorer"; //$NON-NLS-1$
    public static final String AZURE_IMAGE_ID = "AZURE_IMAGE_ID";
    public static final String MOBILE_SERVICES_IMAGE_ID = "MOBILE_SERVICES_IMAGE_ID";
    public static final String VIRTUAL_MACHINES_IMAGE_ID = "VIRTUAL_MACHINES_IMAGE_ID";
    public static final String SCHEDULED_JOB_IMAGE_ID = "SCHEDULED_JOB_IMAGE_ID";
    public static final String REFRESH_IMAGE_ID = "REFRESH_IMAGE_ID";

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        DefaultLoader.setUiHelper(new UIHelperImpl());
        com.microsoft.azuretools.azureexplorer.helpers.HDInsightHelperImpl.initHDInsightLoader();

        Node.setNode2Actions(NodeActionsMap.node2Actions);
//        ServiceExplorerView serviceExplorerView = (ServiceExplorerView) PlatformUI
//                .getWorkbench().getActiveWorkbenchWindow()
//                .getActivePage().showView("ServiceExplorerView");
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
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

    public void log(String message, Throwable excp) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, excp));
    }

//    protected void initializeImageRegistry(ImageRegistry reg) {
//        Bundle bundle = Platform.getBundle(PLUGIN_ID);
//        reg.put(AZURE_IMAGE_ID, createImageDescriptor(bundle, "icons/azure_explorer.png"));
//        reg.put(MOBILE_SERVICES_IMAGE_ID, createImageDescriptor(bundle, "icons/mobileservices.png"));
//        reg.put(VIRTUAL_MACHINES_IMAGE_ID, createImageDescriptor(bundle, "icons/virtualmachines.png"));
//        reg.put(REFRESH_IMAGE_ID, createImageDescriptor(bundle, "icons/refresh.png"));
//    }
//
//    private ImageDescriptor createImageDescriptor(Bundle bundle, String iconPath) {
//        IPath path = new Path(iconPath);
//        URL url = FileLocator.find(bundle, path, null);
//        return ImageDescriptor.createFromURL(url);
//    }
}
