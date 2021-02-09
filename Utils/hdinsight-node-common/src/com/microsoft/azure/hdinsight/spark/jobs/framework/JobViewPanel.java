/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
