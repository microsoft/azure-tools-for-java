package com.microsoft.azuretools.azureexplorer.forms;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.utils.AzureModelController;

public class FormUtils {
	
	private static Activator LOG = Activator.getDefault();
	
	// String const
	private static final String UPDATE_LOCATION_AND_GRP = "Updating locations and resource groups";
	private static final String ERROR_UPDATE_LOACTION_AND_GRP = "Error occurred while update location and resource group information.";
	
	public static void loadLocationsAndResourceGrps(Shell shell) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask(UPDATE_LOCATION_AND_GRP, IProgressMonitor.UNKNOWN);
                try {
                	AzureModelController.updateSubscriptionMaps(null);
                } catch (Exception ex) {
                	ex.printStackTrace();
                	LOG.log(ERROR_UPDATE_LOACTION_AND_GRP, ex);
                }
            }
        };
        try {
            new ProgressMonitorDialog(shell).run(true, false, op);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(ERROR_UPDATE_LOACTION_AND_GRP, ex);
        }
    }
}
