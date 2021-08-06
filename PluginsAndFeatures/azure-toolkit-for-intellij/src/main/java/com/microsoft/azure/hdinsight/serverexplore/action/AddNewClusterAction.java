/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.serverexplore.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.serverexplore.ui.AddNewClusterForm;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

@Name("Link A Cluster")
public class AddNewClusterAction extends NodeActionListener {

    private HDInsightRootModule hdInsightRootModule;

    public AddNewClusterAction(HDInsightRootModuleImpl hdInsightRootModule) {
        this.hdInsightRootModule = hdInsightRootModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        AddNewClusterForm form = new AddNewClusterForm((Project) hdInsightRootModule.getProject(), hdInsightRootModule);
        form.show();
    }
}
