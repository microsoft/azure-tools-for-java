/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.azureexplorer.helpers;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import com.microsoft.azure.hdinsight.common.HDInsightHelper;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.azureexplorer.actions.AddNewHDInsightReaderClusterAction;
import com.microsoft.azuretools.azureexplorer.editors.JobViewInput;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

public class HDInsightHelperImpl implements HDInsightHelper {
	private static final String HDINSIHGT_BUNDLE_ID = "com.microsoft.azuretools.hdinsight";
	private static String instID = "";
	private static boolean isOptIn = true;

	static {
		final String pluginInstLoc = String.format("%s%s%s", PluginUtil.pluginFolder, File.separator,
				Messages.commonPluginID);
		final String dataFile = String.format("%s%s%s", pluginInstLoc, File.separator, Messages.dataFileName);
		instID = DataOperations.getProperty(dataFile, Messages.instID);
        isOptIn = Boolean.parseBoolean(DataOperations.getProperty(dataFile, Messages.prefVal));
	}

	public void openJobViewEditor(Object projectObject, @NotNull String clusterName) {
		try {
			loadHDInsightPlugin();
		} catch (BundleException bundleException) {
			Activator.getDefault().log("Error loading plugin " + HDINSIHGT_BUNDLE_ID, bundleException);
		}

		IClusterDetail clusterDetail = JobViewManager.getCluster(clusterName);
		IWorkbench workbench = PlatformUI.getWorkbench();
		IEditorDescriptor editorDescriptor = workbench.getEditorRegistry()
				.findEditor("com.microsoft.azure.hdinsight.jobview");
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IEditorPart newEditor = page.openEditor(new JobViewInput(clusterDetail), editorDescriptor.getId());
		} catch (PartInitException e2) {
			Activator.getDefault().log("Error opening " + clusterDetail.getName(), e2);
		}
	}

	public void closeJobViewEditor(Object projectObject, String uuid) {

	}

	public String getPluginRootPath() {
		return null;
	}

	private void loadHDInsightPlugin() throws BundleException {
		Bundle bundle = Platform.getBundle(HDINSIHGT_BUNDLE_ID);
		if (bundle == null || bundle.getState() == Bundle.ACTIVE) {
			return;
		} else {
			bundle.start();
		}
	}

	@Override
	public String getInstallationId() {
		if (isOptIn()) {
		    return instID;
        } else {
		    return "";
        }
	}
	
    @Override
    public boolean isIntelliJPlugin() {
        return false;
    }

    @Override
    public boolean isOptIn() {
        return isOptIn;
    }

    public static synchronized void initHDInsightLoader() {
        if (HDInsightLoader.getHDInsightHelper() == null) {
            HDInsightLoader.setHHDInsightHelper(new com.microsoft.azuretools.azureexplorer.helpers.HDInsightHelperImpl());
        }
    }
    
    @NotNull
    public NodeActionListener createAddNewHDInsightReaderClusterAction(@NotNull HDInsightRootModule module, @NotNull ClusterDetail clusterDetail) {
        return new AddNewHDInsightReaderClusterAction(module, clusterDetail);
    }

}
