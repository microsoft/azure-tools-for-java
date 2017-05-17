package com.microsoft.azuretools.azureexplorer.forms;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.utils.AzureModelController;

public class FormUtils {
	public static void loadLocationsAndResourceGrps(ILog LOG, Shell shell) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Getting locations and resource groups information of subscriptions...", IProgressMonitor.UNKNOWN);
                try {
                	AzureModelController.updateSubscriptionMaps(null);
                } catch (Exception ex) {
                    System.out.println(
                            "run@ProgressDialog@doRetriveResourceGroups@CreateRedisCacheForm: " + ex.getMessage());
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "run@ProgressDialog@doRetriveResourceGroups@CreateRedisCacheForm", ex));
                }
            }
        };
        try {
            new ProgressMonitorDialog(shell).run(true, false, op);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "doRetriveResourceGroups@CreateRedisCacheForm", ex));
        }
    }
}
