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

import com.microsoft.azuretools.core.components.AzureDialogWrapper;

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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
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
            FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
            fillLayout.marginHeight = 10;
            area.setLayout(fillLayout);
            GridData gridData = new GridData(GridData.FILL_BOTH);
            area.setLayoutData(gridData);

            Browser browser = new Browser(area, SWT.NONE);
            FillLayout layout = new FillLayout(SWT.HORIZONTAL);
            browser.setLayout(layout);
            browser.setText(createHtmlFormatMessage(area));
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
            long interval = deviceCode.getInterval();
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
                        // swallow the pending exception
                    } else {
                        checkTime++;
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "DeviceLoginWindow", e));
                    }
                }
            }
            Display.getDefault().syncExec(() -> super.close());
        }

        private String createHtmlFormatMessage(Composite composite) {
            Color color = composite.getBackground();
            FontData browserFontData = composite.getFont().getFontData()[0];
            String browserFontStyle = String.format("font-family: '%s'; font-size: 13", browserFontData.getName());
            String bgcolor = String.format("rgb(%s,%s,%s)", color.getRed(), color.getGreen(), color.getBlue());
            final String verificationUrl = deviceCode.getVerificationUrl();
            return String.format("<div style=\"" + browserFontStyle + "\">" + "<body style=\"background-color:%s\"><p>"
                , bgcolor) + deviceCode.getMessage()
                .replace(verificationUrl, String.format("<a href=\"%s\">%s</a>", verificationUrl, verificationUrl))
                + "</p><p>Waiting for signing in with the code ...</p></div>";
        }
    }
}
