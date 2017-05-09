package com.microsoft.azure.hdinsight.serverexplore.action;


import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.serverexplore.ui.AddNewEmulatorForm;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

@Name("Link an Emulator")
public class AddNewEmulatorAction extends NodeActionListener {

    private HDInsightRootModule hdInsightRootModule;

    public AddNewEmulatorAction(HDInsightRootModuleImpl hdInsightRootModule) {
        this.hdInsightRootModule = hdInsightRootModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        AddNewEmulatorForm form = new AddNewEmulatorForm((Project) hdInsightRootModule.getProject(), hdInsightRootModule);
        form.show();
    }
}
