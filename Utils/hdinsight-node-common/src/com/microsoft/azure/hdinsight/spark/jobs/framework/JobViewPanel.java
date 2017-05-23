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

package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azure.hdinsight.spark.jobs.JobHttpServer;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public final class JobViewPanel extends JFXPanel {

    private final Object jobUtil;
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
        jobUtil = new JobUtils();
    }

    private void init(final JFXPanel panel) {
        String url = rootPath + "/com.microsoft.hdinsight/hdinsight/job/html/index.html";

        // for debug only
        final String debugFlag = System.getProperty("hdinsight.debug");
        if(!StringHelper.isNullOrWhiteSpace(debugFlag)) {
            final String workFolder = System.getProperty("user.dir");
            final String path = "resource/hdinsight-node-common/resources/htmlResources/hdinsight/job/html/index.html";
            url = String.format("file:///%s/%s", workFolder, path);
        }
        // end of for debug only part

        final String queryString = String.format(QUERY_TEMPLATE, clusterName, JobHttpServer.getPort());
        final String webUrl = "file:///" + url + queryString;

        Platform.setImplicitExit(false);
        Platform.runLater(()-> {
            webView = new WebView();
            panel.setScene(new Scene(webView));
            webEngine = webView.getEngine();
            webEngine.setJavaScriptEnabled(true);

            // pass the following object to js
            JSObject win = (JSObject) webEngine.executeScript("window");
            win.setMember("JobUtils", jobUtil);
            win.setMember("port", String.valueOf(JobHttpServer.getPort()));
            win.setMember("cluster", clusterName);

            // JavaFx will load web page twice when initialization
            if (!alreadyLoad) {
                webEngine.load(webUrl);
                alreadyLoad = true;
            }
        });
    }

}