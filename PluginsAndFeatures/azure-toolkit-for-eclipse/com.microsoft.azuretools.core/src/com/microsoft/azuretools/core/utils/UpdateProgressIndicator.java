package com.microsoft.azuretools.core.utils;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.azuretools.utils.IProgressIndicator;

public class UpdateProgressIndicator implements IProgressIndicator {
    private IProgressMonitor monitor;

    public UpdateProgressIndicator(IProgressMonitor monitor) {
        this.monitor = monitor;
    }
    
    @Override
    public void setText(String s) {
    	monitor.setTaskName(s);
    }

    @Override
    public void setText2(String s) {
        monitor.subTask(s);
    }

    @Override
    public void setFraction(double v) {
    	monitor.internalWorked(v);
    }

    @Override
    public boolean isCanceled() {
        return monitor.isCanceled();
    }
    
    @Override
    public void notifyComplete() {}
}

