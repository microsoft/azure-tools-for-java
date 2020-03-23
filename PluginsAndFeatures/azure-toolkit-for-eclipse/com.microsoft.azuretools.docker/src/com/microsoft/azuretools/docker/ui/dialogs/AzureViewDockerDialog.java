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

package com.microsoft.azuretools.docker.ui.dialogs;

import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.model.DockerHost.DockerHostVMState;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;
import com.microsoft.azuretools.core.utils.PluginUtil;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class AzureViewDockerDialog extends AzureDialogWrapper {
    private static final Logger log =  Logger.getLogger(AzureViewDockerDialog.class.getName());
    public static final int OK_EXIT_CODE = 0;
    public static final int CANCEL_EXIT_CODE = 1;
    public static final int CLOSE_EXIT_CODE = 1;
    public static final int UPDATE_EXIT_CODE = 3;

    private Text dockerHostNameTextField;
    private Text dockerHostUrlTextField;
    private Text dockerHostSidTextField;
    private Text dockerHostLocationTextField;
    private Text dockerHostStatusTextField;
    private Text dockerHostOSTypeTextField;
    private Text dockerHostVMSizeTextField;
    private Text dockerHostRGNameTextField;
    private Text dockerHostVnetNameAddrTextField;
    private Text dockerHostSubnetNameAddrTextField;
    private Text dockerHostPublicIpTextField;
    private Text dockerHostPrivateIpTextField;
    private Text dockerHostStorageNameTypeTextField;
    private Text dockerHostUsernameTextField;
    private Text dockerHostPwdLoginTextField;
    private Text dockerHostSshLoginTextField;
    private Text dockerHostTlsAuthTextField;
    private Text dockerHostPortTextField;
    private Text dockerHostKeyvaultTextField;

    private IProject project;
    private DockerHost dockerHost;
    private AzureDockerHostsManager dockerManager;
    private int exitCode;

    /**
     * Create the dialog.
     * @param parentShell
     */
    public AzureViewDockerDialog(Shell parentShell, IProject project, DockerHost host, AzureDockerHostsManager dockerManager) {
        super(parentShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MIN | SWT.RESIZE);

        this.project = project;
        this.dockerHost = host;
        this.dockerManager = dockerManager;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(String.format("Viewing %s", dockerHost.name));
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite mainContainer = (Composite) super.createDialogArea(parent);
        mainContainer.setLayout(new GridLayout(2, false));

        Label lblNewLabel = new Label(mainContainer, SWT.NONE);
        GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblNewLabel.verticalIndent = 5;
        gd_lblNewLabel.horizontalIndent = 5;
        lblNewLabel.setLayoutData(gd_lblNewLabel);
        lblNewLabel.setText("Host name:");

        dockerHostNameTextField = new Text(mainContainer, SWT.READ_ONLY);
        GridData gd_dockerHostNameTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_dockerHostNameTextField.verticalIndent = 5;
        dockerHostNameTextField.setLayoutData(gd_dockerHostNameTextField);
        dockerHostNameTextField.setBackground(mainContainer.getBackground());

        Label lblUrl = new Label(mainContainer, SWT.NONE);
        GridData gd_lblUrl = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblUrl.horizontalIndent = 5;
        lblUrl.setLayoutData(gd_lblUrl);
        lblUrl.setText("URL:");

        dockerHostUrlTextField = new Text(mainContainer, SWT.READ_ONLY);
        dockerHostUrlTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dockerHostUrlTextField.setBackground(mainContainer.getBackground());

        Label lblSubscription = new Label(mainContainer, SWT.NONE);
        GridData gd_lblSubscription = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblSubscription.horizontalIndent = 5;
        lblSubscription.setLayoutData(gd_lblSubscription);
        lblSubscription.setText("Subscription:");

        dockerHostSidTextField = new Text(mainContainer, SWT.READ_ONLY);
        dockerHostSidTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dockerHostSidTextField.setBackground(mainContainer.getBackground());

        Label lblLocation = new Label(mainContainer, SWT.NONE);
        GridData gd_lblLocation = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblLocation.horizontalIndent = 5;
        lblLocation.setLayoutData(gd_lblLocation);
        lblLocation.setText("Location:");

        dockerHostLocationTextField = new Text(mainContainer, SWT.READ_ONLY);
        dockerHostLocationTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dockerHostLocationTextField.setBackground(mainContainer.getBackground());

        Label lblStatus = new Label(mainContainer, SWT.NONE);
        GridData gd_lblStatus = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblStatus.horizontalIndent = 5;
        lblStatus.setLayoutData(gd_lblStatus);
        lblStatus.setText("Status:");

        dockerHostStatusTextField = new Text(mainContainer, SWT.READ_ONLY);
        dockerHostStatusTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dockerHostStatusTextField.setBackground(mainContainer.getBackground());

        TabFolder tabFolder = new TabFolder(mainContainer, SWT.NONE);
        GridData gd_tabFolder = new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1);
        gd_tabFolder.heightHint = 320;
        gd_tabFolder.widthHint = 575;
        tabFolder.setLayoutData(gd_tabFolder);

        TabItem tbtmVmSettings = new TabItem(tabFolder, SWT.NONE);
        tbtmVmSettings.setText("Login Settings");

        Composite loginSetsComposite = new Composite(tabFolder, SWT.NONE);
        tbtmVmSettings.setControl(loginSetsComposite);
        loginSetsComposite.setLayout(new GridLayout(4, false));

        Label lblAuthentication = new Label(loginSetsComposite, SWT.NONE);
        GridData gd_lblAuthentication = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAuthentication.verticalIndent = 5;
        gd_lblAuthentication.horizontalIndent = 3;
        lblAuthentication.setLayoutData(gd_lblAuthentication);
        lblAuthentication.setText("Authentication");
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);

        Link dockerHostAuthUpdateHyperlink = new Link(loginSetsComposite, SWT.NONE);
        dockerHostAuthUpdateHyperlink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                exitCode = UPDATE_EXIT_CODE;
                okPressed();
            }
        });
        dockerHostAuthUpdateHyperlink.setText("<a>Update...</a>");

        Label lblUsername = new Label(loginSetsComposite, SWT.NONE);
        GridData gd_lblUsername = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblUsername.horizontalIndent = 20;
        lblUsername.setLayoutData(gd_lblUsername);
        lblUsername.setText("Username:");

        dockerHostUsernameTextField = new Text(loginSetsComposite, SWT.READ_ONLY);
        dockerHostUsernameTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);

        Label lblLogInUsing = new Label(loginSetsComposite, SWT.NONE);
        GridData gd_lblLogInUsing = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblLogInUsing.horizontalIndent = 20;
        lblLogInUsing.setLayoutData(gd_lblLogInUsing);
        lblLogInUsing.setText("Log in using a password:");

        dockerHostPwdLoginTextField = new Text(loginSetsComposite, SWT.READ_ONLY);
        dockerHostPwdLoginTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);

        Label lblLogInUsing_1 = new Label(loginSetsComposite, SWT.NONE);
        GridData gd_lblLogInUsing_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblLogInUsing_1.horizontalIndent = 20;
        lblLogInUsing_1.setLayoutData(gd_lblLogInUsing_1);
        lblLogInUsing_1.setText("Log in using SSH keys:");

        dockerHostSshLoginTextField = new Text(loginSetsComposite, SWT.READ_ONLY);
        dockerHostSshLoginTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(loginSetsComposite, SWT.NONE);

        Link dockerHostSshExportHyperlink = new Link(loginSetsComposite, SWT.NONE);
        dockerHostSshExportHyperlink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (dockerHost.hasSSHLogIn && dockerHost.certVault != null) {
                    Display display = Display.getDefault();
                    Shell dialogShell = new Shell(display, SWT.APPLICATION_MODAL);
                    dialogShell = mainContainer.getShell();
                    AzureExportDockerSshKeysDialog exportDockerSshKeysDialog = new AzureExportDockerSshKeysDialog(dialogShell, project);
                    if (exportDockerSshKeysDialog.open() == Window.OK) {
                        try {
                            AzureDockerCertVaultOps.saveSshKeysToLocalFiles(exportDockerSshKeysDialog.getPath(), dockerHost.certVault);
                        } catch (Exception ex) {
                            String msg = "An error occurred while attempting to export the SSh keys.\n" + ex.getMessage();
                            PluginUtil.displayErrorDialogAndLog(mainContainer.getShell(), "Error", msg, ex);
                        }
                    }
                }
            }
        });
        dockerHostSshExportHyperlink.setText("<a>Export SSH keys...</a>");
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);

        Label lblDockerDaemon = new Label(loginSetsComposite, SWT.NONE);
        GridData gd_lblDockerDaemon = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblDockerDaemon.horizontalIndent = 3;
        lblDockerDaemon.setLayoutData(gd_lblDockerDaemon);
        lblDockerDaemon.setText("Docker Daemon");
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);

        Label lblAuthentication_1 = new Label(loginSetsComposite, SWT.NONE);
        GridData gd_lblAuthentication_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAuthentication_1.horizontalIndent = 20;
        lblAuthentication_1.setLayoutData(gd_lblAuthentication_1);
        lblAuthentication_1.setText("Authentication:");

        dockerHostTlsAuthTextField = new Text(loginSetsComposite, SWT.READ_ONLY);
        dockerHostTlsAuthTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(loginSetsComposite, SWT.NONE);

        Link dockerHostTlsExportHyperlink = new Link(loginSetsComposite, SWT.NONE);
        dockerHostTlsExportHyperlink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (dockerHost.isTLSSecured && dockerHost.certVault != null) {
                    AzureExportDockerTlsKeysDialog exportDockerTlsKeysDialog = new AzureExportDockerTlsKeysDialog(mainContainer.getShell(), project);
                    if (exportDockerTlsKeysDialog.open() == Window.OK) {
                        try {
                            AzureDockerCertVaultOps.saveTlsCertsToLocalFiles(exportDockerTlsKeysDialog.getPath(), dockerHost.certVault);
                        } catch (Exception ex) {
                            String msg = "An error occurred while attempting to export the TLS keys.\n" + ex.getMessage();
                            PluginUtil.displayErrorDialogAndLog(mainContainer.getShell(), "Error", msg, ex);
                        }
                    }
                }
            }
        });
        dockerHostTlsExportHyperlink.setText("<a>Export TLS certs...</a>");

        Label lblPortSettings = new Label(loginSetsComposite, SWT.NONE);
        GridData gd_lblPortSettings = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblPortSettings.horizontalIndent = 20;
        lblPortSettings.setLayoutData(gd_lblPortSettings);
        lblPortSettings.setText("Port settings:");

        dockerHostPortTextField = new Text(loginSetsComposite, SWT.NONE);
        dockerHostPortTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);

        Label lblKeyVaultSettings = new Label(loginSetsComposite, SWT.NONE);
        GridData gd_lblKeyVaultSettings = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblKeyVaultSettings.horizontalIndent = 3;
        lblKeyVaultSettings.setLayoutData(gd_lblKeyVaultSettings);
        lblKeyVaultSettings.setText("Key Vault Settings");
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);
        new Label(loginSetsComposite, SWT.NONE);

        Label lblKeyVaultUrl = new Label(loginSetsComposite, SWT.NONE);
        GridData gd_lblKeyVaultUrl = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblKeyVaultUrl.horizontalIndent = 20;
        lblKeyVaultUrl.setLayoutData(gd_lblKeyVaultUrl);
        lblKeyVaultUrl.setText("Key Vault Url:");

        dockerHostKeyvaultTextField = new Text(loginSetsComposite, SWT.READ_ONLY);
        dockerHostKeyvaultTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        new Label(loginSetsComposite, SWT.NONE);

        Label dockerHostKeyvaultLabel1 = new Label(loginSetsComposite, SWT.WRAP);
        dockerHostKeyvaultLabel1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        dockerHostKeyvaultLabel1.setText("Go to Azure Portal and change the Key Vault permissons in order to");
        new Label(loginSetsComposite, SWT.NONE);

        Label dockerHostKeyvaultLabel2 = new Label(loginSetsComposite, SWT.NONE);
        dockerHostKeyvaultLabel2.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        dockerHostKeyvaultLabel2.setText(" access it from your current sign-in credentials!");

        TabItem tbtmLoginSettings = new TabItem(tabFolder, SWT.NONE);
        tbtmLoginSettings.setText("VM Settings");

        Composite vmSetsComposite = new Composite(tabFolder, SWT.NONE);
        tbtmLoginSettings.setControl(vmSetsComposite);
        vmSetsComposite.setLayout(new GridLayout(2, false));

        Label lblHostOsType = new Label(vmSetsComposite, SWT.NONE);
        GridData gd_lblHostOsType = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblHostOsType.verticalIndent = 5;
        gd_lblHostOsType.horizontalIndent = 3;
        lblHostOsType.setLayoutData(gd_lblHostOsType);
        lblHostOsType.setText("Host OS type:");

        dockerHostOSTypeTextField = new Text(vmSetsComposite, SWT.READ_ONLY);
        GridData gd_dockerHostOSTypeTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_dockerHostOSTypeTextField.verticalIndent = 5;
        dockerHostOSTypeTextField.setLayoutData(gd_dockerHostOSTypeTextField);

        Label lblVmSize = new Label(vmSetsComposite, SWT.NONE);
        GridData gd_lblVmSize = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblVmSize.horizontalIndent = 3;
        lblVmSize.setLayoutData(gd_lblVmSize);
        lblVmSize.setText("VM size:");

        dockerHostVMSizeTextField = new Text(vmSetsComposite, SWT.READ_ONLY);
        dockerHostVMSizeTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblResourceGroup = new Label(vmSetsComposite, SWT.NONE);
        GridData gd_lblResourceGroup = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblResourceGroup.horizontalIndent = 3;
        lblResourceGroup.setLayoutData(gd_lblResourceGroup);
        lblResourceGroup.setText("Resource group:");

        dockerHostRGNameTextField = new Text(vmSetsComposite, SWT.READ_ONLY);
        dockerHostRGNameTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblNetwork = new Label(vmSetsComposite, SWT.NONE);
        GridData gd_lblNetwork = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblNetwork.horizontalIndent = 3;
        lblNetwork.setLayoutData(gd_lblNetwork);
        lblNetwork.setText("Network:");

        dockerHostVnetNameAddrTextField = new Text(vmSetsComposite, SWT.READ_ONLY);
        dockerHostVnetNameAddrTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblSubnet = new Label(vmSetsComposite, SWT.NONE);
        GridData gd_lblSubnet = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblSubnet.horizontalIndent = 3;
        lblSubnet.setLayoutData(gd_lblSubnet);
        lblSubnet.setText("Subnet:");

        dockerHostSubnetNameAddrTextField = new Text(vmSetsComposite, SWT.READ_ONLY);
        dockerHostSubnetNameAddrTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblPublicIp = new Label(vmSetsComposite, SWT.NONE);
        GridData gd_lblPublicIp = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblPublicIp.horizontalIndent = 3;
        lblPublicIp.setLayoutData(gd_lblPublicIp);
        lblPublicIp.setText("Public IP:");

        dockerHostPublicIpTextField = new Text(vmSetsComposite, SWT.READ_ONLY);
        dockerHostPublicIpTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblPrivateIp = new Label(vmSetsComposite, SWT.NONE);
        GridData gd_lblPrivateIp = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblPrivateIp.horizontalIndent = 3;
        lblPrivateIp.setLayoutData(gd_lblPrivateIp);
        lblPrivateIp.setText("Private IP:");

        dockerHostPrivateIpTextField = new Text(vmSetsComposite, SWT.READ_ONLY);
        dockerHostPrivateIpTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblStorageAccount = new Label(vmSetsComposite, SWT.NONE);
        GridData gd_lblStorageAccount = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblStorageAccount.horizontalIndent = 3;
        lblStorageAccount.setLayoutData(gd_lblStorageAccount);
        lblStorageAccount.setText("Storage account:");

        dockerHostStorageNameTypeTextField = new Text(vmSetsComposite, SWT.READ_ONLY);
        dockerHostStorageNameTypeTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Color backgroundColor =  mainContainer.getBackground(); //new Color(null,  mainContainer.getBackground().getRed(),  mainContainer.getBackground().getGreen(),  mainContainer.getBackground().getBlue(), 255);

        loginSetsComposite.setBackgroundMode(SWT.INHERIT_FORCE);
        loginSetsComposite.setBackground(backgroundColor);
        vmSetsComposite.setBackgroundMode(SWT.INHERIT_FORCE);
        vmSetsComposite.setBackground(backgroundColor);

        String updating = dockerHost.isUpdating ? " (updating...)" : null;
        setTextField(dockerHostNameTextField, dockerHost.name);
        dockerHostUsernameTextField.setBackground(tabFolder.getBackground());

        setTextField(dockerHostUrlTextField, dockerHost.apiUrl);
        setTextField(dockerHostSidTextField, dockerHost.sid);
        setTextField(dockerHostLocationTextField, dockerHost.hostVM.region);
        setTextField(dockerHostStatusTextField,
                (updating != null) ? dockerHost.state.toString() + updating : dockerHost.state.toString());

        // Docker VM settings
        setTextField(dockerHostOSTypeTextField, dockerHost.hostOSType.toString());
        // TODO: enable resizing of the current VM -> see
        // VirtualMachine::availableSizes() and update.withSize();
        setTextField(dockerHostVMSizeTextField,
                (updating != null) ? dockerHost.hostVM.vmSize + updating : dockerHost.hostVM.vmSize);
        setTextField(dockerHostRGNameTextField, dockerHost.hostVM.resourceGroupName);
        setTextField(dockerHostVnetNameAddrTextField,
                String.format("%s (%s)", dockerHost.hostVM.vnetName, dockerHost.hostVM.vnetAddressSpace));
        setTextField(dockerHostSubnetNameAddrTextField,
                String.format("%s (%s)", dockerHost.hostVM.subnetName, dockerHost.hostVM.subnetAddressRange));
        setTextField(dockerHostPublicIpTextField,
                String.format("%s (%s)", dockerHost.hostVM.publicIp, dockerHost.hostVM.publicIpName));
        setTextField(dockerHostPrivateIpTextField, dockerHost.hostVM.privateIp);
        setTextField(dockerHostStorageNameTypeTextField,
                String.format("%s (%s)", dockerHost.hostVM.storageAccountName, dockerHost.hostVM.storageAccountType));

        // Docker VM log in settings
        dockerHostAuthUpdateHyperlink.setEnabled(!dockerHost.isUpdating);
        String username = (dockerHost.certVault != null && dockerHost.certVault.vmUsername != null) ? dockerHost.certVault.vmUsername : "-unknown-";
        setTextField(dockerHostUsernameTextField, (updating != null) ? username + updating : username);
        setTextField(dockerHostPwdLoginTextField, (updating != null)
                        ? (dockerHost.hasPwdLogIn ? "Yes" : (dockerHost.certVault == null) ? "-unknown- " : "No") + updating
                        : (dockerHost.hasPwdLogIn ? "Yes" : (dockerHost.certVault == null) ? "-unknown- " : "No"));
        setTextField(dockerHostSshLoginTextField, (updating != null)
                        ? (dockerHost.hasSSHLogIn ? "Yes" : (dockerHost.certVault == null) ? "-unknown- " : "No") + updating
                        : (dockerHost.hasSSHLogIn ? "Yes" : (dockerHost.certVault == null) ? "-unknown- " : "No"));
        dockerHostSshExportHyperlink.setEnabled(!dockerHost.isUpdating && dockerHost.hasSSHLogIn);

        // Docker Daemon settings
        setTextField(dockerHostTlsAuthTextField, (updating != null)
                        ? (dockerHost.isTLSSecured ? "Using TLS certificates" : (dockerHost.certVault == null) ? "-unknown- " : "Open/unsecured access") + updating
                        : (dockerHost.isTLSSecured ? "Using TLS certificates" : (dockerHost.certVault == null) ? "-unknown- " : "Open/unsecured access"));
        dockerHostTlsExportHyperlink.setEnabled(!dockerHost.isUpdating && dockerHost.isTLSSecured && !dockerHost.state.equals(DockerHostVMState.TO_BE_CREATED));

        setTextField(dockerHostPortTextField, (updating != null) ? dockerHost.port + updating : dockerHost.port);

        // Docker Key Vault settings
        if (dockerHost.certVault != null && dockerHost.certVault.uri != null && !dockerHost.certVault.uri.isEmpty()) {
            setTextField(dockerHostKeyvaultTextField, (updating != null) ? dockerHost.certVault.uri + updating : dockerHost.certVault.uri);
            dockerHostKeyvaultLabel1.setVisible(false);
            dockerHostKeyvaultLabel2.setVisible(false);
        } else if (dockerHost.hostVM.vaultName != null && !dockerHost.hostVM.vaultName.isEmpty()) {
            setTextField(dockerHostKeyvaultTextField, String.format("Error reading http://%s.vault.azure.net", dockerHost.hostVM.vaultName));
            dockerHostKeyvaultLabel1.setVisible(true);
            dockerHostKeyvaultLabel2.setVisible(true);
        } else {
            setTextField(dockerHostKeyvaultTextField, "Not using Key Vault");
            dockerHostKeyvaultLabel1.setVisible(false);
            dockerHostKeyvaultLabel2.setVisible(false);
        }

        exitCode = CLOSE_EXIT_CODE;

        return mainContainer;
    }

    private void setTextField(Text textField, String text) {
        textField.setText(text != null ? text : "-unknown-");
        textField.setEditable(false);
//        textField.setBackground(AzureDockerUIResources.getColor(37)); // SWT.COLOR_TRANSPARENT
        FontDescriptor boldDescriptor = FontDescriptor.createFrom(textField.getFont()).setStyle(SWT.BOLD);
        Font boldFont = boldDescriptor.createFont(textField.getDisplay());
        textField.setFont(boldFont);
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(600, 550);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    public int getInternalExitCode() {
        return exitCode;
    }
}
