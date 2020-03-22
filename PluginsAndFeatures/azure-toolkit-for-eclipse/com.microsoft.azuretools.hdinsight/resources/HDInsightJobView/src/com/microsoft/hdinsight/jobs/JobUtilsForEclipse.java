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
