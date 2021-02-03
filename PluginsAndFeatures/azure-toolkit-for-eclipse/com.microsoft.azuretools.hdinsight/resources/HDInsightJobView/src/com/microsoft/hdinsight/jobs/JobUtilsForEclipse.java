/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.hdinsight.jobs;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.File;

public class JobUtilsForEclipse {
    private static String yarnUIHisotryFormat = "https://%s.azurehdinsight.net/yarnui/hn/cluster/app/%s";
    private static String sparkUIHistoryFormat = "https://%s.azurehdinsight.net/sparkhistory/history/%s/jobs";

    public void openYarnUIHistory(String clusterName, String applicationId) {
        String jobUrl = String.format(yarnUIHisotryFormat, clusterName, applicationId);
        try {
            openBrowser(jobUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openSparkUIHistory(String clusterName, String applicationId) {
        String jobUrl = String.format(sparkUIHistoryFormat, clusterName, applicationId);
        try {
            openBrowser(jobUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void openBrowser(String url) throws Exception{
        Application application = new Application() {
            @Override
            public void start(Stage primaryStage) throws Exception {
                getHostServices().showDocument(url);
            }
        };

        application.start(null);
    }
}
