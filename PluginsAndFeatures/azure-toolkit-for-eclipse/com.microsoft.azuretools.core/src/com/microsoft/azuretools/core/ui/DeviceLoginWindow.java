/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.aad.adal4j.AdalErrorCode;
import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;

public class DeviceLoginWindow implements IDeviceLoginUI {

    private static ILog LOG = Activator.getDefault().getLog();
    private AuthenticationResult authenticationResult = null;

    public DeviceLoginWindow() {
    }

    @Override
    public AuthenticationResult authenticate(AuthenticationContext authenticationContext, DeviceCode deviceCode,
        AuthenticationCallback<AuthenticationResult> authenticationCallback) {
        final Runnable gui = () -> {
            try {
                Display display = Display.getDefault();
                final Shell activeShell = display.getActiveShell();
                DeviceLoginDialog dlg = new DeviceLoginDialog(activeShell, authenticationContext, deviceCode,
                    authenticationCallback);
                dlg.open();
                authenticationResult = dlg.authenticationResult;
            } catch (Exception ex) {
                ex.printStackTrace();
                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "DeviceLoginWindow", ex));
            }
        };
        Display.getDefault().syncExec(gui);
        return authenticationResult;
    }

    private class DeviceLoginDialog extends AzureDialogWrapper {

        private final DeviceCode deviceCode;
        private final Future<?> future;
        private AuthenticationResult authenticationResult = null;
        private Browser browser;
        private Link link;
        private final ExecutorService es = Executors.newSingleThreadExecutor();

        public DeviceLoginDialog(Shell parentShell, AuthenticationContext authenticationContext, DeviceCode deviceCode,
            AuthenticationCallback<AuthenticationResult> authenticationCallback) {
            super(parentShell);
            setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
            this.deviceCode = deviceCode;
            future = es
                .submit(() -> pullAuthenticationResult(authenticationContext, deviceCode, authenticationCallback));
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite area = (Composite) super.createDialogArea(parent);
            FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
            fillLayout.marginHeight = 10;
            fillLayout.marginWidth = 10;
            area.setLayout(fillLayout);
            GridData gridData = new GridData(GridData.FILL_BOTH);
            area.setLayoutData(gridData);


            link = new Link(area,SWT.NONE);
            link.setText(createHtmlFormatMessage(area));
            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
              .openURL(new URL(deviceCode.getVerificationUrl()));
                    } catch (PartInitException | MalformedURLException ex) {
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "DeviceLoginWindow", ex));
                    }
                }
            });

            Label label = new Label(area,SWT.NONE);
            label.setText("Waiting for signing in with the code, do not close the window.");

            return area;
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText("Azure Device Login");
        }

        @Override
        protected void okPressed() {
            final StringSelection selection = new StringSelection(deviceCode.getUserCode());
            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            try {
                PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
                    .openURL(new URL(deviceCode.getVerificationUrl()));
            } catch (Exception e) {
                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "DeviceLoginWindow", e));
            }
        }

        @Override
        protected void cancelPressed() {
            future.cancel(true);
            super.cancelPressed();
        }

        @Override
        protected Point getInitialSize() {
            Point shellSize = super.getInitialSize();
            return new Point(Math.max(this.convertHorizontalDLUsToPixels(350), shellSize.x),
                Math.max(this.convertVerticalDLUsToPixels(120), shellSize.y));
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            Button okButton = getButton(IDialogConstants.OK_ID);
            okButton.setText("Copy&&Open");
        }

        private void pullAuthenticationResult(final AuthenticationContext ctx, final DeviceCode deviceCode,
            final AuthenticationCallback<AuthenticationResult> callback) {
            long remaining = deviceCode.getExpiresIn();
            long interval = Math.min(3, deviceCode.getInterval());
            long expiredTime = System.currentTimeMillis() + remaining * 1000;
            int maxTries = 3;
            int checkTime = 0;
            while (System.currentTimeMillis() < expiredTime && checkTime < maxTries && authenticationResult == null) {
                try {
                    Thread.sleep(1000 * interval);
                    authenticationResult = ctx.acquireTokenByDeviceCode(deviceCode, callback).get();
                } catch (Exception e) {
                    if (e.getCause() instanceof AuthenticationException &&
                        ((AuthenticationException) e.getCause()).getErrorCode()
                            == AdalErrorCode.AUTHORIZATION_PENDING) {
                        // reset the retryCount to zero, will quit only for 3 consecutive fail
                        checkTime = 0;
                    } else {
                        checkTime++;
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "DeviceLoginWindow", e));
                    }
                }
            }
            Display.getDefault().syncExec(() -> super.close());
        }

        private String createHtmlFormatMessage(Composite composite) {
            final String verificationUrl = deviceCode.getVerificationUrl();
            return deviceCode.getMessage()
                .replace(verificationUrl, String.format("<a href=\"%s\" id=\"%s\" title=\"%s\">%s</a>", verificationUrl, verificationUrl,verificationUrl,verificationUrl));
        }
    }
}
