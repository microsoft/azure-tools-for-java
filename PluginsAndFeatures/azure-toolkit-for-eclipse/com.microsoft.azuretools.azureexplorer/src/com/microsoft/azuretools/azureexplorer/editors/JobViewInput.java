package com.microsoft.azuretools.azureexplorer.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;

public class JobViewInput implements IEditorInput {
    private IClusterDetail clusterDetail;

    public JobViewInput(IClusterDetail clusterDetail) {
        this.clusterDetail = clusterDetail;
    }
    
    public IClusterDetail getClusterDetail() {
		return clusterDetail;
	}

	public String getClusterName() {
		return clusterDetail.getName();
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
    public Object getAdapter(Class aClass) {
        return null;
    }
}