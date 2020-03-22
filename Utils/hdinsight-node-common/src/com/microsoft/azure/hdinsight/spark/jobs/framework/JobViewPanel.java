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
package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azure.hdinsight.spark.jobs.JobViewHttpServer;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public final class JobViewPanel extends JFXPanel {

    private final String rootPath;
    private final String clusterName;
    private WebView webView;
    private WebEngine webEngine;
    private boolean alreadyLoad = false;

    private static final String QUERY_TEMPLATE = "?clusterName=%s&port=%s&engineType=javafx";

    public JobViewPanel(@NotNull String rootPath, @NotNull String clusterName) {
        this.rootPath = rootPath;
        this.clusterName = clusterName;
        init(this);
    }

    private void init(final JFXPanel panel) {
        String url = String.format("file:///%s/com.microsoft.hdinsight/hdinsight/job/html/index.html", rootPath);

         // for debug only
//        final String ideaSystemPath = System.getProperty("idea.system.path");
//        if(!StringHelper.isNullOrWhiteSpace(ideaSystemPath) && ideaSystemPath.contains("idea-sandbox")) {
//            final String workFolder = System.getProperty("user.dir");
//            final String path = "Utils/hdinsight-node-common/resources/htmlResources/hdinsight/job/html/index.html";
//            url = String.format("file:///%s/%s", workFolder, path);
//        }
        // end of for debug only part

       final String queryString = String.format(QUERY_TEMPLATE, clusterName, JobViewHttpServer.getPort());
        final String webUrl = url + queryString;

        Platform.setImplicitExit(false);
        Platform.runLater(()-> {
            webView = new WebView();
            panel.setScene(new Scene(webView));
            webEngine = webView.getEngine();
            webEngine.setJavaScriptEnabled(true);
            if (!alreadyLoad) {
                webEngine.load(webUrl);
                alreadyLoad = true;
            }
        });
    }
}