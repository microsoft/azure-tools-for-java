package com.microsoft.azuretools.core.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.adauth.IWebUi;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.authmanage.interact.IUIFactory;
import com.microsoft.azuretools.core.utils.Notification;
import com.microsoft.azuretools.core.utils.ProgressTaskModal;
import com.microsoft.azuretools.utils.IProgressTaskImpl;

public class UIFactory implements IUIFactory {

    @Override
    public INotification getNotificationWindow() {
       return new Notification();
    }

    @Override
    public IWebUi getWebUi() {
        return new LoginWindow();
    }

    @Override
    public IProgressTaskImpl getProgressTaskImpl() {
    	Display display = Display.getDefault();
        Shell activeShell = display.getActiveShell();
        return new ProgressTaskModal(activeShell);
    }

    @Override
    public IDeviceLoginUI getDeviceLoginUI() {
        return new DeviceLoginWindow();
    }
}
