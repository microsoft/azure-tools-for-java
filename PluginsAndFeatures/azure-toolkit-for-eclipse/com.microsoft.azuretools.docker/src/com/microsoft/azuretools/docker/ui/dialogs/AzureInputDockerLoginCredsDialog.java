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

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.EditableDockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.logging.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.swt.widgets.Shell;

public class AzureInputDockerLoginCredsDialog extends AzureTitleAreaDialogWrapper {
    private static final Logger log =  Logger.getLogger(AzureInputDockerLoginCredsDialog.class.getName());

    private Button copyFromAzureKeyButton;
    private Text dockerHostUsernameTextField;
    private Text dockerHostFirstPwdField;
    private Text dockerHostSecondPwdField;
    private Button dockerHostKeepSshRadioButton;
    private Button dockerHostImportSshRadioButton;
    private Text dockerHostImportSSHTextField;
    private Button dockerHostImportSSHBrowseButton;
    private Button dockerHostAutoSshRadioButton;
    private Button okButton;

    private ManagedForm managedForm;
    private ScrolledForm errMsgForm;
    private IMessageManager errDispatcher;

    private boolean resetCredentials;
    private IProject project;
    private AzureDockerHostsManager dockerManager;
    private EditableDockerHost editableDockerHost;

    /**
     * Create the dialog.
     * @param parentShell
     */
    public AzureInputDockerLoginCredsDialog(Shell parentShell, IProject project, EditableDockerHost editableDockerHost, AzureDockerHostsManager dockerManager, boolean resetCredentials) {
        super(parentShell);
        setHelpAvailable(false);

        this.project = project;
        this.editableDockerHost = editableDockerHost;
        this.dockerManager = dockerManager;
        this.resetCredentials = resetCredentials;
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite mainContainer = new Composite(area, SWT.NONE);
        mainContainer.setLayout(new GridLayout(4, false));
        mainContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        copyFromAzureKeyButton = new Button(mainContainer, SWT.NONE);
        GridData gd_copyFromAzureKeyButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        gd_copyFromAzureKeyButton.horizontalIndent = 3;
        copyFromAzureKeyButton.setLayoutData(gd_copyFromAzureKeyButton);
        copyFromAzureKeyButton.setText("Copy from Azure Key Vault...");
        new Label(mainContainer, SWT.NONE);

        Label separatorCopyKeyVaultLabel = new Label(mainContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gd_separatorCopyKeyVaultLabel = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
        gd_separatorCopyKeyVaultLabel.widthHint = 199;
        separatorCopyKeyVaultLabel.setLayoutData(gd_separatorCopyKeyVaultLabel);
        new Label(mainContainer, SWT.NONE);

        Label lblUsername = new Label(mainContainer, SWT.NONE);
        GridData gd_lblUsername = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblUsername.horizontalIndent = 5;
        lblUsername.setLayoutData(gd_lblUsername);
        lblUsername.setText("Username:");

        dockerHostUsernameTextField = new Text(mainContainer, SWT.BORDER);
        dockerHostUsernameTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(mainContainer, SWT.NONE);

        Label lblPassword = new Label(mainContainer, SWT.NONE);
        GridData gd_lblPassword = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblPassword.horizontalIndent = 5;
        lblPassword.setLayoutData(gd_lblPassword);
        lblPassword.setText("Password:");

        dockerHostFirstPwdField = new Text(mainContainer, SWT.BORDER | SWT.PASSWORD);
        dockerHostFirstPwdField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(mainContainer, SWT.NONE);

        Label lblConfirm = new Label(mainContainer, SWT.NONE);
        GridData gd_lblConfirm = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblConfirm.horizontalIndent = 5;
        lblConfirm.setLayoutData(gd_lblConfirm);
        lblConfirm.setText("Confirm:");

        dockerHostSecondPwdField = new Text(mainContainer, SWT.BORDER | SWT.PASSWORD);
        dockerHostSecondPwdField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(mainContainer, SWT.NONE);
        new Label(mainContainer, SWT.NONE);
        new Label(mainContainer, SWT.NONE);
        new Label(mainContainer, SWT.NONE);
        new Label(mainContainer, SWT.NONE);

        Label lblSsh_1 = new Label(mainContainer, SWT.NONE);
        GridData gd_lblSsh_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblSsh_1.horizontalIndent = 3;
        lblSsh_1.setLayoutData(gd_lblSsh_1);
        lblSsh_1.setText("SSH");

        Label lblSsh = new Label(mainContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
        lblSsh.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        lblSsh.setText("SSH");
        new Label(mainContainer, SWT.NONE);

        dockerHostKeepSshRadioButton = new Button(mainContainer, SWT.RADIO);
        GridData gd_dockerHostKeepSshRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        gd_dockerHostKeepSshRadioButton.horizontalIndent = 5;
        dockerHostKeepSshRadioButton.setLayoutData(gd_dockerHostKeepSshRadioButton);
        dockerHostKeepSshRadioButton.setText("Use current keys");
        new Label(mainContainer, SWT.NONE);

        dockerHostImportSshRadioButton = new Button(mainContainer, SWT.RADIO);
        GridData gd_dockerHostImportSshRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        gd_dockerHostImportSshRadioButton.horizontalIndent = 5;
        dockerHostImportSshRadioButton.setLayoutData(gd_dockerHostImportSshRadioButton);
        dockerHostImportSshRadioButton.setText("Import from directory:");
        new Label(mainContainer, SWT.NONE);
        new Label(mainContainer, SWT.NONE);

        dockerHostImportSSHTextField = new Text(mainContainer, SWT.BORDER);
        dockerHostImportSSHTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        dockerHostImportSSHBrowseButton = new Button(mainContainer, SWT.NONE);
        dockerHostImportSSHBrowseButton.setText("Browse...");

        dockerHostAutoSshRadioButton = new Button(mainContainer, SWT.RADIO);
        GridData gd_dockerHostAutoSshRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        gd_dockerHostAutoSshRadioButton.horizontalIndent = 5;
        dockerHostAutoSshRadioButton.setLayoutData(gd_dockerHostAutoSshRadioButton);
        dockerHostAutoSshRadioButton.setText("Regenerate keys");
        new Label(mainContainer, SWT.NONE);

        FormToolkit toolkit = new FormToolkit(mainContainer.getDisplay());
        toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
        managedForm = new ManagedForm(mainContainer);
        errMsgForm = managedForm.getForm();
        errMsgForm.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        errMsgForm.setBackground(mainContainer.getBackground());
        errDispatcher = managedForm.getMessageManager();

        initUIComponents(mainContainer);


        return area;
    }

    private void initUIComponents(Composite mainContainer) {
        setTitle("Docker Host Log In Credentials");
        if (resetCredentials) {
            setMessage(String.format("Update %s with new log in credentials", editableDockerHost.originalDockerHost.name), IMessageProvider.INFORMATION);
        } else {
            setMessage(String.format("Docker host %s log in credentials not found; enter your log in credentials", editableDockerHost.originalDockerHost.name), IMessageProvider.INFORMATION);
        }

        copyFromAzureKeyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                AzureSelectKeyVault azureSelectKeyVaultDialog = new AzureSelectKeyVault(mainContainer.getShell(), dockerManager);
                if (azureSelectKeyVaultDialog.open() == Window.OK && azureSelectKeyVaultDialog.getSelectedKeyvault() != null) {
                    updateUIWithKeyvault(azureSelectKeyVaultDialog.getSelectedKeyvault());
                }
                okButton.setEnabled(doValidate());
            }
        });

        dockerHostUsernameTextField.setText((editableDockerHost.originalDockerHost.certVault != null && editableDockerHost.originalDockerHost.certVault.vmUsername != null) ?
                editableDockerHost.originalDockerHost.certVault.vmUsername : "");
        dockerHostUsernameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostUserNameTip());
        dockerHostUsernameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (!resetCredentials || AzureDockerValidationUtils.validateDockerHostUserName(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerHostUsernameTextField", dockerHostUsernameTextField);
                    setErrorMessage(null);
                    okButton.setEnabled(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostUsernameTextField", AzureDockerValidationUtils.getDockerHostUserNameTip(), null, IMessageProvider.ERROR, dockerHostUsernameTextField);
                    setErrorMessage("Invalid user name");
                    okButton.setEnabled(false);
                }
            }
        });

        dockerHostFirstPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());
        dockerHostFirstPwdField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = ((Text) event.getSource()).getText();
                if (text == null || text.isEmpty() || (!resetCredentials || AzureDockerValidationUtils.validateDockerHostPassword(text))) {
                    errDispatcher.removeMessage("dockerHostFirstPwdField", dockerHostFirstPwdField);
                    setErrorMessage(null);
                    if (!resetCredentials) {
                        dockerHostSecondPwdField.setText(text);
                    }
                    okButton.setEnabled(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostFirstPwdField", AzureDockerValidationUtils.getDockerHostPasswordTip(), null, IMessageProvider.ERROR, dockerHostFirstPwdField);
                    setErrorMessage("Invalid password");
                    okButton.setEnabled(false);
                }
            }
        });

        dockerHostSecondPwdField.setVisible(resetCredentials);
        dockerHostSecondPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());
        dockerHostSecondPwdField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String pwd1 = dockerHostFirstPwdField.getText();
                String pwd2 = ((Text) event.getSource()).getText();
                if ((pwd1 == null && pwd2 == null) || pwd2.equals(pwd1)) {
                    errDispatcher.removeMessage("dockerHostSecondPwdField", dockerHostSecondPwdField);
                    setErrorMessage(null);
                    okButton.setEnabled(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostSecondPwdField", AzureDockerValidationUtils.getDockerHostPasswordTip(), null, IMessageProvider.ERROR, dockerHostSecondPwdField);
                    setErrorMessage("Invalid confirmation password");
                    okButton.setEnabled(false);
                }
            }
        });

        dockerHostKeepSshRadioButton.setSelection(true);
        dockerHostKeepSshRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                dockerHostImportSSHTextField.setEnabled(false);
                dockerHostImportSSHBrowseButton.setEnabled(false);
                errDispatcher.removeMessage("dockerHostImportSSHTextField", dockerHostImportSSHTextField);
                setErrorMessage(null);
                if (editableDockerHost.originalDockerHost.hasSSHLogIn) {
                    AzureDockerCertVaultOps.copyVaultSshKeys(editableDockerHost.updatedDockerHost.certVault, editableDockerHost.originalDockerHost.certVault);
                }
                editableDockerHost.updatedDockerHost.hasSSHLogIn = editableDockerHost.originalDockerHost.hasSSHLogIn;
                okButton.setEnabled(doValidate());
            }
        });

        dockerHostImportSshRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                dockerHostImportSSHTextField.setEnabled(true);
                dockerHostImportSSHBrowseButton.setEnabled(true);
                okButton.setEnabled(doValidate());
            }
        });

        dockerHostImportSSHTextField.setEnabled(false);
        dockerHostImportSSHTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostSshDirectoryTip());
        dockerHostImportSSHTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerHostSshDirectory(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerHostImportSSHTextField", dockerHostImportSSHTextField);
                    setErrorMessage(null);
                    okButton.setEnabled(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostImportSSHTextField", AzureDockerValidationUtils.getDockerHostSshDirectoryTip(), null, IMessageProvider.ERROR, dockerHostImportSSHTextField);
                    setErrorMessage("SSH key files not found in the specified directory");
                    okButton.setEnabled(false);
                }
            }
        });

        dockerHostImportSSHBrowseButton.setEnabled(false);
        dockerHostImportSSHBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog directoryDialog = new DirectoryDialog(dockerHostImportSSHBrowseButton.getShell());
                directoryDialog.setText("Select SSH Keys Directory");
                directoryDialog.setFilterPath(System.getProperty("user.home"));
                String path = directoryDialog.open();
                if (path == null) {
                    return;
                }
                dockerHostImportSSHTextField.setText(path);
                okButton.setEnabled(doValidate());
            }
        });

        dockerHostAutoSshRadioButton.setVisible(resetCredentials);
        dockerHostAutoSshRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                dockerHostImportSSHTextField.setEnabled(false);
                dockerHostImportSSHBrowseButton.setEnabled(false);
                errDispatcher.removeMessage("dockerHostImportSSHTextField", dockerHostImportSSHTextField);
                setErrorMessage(null);
                AzureDockerCertVault certVault = AzureDockerCertVaultOps.generateSSHKeys(null, "SSH keys for " + editableDockerHost.updatedDockerHost.name);
                AzureDockerCertVaultOps.copyVaultSshKeys(editableDockerHost.updatedDockerHost.certVault, certVault);
                editableDockerHost.updatedDockerHost.hasSSHLogIn = true;
                okButton.setEnabled(doValidate());
            }
        });

    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        okButton = createButton(parent, IDialogConstants.OK_ID, resetCredentials ? "Update" : "OK", true);
        okButton.setEnabled(false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        if (doValidate()) {
            if (editableDockerHost.originalDockerHost.hasKeyVault &&
                    !DefaultLoader.getUIHelper().showConfirmation( String.format("We've detected that the selected host's login credentials are currently loaded from an Azure Key Vault. Reseting them will remove this association and will require to enter the credentials manually.\n\n Do you want to proceed with this update?"),
                            "Removing Key Vault Association", new String[] { "Yes", "No" }, null)) {
                return;
            }
            super.okPressed();
        } else {
            okButton.setEnabled(false);
        }
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(450, 450);
    }

    public boolean doValidate() {
        String vmUsername = dockerHostUsernameTextField.getText();
        if (vmUsername == null || vmUsername.isEmpty() || (resetCredentials && !AzureDockerValidationUtils.validateDockerHostUserName(vmUsername))) {
            errDispatcher.addMessage("dockerHostUsernameTextField", AzureDockerValidationUtils.getDockerHostUserNameTip(), null, IMessageProvider.ERROR, dockerHostUsernameTextField);
            setErrorMessage("Invalid user name");
            return false;
        } else {
            errDispatcher.removeMessage("dockerHostUsernameTextField", dockerHostUsernameTextField);
            setErrorMessage(null);
            editableDockerHost.updatedDockerHost.certVault.vmUsername = vmUsername;
        }

        // Password login
        String vmPwd1 = dockerHostFirstPwdField.getText();
        String vmPwd2 = dockerHostSecondPwdField.getText();
        if (((dockerHostKeepSshRadioButton.getSelection() && !editableDockerHost.originalDockerHost.hasSSHLogIn) ||
                (vmPwd1 != null && !vmPwd1.isEmpty()) || (vmPwd2 != null && !vmPwd2.isEmpty())) &&
                (vmPwd1.isEmpty() || vmPwd2.isEmpty() || !vmPwd1.equals(vmPwd2) || (resetCredentials && !AzureDockerValidationUtils.validateDockerHostPassword(vmPwd1)))) {
            errDispatcher.addMessage("dockerHostFirstPwdField", AzureDockerValidationUtils.getDockerHostPasswordTip(), null, IMessageProvider.ERROR, dockerHostFirstPwdField);
            setErrorMessage("Invalid password");
            return false;
        } else {
            errDispatcher.removeMessage("dockerHostFirstPwdField", dockerHostFirstPwdField);
            errDispatcher.removeMessage("dockerHostSecondPwdField", dockerHostSecondPwdField);
            setErrorMessage(null);
            if (vmPwd1 == null || vmPwd1.isEmpty()) {
                editableDockerHost.updatedDockerHost.certVault.vmPwd = null;
                editableDockerHost.updatedDockerHost.hasPwdLogIn = false;
            } else {
                editableDockerHost.updatedDockerHost.certVault.vmPwd = vmPwd1;
                editableDockerHost.updatedDockerHost.hasPwdLogIn = true;
            }
        }

        // SSH key imported from local file directory
        if (dockerHostImportSshRadioButton.getSelection()) {
            String sshPath = dockerHostImportSSHTextField.getText();
            if (sshPath == null || sshPath.isEmpty() || !AzureDockerValidationUtils.validateDockerHostSshDirectory(sshPath)) {
                errDispatcher.addMessage("dockerHostImportSSHTextField", AzureDockerValidationUtils.getDockerHostPasswordTip(), null, IMessageProvider.ERROR, dockerHostImportSSHTextField);
                setErrorMessage("SSH key files not found in the specified directory");
                return false;
            } else {
                try {
                    AzureDockerCertVault certVault = AzureDockerCertVaultOps.getSSHKeysFromLocalFile(sshPath);
                    AzureDockerCertVaultOps.copyVaultSshKeys(editableDockerHost.updatedDockerHost.certVault, certVault);
                    editableDockerHost.updatedDockerHost.hasSSHLogIn = true;
                } catch (Exception e) {
                    errDispatcher.addMessage("dockerHostImportSSHTextField", AzureDockerValidationUtils.getDockerHostPasswordTip(), null, IMessageProvider.ERROR, dockerHostImportSSHTextField);
                    setErrorMessage("Unexpected error reading SSH key files from specified directory: " + e.getMessage());
                    return false;
                }
                errDispatcher.removeMessage("dockerHostImportSSHTextField", dockerHostImportSSHTextField);
                setErrorMessage(null);
            }
        } else {
            errDispatcher.removeMessage("dockerHostImportSSHTextField", dockerHostImportSSHTextField);
            setErrorMessage(null);
        }

        return true;
    }

    private void updateUIWithKeyvault(String keyvault) {
        AzureDockerCertVault certVault = dockerManager.getDockerVault(keyvault);
        if (certVault != null) {
            editableDockerHost.updatedDockerHost.certVault = certVault;
            dockerHostUsernameTextField.setText((certVault.vmUsername != null) ? certVault.vmUsername : "");
            dockerHostFirstPwdField.setText((certVault.vmPwd != null) ? certVault.vmPwd : "");
            dockerHostSecondPwdField.setText((certVault.vmPwd != null) ? certVault.vmPwd : "");
        }
    }
}
