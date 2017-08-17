package com.microsoft.azuretools.core.azureexplorer.helpers;

import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelper;

import org.eclipse.jface.dialogs.MessageDialog;

public class MvpUIHelperImpl implements MvpUIHelper {

    @Override
    public void showError(String message) {
        MessageDialog.openError(null, message, message);
    }

    @Override
    public void showException(String message, Exception e) {
        Activator.getDefault().log(message, e);
        MessageDialog.openError(null, message, e.getMessage());
    }

}
