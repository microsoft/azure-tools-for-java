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

package com.microsoft.azuretools.azureexplorer.editors;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.azuretools.azureexplorer.hdinsight.*;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.jobs.JobViewHttpServer;

public class JobViewEditor extends EditorPart {

    private static final String QUERY_TEMPLATE = "?clusterName=%s&port=%s&engineType=javafx";

    private IClusterDetail clusterDetail;

    @Override
    public void doSave(IProgressMonitor iProgressMonitor) {
        AppInsightsClient.create("HDInsight.Spark.CloseJobviewPage", null);
    }

    @Override
    public void doSaveAs() {
        AppInsightsClient.create("HDInsight.Spark.CloseJobviewPage", null);
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        AppInsightsClient.create("HDInsight.Spark.OpenJobviewPage", null);
        setSite(site);
        setInput(input);
        clusterDetail = ((JobViewInput) input).getClusterDetail();
        setPartName(clusterDetail.getName() + " Spark JobView");
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite composite) {
        composite.setLayout(new FillLayout());

        final String indexPath = PluginUtil.pluginFolder + "/com.microsoft.azuretools.hdinsight/html"
                + "/hdinsight/job/html/index.html";
        File indexFile = new File(indexPath);
        if(indexFile.exists()) {
            final String queryString = String.format(QUERY_TEMPLATE, clusterDetail.getName(),
                    JobViewHttpServer.getPort());
            final String webUrl = "file:///" + indexPath.replace("\\", "/") + queryString;
            FxClassLoader.loadJavaFxForJobView(composite, webUrl);
        } else {
            DefaultLoader.getUIHelper().showError("HDInsight Job view index page not exist!", "job view");
        }
    }

    @Override
    public void setFocus() {
    }

}
