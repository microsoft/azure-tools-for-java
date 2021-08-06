/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.hdinsight.jobs.framework

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.microsoft.azure.hdinsight.spark.jobs.JobViewHttpServer
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class JobViewPanel(private val rootPath: String, private val clusterName: String) {
    private val QUERY_TEMPLATE = "?clusterName=%s&port=%s"
    private val myComponent: JComponent

    init {
        val url = String.format("file:///%s/com.microsoft.hdinsight/hdinsight/job/html/index.html", rootPath)
/*
        // for debug only
        final String ideaSystemPath = System.getProperty("idea.system.path");
        if(!StringHelper.isNullOrWhiteSpace(ideaSystemPath) && ideaSystemPath.contains("idea-sandbox")) {
            final String workFolder = System.getProperty("user.dir");
            final String path = "Utils/hdinsight-node-common/resources/htmlResources/hdinsight/job/html/index.html";
            url = String.format("file:///%s/%s", workFolder, path);
        }
*/
        val queryString = String.format(QUERY_TEMPLATE, clusterName, JobViewHttpServer.getPort())
        val webUrl = url + queryString

        if (!JBCefApp.isSupported()) {
            throw RuntimeException("JCEF is not enabled in current IDE. Please follow the instruction to enable JCEF first. https://youtrack.jetbrains" +
                    ".com/issue/IDEA-231833#focus=streamItem-27-3993099.0-0")
        }

        val panel = JPanel(BorderLayout())
        panel.add(JBCefBrowser(webUrl).component)
        myComponent = panel
    }

    val component: JComponent
        get() = myComponent
}
