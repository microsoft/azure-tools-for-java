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

package com.microsoft.azuretools.hdinsight.spark.actions;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.HDINSIGHT;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.hdinsight.Activator;
import com.microsoft.azuretools.hdinsight.common2.HDInsightUtil;
import com.microsoft.azuretools.hdinsight.spark.ui.SparkSubmissionExDialog;
import com.microsoft.azuretools.core.utils.Messages;

public class SubmitHandler extends AzureAbstractHandler {
    private List<IClusterDetail> cachedClusterDetails = null;

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        synchronized (SubmitHandler.class) {
                    TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);

                    IProject project = null;
                    Object selectedObj = selection.getFirstElement();

                    // for different version of Eclipse
                    if (selectedObj instanceof IProjectNature) {
                        IProjectNature projectNature = (IProjectNature)selectedObj;
                        project = projectNature.getProject();
                    } else if (selectedObj instanceof IProject){
                        project = (IProject)selectedObj;
                    }

                    AppInsightsClient.create(Messages.SparkSubmissionRightClickProject, Activator.getDefault().getBundle().getVersion().toString());
                    EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkSubmissionRightClickProject, null);
                    HDInsightUtil.showInfoOnSubmissionMessageWindow("List spark clusters ...");

                    cachedClusterDetails = ClusterManagerEx.getInstance().getClusterDetailsWithoutAsync(true);
                    if(!ClusterManagerEx.getInstance().isSelectedSubscriptionExist()) {
                        HDInsightUtil.showWarningMessageOnSubmissionMessageWindow("No selected subscription(s), Please go to HDInsight Explorer to sign in....");
                    }

                    if (ClusterManagerEx.getInstance().isListClusterSuccess()) {
                        HDInsightUtil.showInfoOnSubmissionMessageWindow("List spark clusters successfully");
                    } else {
                        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Error : Failed to list spark clusters.");
                    }
                    if (ClusterManagerEx.getInstance().isListAdditionalClusterSuccess()) {
                        HDInsightUtil.showInfoOnSubmissionMessageWindow("List additional spark clusters successfully");
                    } else {
                        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Error: Failed to list additional cluster");
                    }
                    SparkSubmissionExDialog dialog = new SparkSubmissionExDialog(PluginUtil.getParentShell(), cachedClusterDetails == null ? Collections.emptyList() : cachedClusterDetails, project, null);
                    dialog.open();
            return null;
        }

    }
}
