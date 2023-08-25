/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui;

import com.azure.identity.DeviceCodeInfo;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;
import com.microsoft.azuretools.core.utils.AccessibilityUtils;
import lombok.Setter;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;

public class DeviceLoginWindow implements IDeviceLoginUI {

    private static ILog LOG = Activator.getDefault().getLog();
    private DeviceLoginDialog dialog;

    @Setter
    private Future future;

    private Shell shell;

    public DeviceLoginWindow(Shell shell) {
        this.shell = shell;
    }

    @Override
    public void promptDeviceCode(DeviceCodeInfo deviceCode) {
        final Runnable gui = () -> {
            try {
                dialog = new DeviceLoginDialog(shell, deviceCode);
                dialog.open();
            } catch (Exception ex) {
                ex.printStackTrace();
                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "DeviceLoginWindow", ex));
            }
        };
        Display.getDefault().syncExec(gui);
    }

    public void closePrompt() {
        if (dialog != null && dialog.getShell() != null) {
            Display.getDefault().syncExec(() -> dialog.close());
        }
    }

    @Override
    public void cancel() {
        if (future != null) {
            this.future.cancel(true);
        }
    }

    private class DeviceLoginDialog extends AzureDialogWrapper {

        private final DeviceCodeInfo deviceCode;
        private Link link;

        public DeviceLoginDialog(Shell parentShell, DeviceCodeInfo deviceCode
        ) {
            super(parentShell);
            setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
            this.deviceCode = deviceCode;
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

            link = new Link(area, SWT.NONE);
            link.setText(createHtmlFormatMessage());
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
            AccessibilityUtils.addAccessibilityNameForUIComponent(link, deviceCode.getMessage());

            Label label = new Label(area, SWT.NONE);
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
            DeviceLoginWindow.this.cancel();
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

        private String createHtmlFormatMessage() {
            final String verificationUrl = deviceCode.getVerificationUrl();
            return deviceCode.getMessage()
                .replace(verificationUrl, String.format("<a href=\"%s\" id=\"%s\" title=\"%s\">%s</a>", verificationUrl, verificationUrl, verificationUrl, verificationUrl));
        }
    }


}
