package com.microsoft.azuretools.azureexplorer.editors.rediscache;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.azuretools.azureexplorer.editors.StorageEditorInput;

public class RedisExplorerEditorInput implements IEditorInput {
    
    private String id;
    private String subscriptionId;
    
    public RedisExplorerEditorInput(String sid, String id) {
        this.id = id;
        this.setSubscriptionId(sid);
    }

    @Override
    public <T> T getAdapter(Class<T> arg0) {
        return null;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return null;
    }
    
    @Override
    public boolean equals(Object o) {
        return true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

}
