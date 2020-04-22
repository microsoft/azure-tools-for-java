/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.core.ui;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SIGNIN;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.signInDCProp;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.signInSPProp;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.BaseADAuthManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import com.microsoft.azuretools.adauth.AuthCanceledException;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.AuthMethod;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;

public class SignInDialog extends AzureTitleAreaDialogWrapper {
    private static ILog LOG = Activator.getDefault().getLog();
    private Text textAuthenticationFilePath;
    private Button rbtnDevice;
    private Button rbtnAutomated;
    private Label lblAuthenticationFile;
    private Button btnBrowse;
    private Button btnCreateAuthenticationFile;
    private Label lblDeviceInfo;
    private Label lblAutomatedInfo;

    private AuthMethodDetails authMethodDetails;
    private String accountEmail;
    FileDialog fileDialog;

    public AuthMethodDetails getAuthMethodDetails() {
        return authMethodDetails;
    }

    /**
     * Create the dialog.
     * @param parentShell
     */
    public SignInDialog(Shell parentShell) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
    }

    public static SignInDialog go(Shell parentShell, AuthMethodDetails authMethodDetails) {
        SignInDialog d = new SignInDialog(parentShell);
        d.authMethodDetails = authMethodDetails;
        d.create();
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText("Sign in");
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Azure Sign In");
        setTitle("Azure Sign In");
        getShell().setText("Azure Sign In");
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new FillLayout(SWT.HORIZONTAL));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite composite = new Composite(container, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        Group group = new Group(composite, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        group.setText("Authentication Method");
        group.setLayout(new GridLayout(1, false));

        rbtnDevice = new Button(group, SWT.RADIO);
        rbtnDevice.setSelection(true);
        rbtnDevice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (rbtnDevice.getSelection()) {
                    rbtnDevice.setFocus();
                    enableAutomatedAuthControls(false);
                }
            }
        });
        rbtnDevice.setText("Device Login");

        Composite compositeDevice = new Composite(group, SWT.NONE);
        GridData gdCompositeDevice = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gdCompositeDevice.heightHint = 38;
        gdCompositeDevice.widthHint = 66;
        compositeDevice.setLayoutData(gdCompositeDevice);
        compositeDevice.setLayout(new GridLayout(1, false));

        lblDeviceInfo = new Label(compositeDevice, SWT.WRAP);
        GridData gdLblDeviceInfo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gdLblDeviceInfo.horizontalIndent = 11;
        lblDeviceInfo.setLayoutData(gdLblDeviceInfo);
        lblDeviceInfo.setText("You will need to open an external browser and sign in with a generated device code.");

        rbtnAutomated = new Button(group, SWT.RADIO);
        rbtnAutomated.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (rbtnAutomated.getSelection()) {
                    // Set the radio button to be focused or the default one will be selected when refresh
                    // For issue https://github.com/microsoft/azure-tools-for-java/issues/3543
                    rbtnAutomated.setFocus();
                    enableAutomatedAuthControls(true);
                }
            }
        });
        rbtnAutomated.setText("Service Principal");

        Composite compositeAutomated = new Composite(group, SWT.NONE);
        compositeAutomated.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        compositeAutomated.setLayout(new GridLayout(3, false));

        lblAutomatedInfo = new Label(compositeAutomated, SWT.WRAP | SWT.HORIZONTAL);
        lblAutomatedInfo.setEnabled(false);
        GridData gdLblAutomatedInfo = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        gdLblAutomatedInfo.widthHint = 483;
        gdLblAutomatedInfo.horizontalIndent = 11;
        gdLblAutomatedInfo.heightHint = 49;
        lblAutomatedInfo.setLayoutData(gdLblAutomatedInfo);
        lblAutomatedInfo.setText("An authentication file with credentials for an Azure Active Directory service" +
                " principal will be used for automated sign ins.");

        lblAuthenticationFile = new Label(compositeAutomated, SWT.NONE);
        lblAuthenticationFile.setEnabled(false);
        GridData gdLblAuthenticationFile = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdLblAuthenticationFile.horizontalIndent = 10;
        lblAuthenticationFile.setLayoutData(gdLblAuthenticationFile);
        lblAuthenticationFile.setText("Authentication file:");

        textAuthenticationFilePath = new Text(compositeAutomated, SWT.BORDER | SWT.READ_ONLY);
        textAuthenticationFilePath.setEnabled(false);
        GridData gdTextAuthenticationFilePath = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gdTextAuthenticationFilePath.horizontalIndent = 10;
        textAuthenticationFilePath.setLayoutData(gdTextAuthenticationFilePath);

        btnBrowse = new Button(compositeAutomated, SWT.NONE);
        btnBrowse.setEnabled(false);
        btnBrowse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doSelectCredFilepath();
            }
        });
        btnBrowse.setText("Browse...");
        new Label(compositeAutomated, SWT.NONE);
        new Label(compositeAutomated, SWT.NONE);

        btnCreateAuthenticationFile = new Button(compositeAutomated, SWT.NONE);
        btnCreateAuthenticationFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doCreateServicePrincipal();
            }
        });
        btnCreateAuthenticationFile.setEnabled(false);
        btnCreateAuthenticationFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnCreateAuthenticationFile.setText("New...");

        fileDialog = new FileDialog(btnBrowse.getShell(), SWT.OPEN);
        fileDialog.setText("Select Authentication File");
        fileDialog.setFilterPath(System.getProperty("user.home"));
        fileDialog.setFilterExtensions(new String[]{"*.azureauth", "*.*"});

        return area;
    }

    private void enableAutomatedAuthControls(boolean enabled) {
        setErrorMessage(null);
        lblDeviceInfo.setEnabled(!enabled);
        lblAutomatedInfo.setEnabled(enabled);
        lblAuthenticationFile.setEnabled(enabled);
        lblAuthenticationFile.setEnabled(enabled);
        textAuthenticationFilePath.setEnabled(enabled);
        btnBrowse.setEnabled(enabled);
        btnCreateAuthenticationFile.setEnabled(enabled);
    }

    @Override
    public void okPressed() {
        AuthMethodDetails authMethodDetailsResult = new AuthMethodDetails();
        if (rbtnDevice.getSelection()) {
            doSignIn();
            if (StringUtils.isNullOrEmpty(accountEmail)) {
                System.out.println("Canceled by the user.");
                return;
            }
            authMethodDetailsResult.setAuthMethod(AuthMethod.DC);
            authMethodDetailsResult.setAccountEmail(accountEmail);
        } else { // automated
            String authPath = textAuthenticationFilePath.getText();
            EventUtil.logEvent(EventType.info, ACCOUNT, SIGNIN, signInSPProp, null);
            if (StringUtils.isNullOrWhiteSpace(authPath)) {
                this.setErrorMessage("Select authentication file");
                return;
            }

            authMethodDetailsResult.setAuthMethod(AuthMethod.SP);
            // TODO: check the file is valid
            authMethodDetailsResult.setCredFilePath(authPath);
        }

        this.authMethodDetails = authMethodDetailsResult;

        super.okPressed();
    }

    private void doSelectCredFilepath() {
        setErrorMessage(null);
        String path = fileDialog.open();
        if (path == null) {
            return;
        }
        textAuthenticationFilePath.setText(path);
    }

    private AuthMethodManager getAuthMethodManager() {
        return AuthMethodManager.getInstance();
    }

    @Nullable
    private synchronized BaseADAuthManager doSignIn() {
        try {
            final BaseADAuthManager dcAuthManager = AuthMethod.DC.getAdAuthManager();

            if (dcAuthManager.isSignedIn()) {
                doSignOut();
            }
            signInAsync(dcAuthManager);
            accountEmail = dcAuthManager.getAccountEmail();

            return dcAuthManager;
        } catch (Exception ex) {
            System.out.println("doSignIn@SingInDialog: " + ex.getMessage());
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "doSignIn@SingInDialog", ex));
        }

        return null;
    }

    private void signInAsync(final BaseADAuthManager dcAuthManager) throws InvocationTargetException, InterruptedException {
        Operation operation = TelemetryManager.createOperation(ACCOUNT, SIGNIN);
        IRunnableWithProgress op = (monitor) -> {
            operation.start();
            monitor.beginTask("Signing In...", IProgressMonitor.UNKNOWN);
            try {
                EventUtil.logEvent(EventType.info, operation, signInDCProp, null);
                dcAuthManager.signIn(null);
            } catch (AuthCanceledException ex) {
                EventUtil.logError(operation, ErrorType.userError, ex, signInDCProp, null);
                System.out.println(ex.getMessage());
            } catch (Exception ex) {
                EventUtil.logError(operation, ErrorType.userError, ex, signInDCProp, null);
                System.out.println("run@ProgressDialog@signInAsync@SingInDialog: " + ex.getMessage());
                Display.getDefault().asyncExec(() -> ErrorWindow.go(getShell(), ex.getMessage(), "Sign In Error"));
            } finally {
                operation.complete();
            }
        };
        new ProgressMonitorDialog(this.getShell()).run(true, false, op);
    }

    private void doSignOut() {
        accountEmail = null;
        // AuthMethod.AD is deprecated.
        AuthMethod.DC.getAdAuthManager().signOut();
    }

    private void doCreateServicePrincipal() {
        setErrorMessage(null);
        BaseADAuthManager dcAuthManager = null;
        try {
            if (getAuthMethodManager().isSignedIn()) {
                getAuthMethodManager().signOut();
            }

            dcAuthManager = doSignIn();
            if (dcAuthManager == null || !dcAuthManager.isSignedIn()) {
                // canceled by the user
                System.out.println(">> Canceled by the user");
                return;
            }

            AccessTokenAzureManager accessTokenAzureManager = new AccessTokenAzureManager(dcAuthManager);
            SubscriptionManager subscriptionManager = accessTokenAzureManager.getSubscriptionManager();

            IRunnableWithProgress op = new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Getting Subscription List...", IProgressMonitor.UNKNOWN);
                    try {
                        subscriptionManager.getSubscriptionDetails();
                    } catch (Exception ex) {
                        System.out.println("run@ProgressDialog@doCreateServicePrincipal@SignInDialog: " + ex.getMessage());
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@doCreateServicePrincipal@SignInDialogg", ex));
                    }
                }
            };
            new ProgressMonitorDialog(this.getShell()).run(true, false, op);

            SrvPriSettingsDialog d = SrvPriSettingsDialog.go(this.getShell(), subscriptionManager.getSubscriptionDetails());
            List<SubscriptionDetail> subscriptionDetailsUpdated;
            String destinationFolder;
            if (d != null) {
                subscriptionDetailsUpdated = d.getSubscriptionDetails();
                destinationFolder = d.getDestinationFolder();
            } else {
                System.out.println(">> Canceled by the user");
                return;
            }

            Map<String, List<String>> tidSidsMap = new HashMap<>();
            for (SubscriptionDetail sd : subscriptionDetailsUpdated) {
                if (sd.isSelected()) {
                    System.out.format(">> %s\n", sd.getSubscriptionName());
                    String tid = sd.getTenantId();
                    List<String> sidList;
                    if (!tidSidsMap.containsKey(tid)) {
                        sidList = new LinkedList<>();
                    } else {
                        sidList = tidSidsMap.get(tid);
                    }
                    sidList.add(sd.getSubscriptionId());
                    tidSidsMap.put(tid, sidList);
                }
            }

            SrvPriCreationStatusDialog d1 = SrvPriCreationStatusDialog
                    .go(accessTokenAzureManager, this.getShell(), tidSidsMap, destinationFolder);
            if (d1 == null) {
                System.out.println(">> Canceled by the user");
                return;
            }

            String path = d1.getSelectedAuthFilePath();
            if (path == null) {
                System.out.println(">> No file was created");
                return;
            }

            textAuthenticationFilePath.setText(path);
            fileDialog.setFilterPath(destinationFolder);

        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "doCreateServicePrincipal@SignInDialog", ex));
        } finally {
            if (dcAuthManager != null) {
                dcAuthManager.signOut();
            }
        }
    }
}
