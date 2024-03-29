/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.hdinsight;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.hdinsight.util.HDInsightJobViewUtils;
import com.microsoft.azuretools.core.utils.Messages;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.microsoft.azuretools.hdinsight"; //$NON-NLS-1$

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

        com.microsoft.azuretools.azureexplorer.helpers.HDInsightHelperImpl.initHDInsightLoader();

        String enabledProperty = DefaultLoader.getIdeHelper().getApplicationProperty(Messages.HDInsightFeatureEnabled);
        if(StringHelper.isNullOrWhiteSpace(enabledProperty)) {
            AppInsightsClient.create(Messages.HDInsightFeatureEnabled, context.getBundle().getVersion().toString());
            DefaultLoader.getIdeHelper().setApplicationProperty(Messages.HDInsightFeatureEnabled, "true");
        }
        HDInsightJobViewUtils.checkInitlize();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
        HDInsightJobViewUtils.closeJobViewHttpServer();
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public void log(String message, Throwable excp) {
        getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, excp));
    }
}
