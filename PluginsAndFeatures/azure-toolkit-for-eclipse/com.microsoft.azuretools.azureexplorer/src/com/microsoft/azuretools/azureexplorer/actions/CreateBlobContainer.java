package com.microsoft.azuretools.azureexplorer.actions;

import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azureexplorer.forms.CreateBlobContainerForm;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.BlobModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageNode;

@Name("Create blob container")
public class CreateBlobContainer extends NodeActionListener {
    private RefreshableNode parent;

    public CreateBlobContainer(BlobModule parent) {
        this.parent = parent;
    }

    public CreateBlobContainer(StorageNode parent) {
        this.parent = parent;
    }
    
    @Override
    public void actionPerformed(NodeActionEvent e) {
    	String connectionString;
    	if (parent instanceof BlobModule) {
    		connectionString = StorageClientSDKManager.getConnectionString(((BlobModule) parent).getStorageAccount());
    	} else {
    		connectionString = StorageClientSDKManager.getConnectionString(((StorageNode) parent).getStorageAccount());
    	}
        CreateBlobContainerForm form = new CreateBlobContainerForm(PluginUtil.getParentShell(), connectionString);
        if(parent instanceof StorageNode) { form.setSubscription(new SubscriptionDetail(((StorageNode)parent).getSubscriptionId(), null, null, true)); }
        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                parent.removeAllChildNodes();
                parent.load(false);
            }
        });
        form.open();
    }
}