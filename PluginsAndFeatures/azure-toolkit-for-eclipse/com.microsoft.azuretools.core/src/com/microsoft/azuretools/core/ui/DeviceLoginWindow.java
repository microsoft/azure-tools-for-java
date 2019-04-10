/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azuretools.core.ui;

import com.microsoft.aad.adal4j.AdalErrorCode;
import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;

import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.core.Activator;
import org.eclipse.ui.PlatformUI;

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
                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@Runnable@LoginWindow", ex));
            }
        };
        Display.getDefault().syncExec(gui);
        return authenticationResult;
    }

    private class DeviceLoginDialog extends AzureTitleAreaDialogWrapper {

        private final DeviceCode deviceCode;
        private final Future<?> future;
        private AuthenticationResult authenticationResult = null;
        private final ExecutorService es = Executors.newSingleThreadExecutor();

        public DeviceLoginDialog(Shell parentShell, AuthenticationContext authenticationContext, DeviceCode deviceCode,
            AuthenticationCallback<AuthenticationResult> authenticationCallback) {
            super(parentShell);
            setHelpAvailable(false);
            setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
            this.deviceCode = deviceCode;
            future = es
                .submit(() -> pullAuthenticationResult(authenticationContext, deviceCode, authenticationCallback));
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            setTitle("Azure Device Login");
            setMessage("Azure Device Login");
            GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
            Browser browser = new Browser(parent, SWT.NONE);
            FillLayout layout = new FillLayout(SWT.HORIZONTAL);
            layout.marginHeight = 20;
            browser.setLayout(layout);
            browser.setLayoutData(gridData);
            browser.setText(createHtmlFormatMessage());
            browser.addLocationListener(new LocationListener() {
                @Override
                public void changing(LocationEvent event) {
                    try {
                        if (event.location.contains("http")) {
                            event.doit = false;
                            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
                                .openURL(new URL(event.location));
                        }
                    } catch (Exception ex) {
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "DeviceLoginWindow", ex));
                    }
                }

                @Override
                public void changed(LocationEvent locationEvent) {
                }
            });
            return browser;
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
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            Button okButton = getButton(IDialogConstants.OK_ID);
            okButton.setText("Copy&&Open");
        }

        private void pullAuthenticationResult(final AuthenticationContext ctx, final DeviceCode deviceCode,
            final AuthenticationCallback<AuthenticationResult> callback) {
            long remaining = deviceCode.getExpiresIn();
            while (remaining > 0 && authenticationResult == null) {
                try {
                    remaining--;
                    Thread.sleep(1000);
                    authenticationResult = ctx.acquireTokenByDeviceCode(deviceCode, callback).get();
                } catch (Exception e) {
                    if (e.getCause() instanceof AuthenticationException &&
                        ((AuthenticationException) e.getCause()).getErrorCode()
                            == AdalErrorCode.AUTHORIZATION_PENDING) {
                        // swallow the pending exception
                    } else {
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "DeviceLoginWindow", e));
                        break;
                    }
                }
            }
            Display.getDefault().syncExec(() -> super.close());
        }

        private String createHtmlFormatMessage() {
            final String verificationUrl = deviceCode.getVerificationUrl();
            return "<body bgcolor=\"#F0F0F0\"><p>"
                + deviceCode.getMessage()
                .replace(verificationUrl, String.format("<a href=\"%s\">%s</a>", verificationUrl, verificationUrl))
                + "</p><p>Waiting for signing in with the code ...</p>";
        }
    }
}
