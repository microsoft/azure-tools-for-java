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

package com.microsoft.azuretools.docker.ui.wizards.createhost;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azure.docker.model.DockerHost;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.azuretools.core.Activator;

import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class AzureNewDockerLoginPage extends WizardPage {
    private static final Logger log =  Logger.getLogger(AzureNewDockerLoginPage.class.getName());

    private Button dockerHostImportKeyvaultCredsRadioButton;
    private Combo dockerHostImportKeyvaultComboBox;

    private Button dockerHostNewCredsRadioButton;
    private TabFolder credsTabfolder;
    private TabItem vmCredsTableItem;
    private Text dockerHostUsernameTextField;
    private Text dockerHostFirstPwdField;
    private Label dockerHostPwdLabel;
    private Text dockerHostSecondPwdField;
    private Button dockerHostNoSshRadioButton;
    private Button dockerHostAutoSshRadioButton;
    private Button dockerHostImportSshRadioButton;
    private Text dockerHostImportSSHTextField;
    private Button dockerHostImportSSHBrowseButton;

    private TabItem daemonCredsTableItem;
    private Button dockerHostNoTlsRadioButton;
    private Text dockerHostImportTLSTextField;
    private Button dockerHostAutoTlsRadioButton;
    private Button dockerHostImportTlsRadioButton;
    private Button dockerHostImportTLSBrowseButton;

    private Text dockerDaemonPortTextField;

    private Button dockerHostSaveCredsCheckBox;
    private Text dockerHostNewKeyvaultTextField;

    private ManagedForm managedForm;
    private ScrolledForm errMsgForm;
    private IMessageManager errDispatcher;

    private AzureNewDockerWizard wizard;
    private AzureDockerHostsManager dockerManager;
    private DockerHost newHost;
    private IProject project;

    /**
     * Create the wizard.
     */
    public AzureNewDockerLoginPage(AzureNewDockerWizard wizard) {
        super("Create Docker Host", "Configure log in credentials and port settings", Activator.getImageDescriptor("icons/large/Azure.png"));

        this.wizard = wizard;
        this.dockerManager = wizard.getDockerManager();
        this.newHost = wizard.getDockerHost();
        this.project = wizard.getProject();
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite mainContainer = new Composite(parent, SWT.NONE);
        setControl(mainContainer);
        mainContainer.setLayout(new GridLayout(2, false));

        dockerHostImportKeyvaultCredsRadioButton = new Button(mainContainer, SWT.RADIO);
        GridData gd_dockerHostImportKeyvaultCredsRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostImportKeyvaultCredsRadioButton.horizontalIndent = 5;
        dockerHostImportKeyvaultCredsRadioButton.setLayoutData(gd_dockerHostImportKeyvaultCredsRadioButton);
        dockerHostImportKeyvaultCredsRadioButton.setText("Import credentials from Azure Key Vault:");

        dockerHostImportKeyvaultComboBox = new Combo(mainContainer, SWT.READ_ONLY);
        GridData gd_dockerHostImportKeyvaultComboBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostImportKeyvaultComboBox.widthHint = 230;
        dockerHostImportKeyvaultComboBox.setLayoutData(gd_dockerHostImportKeyvaultComboBox);

        dockerHostNewCredsRadioButton = new Button(mainContainer, SWT.RADIO);
        GridData gd_dockerHostNewCredsRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostNewCredsRadioButton.horizontalIndent = 5;
        dockerHostNewCredsRadioButton.setLayoutData(gd_dockerHostNewCredsRadioButton);
        dockerHostNewCredsRadioButton.setText("New log in credentials:");
        new Label(mainContainer, SWT.NONE);

        credsTabfolder = new TabFolder(mainContainer, SWT.NONE);
        GridData gd_credsTabfolder = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 2);
        gd_credsTabfolder.heightHint = 235;
        credsTabfolder.setLayoutData(gd_credsTabfolder);

        vmCredsTableItem = new TabItem(credsTabfolder, SWT.NONE);
        vmCredsTableItem.setText("VM Credentials");

        Composite vmCredsComposite = new Composite(credsTabfolder, SWT.NONE);
        vmCredsTableItem.setControl(vmCredsComposite);
        vmCredsComposite.setLayout(new GridLayout(6, false));

        Label lblUsername = new Label(vmCredsComposite, SWT.NONE);
        GridData gd_lblUsername = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblUsername.horizontalIndent = 5;
        lblUsername.setLayoutData(gd_lblUsername);
        lblUsername.setText("Username:");

        dockerHostUsernameTextField = new Text(vmCredsComposite, SWT.BORDER);
        GridData gd_dockerHostUsernameTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostUsernameTextField.widthHint = 150;
        dockerHostUsernameTextField.setLayoutData(gd_dockerHostUsernameTextField);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);

        Label lblPassword = new Label(vmCredsComposite, SWT.NONE);
        GridData gd_lblPassword = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblPassword.horizontalIndent = 5;
        lblPassword.setLayoutData(gd_lblPassword);
        lblPassword.setText("Password:");

        dockerHostFirstPwdField = new Text(vmCredsComposite, SWT.BORDER | SWT.PASSWORD);
        GridData gd_dockerHostFirstPwdField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostFirstPwdField.widthHint = 150;
        dockerHostFirstPwdField.setLayoutData(gd_dockerHostFirstPwdField);

        dockerHostPwdLabel = new Label(vmCredsComposite, SWT.NONE);
        dockerHostPwdLabel.setText("(Optional)");
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);

        Label lblConfirm = new Label(vmCredsComposite, SWT.NONE);
        GridData gd_lblConfirm = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblConfirm.horizontalIndent = 5;
        lblConfirm.setLayoutData(gd_lblConfirm);
        lblConfirm.setText("Confirm:");

        dockerHostSecondPwdField = new Text(vmCredsComposite, SWT.BORDER | SWT.PASSWORD);
        GridData gd_dockerHostSecondPwdField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostSecondPwdField.widthHint = 150;
        dockerHostSecondPwdField.setLayoutData(gd_dockerHostSecondPwdField);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);

        Label lblSsh = new Label(vmCredsComposite, SWT.NONE);
        GridData gd_lblSsh = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblSsh.horizontalIndent = 5;
        lblSsh.setLayoutData(gd_lblSsh);
        lblSsh.setText("SSH");

        Label label = new Label(vmCredsComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));

        dockerHostNoSshRadioButton = new Button(vmCredsComposite, SWT.RADIO);
        GridData gd_dockerHostNoSshRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
        gd_dockerHostNoSshRadioButton.horizontalIndent = 5;
        dockerHostNoSshRadioButton.setLayoutData(gd_dockerHostNoSshRadioButton);
        dockerHostNoSshRadioButton.setText("None");
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);

        dockerHostAutoSshRadioButton = new Button(vmCredsComposite, SWT.RADIO);
        GridData gd_dockerHostAutoSshRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
        gd_dockerHostAutoSshRadioButton.horizontalIndent = 5;
        dockerHostAutoSshRadioButton.setLayoutData(gd_dockerHostAutoSshRadioButton);
        dockerHostAutoSshRadioButton.setText("Auto-generate");
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);

        dockerHostImportSshRadioButton = new Button(vmCredsComposite, SWT.RADIO);
        GridData gd_dockerHostImportSshRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
        gd_dockerHostImportSshRadioButton.horizontalIndent = 5;
        dockerHostImportSshRadioButton.setLayoutData(gd_dockerHostImportSshRadioButton);
        dockerHostImportSshRadioButton.setText("Import from directory:");
        new Label(vmCredsComposite, SWT.NONE);
        new Label(vmCredsComposite, SWT.NONE);

        dockerHostImportSSHTextField = new Text(vmCredsComposite, SWT.BORDER);
        GridData gd_dockerHostImportSSHTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1);
        gd_dockerHostImportSSHTextField.horizontalIndent = 24;
        dockerHostImportSSHTextField.setLayoutData(gd_dockerHostImportSSHTextField);

        dockerHostImportSSHBrowseButton = new Button(vmCredsComposite, SWT.NONE);
        dockerHostImportSSHBrowseButton.setText("Browse...");

        daemonCredsTableItem = new TabItem(credsTabfolder, SWT.NONE);
        daemonCredsTableItem.setText("Docker Daemon Credentials");

        Composite daemonCredsComposite = new Composite(credsTabfolder, SWT.NONE);
        daemonCredsTableItem.setControl(daemonCredsComposite);
        daemonCredsComposite.setLayout(new GridLayout(4, false));

        Label lblDockerDaemonPort = new Label(daemonCredsComposite, SWT.NONE);
        GridData gd_lblDockerDaemonPort = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblDockerDaemonPort.horizontalIndent = 5;
        lblDockerDaemonPort.setLayoutData(gd_lblDockerDaemonPort);
        lblDockerDaemonPort.setText("Docker daemon port:");

        dockerDaemonPortTextField = new Text(daemonCredsComposite, SWT.BORDER);
        GridData gd_dockerDaemonPortTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerDaemonPortTextField.widthHint = 50;
        dockerDaemonPortTextField.setLayoutData(gd_dockerDaemonPortTextField);
        new Label(daemonCredsComposite, SWT.NONE);

        Label lblTlsSecurity = new Label(daemonCredsComposite, SWT.NONE);
        GridData gd_lblTlsSecurity = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblTlsSecurity.horizontalIndent = 5;
        lblTlsSecurity.setLayoutData(gd_lblTlsSecurity);
        lblTlsSecurity.setText("TLS security");

        Label label_1 = new Label(daemonCredsComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        label_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        dockerHostNoTlsRadioButton = new Button(daemonCredsComposite, SWT.RADIO);
        GridData gd_dockerHostNoTlsRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        gd_dockerHostNoTlsRadioButton.horizontalIndent = 5;
        dockerHostNoTlsRadioButton.setLayoutData(gd_dockerHostNoTlsRadioButton);
        dockerHostNoTlsRadioButton.setText("None");
        new Label(daemonCredsComposite, SWT.NONE);

        dockerHostAutoTlsRadioButton = new Button(daemonCredsComposite, SWT.RADIO);
        GridData gd_dockerHostAutoTlsRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        gd_dockerHostAutoTlsRadioButton.horizontalIndent = 5;
        dockerHostAutoTlsRadioButton.setLayoutData(gd_dockerHostAutoTlsRadioButton);
        dockerHostAutoTlsRadioButton.setText("Auto-generate");
        new Label(daemonCredsComposite, SWT.NONE);

        dockerHostImportTlsRadioButton = new Button(daemonCredsComposite, SWT.RADIO);
        GridData gd_dockerHostImportTlsRadioButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        gd_dockerHostImportTlsRadioButton.horizontalIndent = 5;
        dockerHostImportTlsRadioButton.setLayoutData(gd_dockerHostImportTlsRadioButton);
        dockerHostImportTlsRadioButton.setText("Import from directory:");
        new Label(daemonCredsComposite, SWT.NONE);

        dockerHostImportTLSTextField = new Text(daemonCredsComposite, SWT.BORDER);
        GridData gd_dockerHostImportTLSTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        gd_dockerHostImportTLSTextField.horizontalIndent = 24;
        dockerHostImportTLSTextField.setLayoutData(gd_dockerHostImportTLSTextField);

        dockerHostImportTLSBrowseButton = new Button(daemonCredsComposite, SWT.NONE);
        dockerHostImportTLSBrowseButton.setText("Browse...");

        dockerHostSaveCredsCheckBox = new Button(mainContainer, SWT.CHECK);
        GridData gd_dockerHostSaveCredsCheckBox = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostSaveCredsCheckBox.horizontalIndent = 5;
        dockerHostSaveCredsCheckBox.setLayoutData(gd_dockerHostSaveCredsCheckBox);
        dockerHostSaveCredsCheckBox.setText("Save credentials into a new Azure Key Vault:");

        dockerHostNewKeyvaultTextField = new Text(mainContainer, SWT.BORDER);
        GridData gd_dockerHostNewKeyvaultTextField = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_dockerHostNewKeyvaultTextField.widthHint = 210;
        dockerHostNewKeyvaultTextField.setLayoutData(gd_dockerHostNewKeyvaultTextField);

        FormToolkit toolkit = new FormToolkit(mainContainer.getDisplay());
        toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
                HyperlinkSettings.UNDERLINE_HOVER);
        managedForm = new ManagedForm(mainContainer);
        errMsgForm = managedForm.getForm();
        errMsgForm.setVisible(false);
//        errMsgForm.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
//        errMsgForm.setBackground(mainContainer.getBackground());
        errDispatcher = managedForm.getMessageManager();
//        errDispatcher.addMessage("dockerHostNameTextField", "Test error", null, IMessageProvider.ERROR, dockerHostNameTextField);
//        errMsgForm.setMessage("This is an error message", IMessageProvider.ERROR);

        initUIMainContainer(mainContainer);
        mainContainer.setTabList(new Control[]{dockerHostImportKeyvaultCredsRadioButton, dockerHostImportKeyvaultComboBox, dockerHostNewCredsRadioButton, credsTabfolder, dockerHostSaveCredsCheckBox, dockerHostNewKeyvaultTextField});
    }

    private void initUIMainContainer(Composite mainContainer) {
        dockerHostImportKeyvaultCredsRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostImportKeyvaultComboBox.setEnabled(true);
                dockerHostUsernameTextField.setEnabled(false);
                dockerHostFirstPwdField.setEnabled(false);
                dockerHostSecondPwdField.setEnabled(false);
                dockerHostNoSshRadioButton.setEnabled(false);
                dockerHostAutoSshRadioButton.setEnabled(false);
                dockerHostImportSshRadioButton.setEnabled(false);
                dockerHostImportSSHTextField.setEnabled(false);
                dockerHostImportSSHBrowseButton.setEnabled(false);
                dockerHostNoTlsRadioButton.setEnabled(false);
                dockerHostAutoTlsRadioButton.setEnabled(false);
                dockerHostImportTlsRadioButton.setEnabled(false);
                dockerHostImportTLSTextField.setEnabled(false);
                dockerHostImportTLSBrowseButton.setEnabled(false);
                setErrorMessage(null);
                setPageComplete(doValidate());
            }
        });
        dockerHostImportKeyvaultComboBox.setEnabled(false);
        for (AzureDockerCertVault certVault : dockerManager.getDockerKeyVaults()) {
            dockerHostImportKeyvaultComboBox.add(certVault.name);
            dockerHostImportKeyvaultComboBox.setData(certVault.name, certVault);
        }
        if (dockerManager.getDockerKeyVaults().size() > 0) {
            dockerHostImportKeyvaultComboBox.select(0);
        }
        dockerHostImportKeyvaultComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setPageComplete(doValidate());
            }
        });
        dockerHostNewCredsRadioButton.setSelection(true);
        dockerHostNewCredsRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostImportKeyvaultComboBox.setEnabled(false);

                dockerHostUsernameTextField.setEnabled(true);
                dockerHostFirstPwdField.setEnabled(true);
                dockerHostSecondPwdField.setEnabled(true);
                dockerHostNoSshRadioButton.setEnabled(true);
                dockerHostAutoSshRadioButton.setEnabled(true);
                dockerHostImportSshRadioButton.setEnabled(true);
                if (dockerHostImportSshRadioButton.getSelection()) {
                  dockerHostImportSSHTextField.setEnabled(true);
                  dockerHostImportSSHBrowseButton.setEnabled(true);
                }
                dockerDaemonPortTextField.setEnabled(true);
                dockerHostNoTlsRadioButton.setEnabled(true);
                dockerHostAutoTlsRadioButton.setEnabled(true);
                dockerHostImportTlsRadioButton.setEnabled(true);
                if (dockerHostImportTlsRadioButton.getSelection()) {
                  dockerHostImportTLSTextField.setEnabled(true);
                  dockerHostImportTLSBrowseButton.setEnabled(true);
                }
                setErrorMessage(null);
                setPageComplete(doValidate());
            }
        });
        dockerHostUsernameTextField.setText(newHost.certVault.vmUsername);
        dockerHostUsernameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostUserNameTip());
        dockerHostUsernameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerHostUserName(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerHostUsernameTextField", dockerHostUsernameTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostUsernameTextField", AzureDockerValidationUtils.getDockerHostUserNameTip(), null, IMessageProvider.ERROR, dockerHostUsernameTextField);
                    setErrorMessage("Invalid user name");
                    setPageComplete(false);
                }
            }
        });
        dockerHostFirstPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());
        dockerHostFirstPwdField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String text = ((Text) event.getSource()).getText();
                if (text == null || text.isEmpty() || AzureDockerValidationUtils.validateDockerHostPassword(text)) {
                    errDispatcher.removeMessage("dockerHostFirstPwdField", dockerHostFirstPwdField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostFirstPwdField", AzureDockerValidationUtils.getDockerHostPasswordTip(), null, IMessageProvider.ERROR, dockerHostFirstPwdField);
                    setErrorMessage("Invalid password");
                    setPageComplete(false);
                }
            }
        });
        dockerHostSecondPwdField.setToolTipText(AzureDockerValidationUtils.getDockerHostPasswordTip());
        dockerHostSecondPwdField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                String pwd1 = dockerHostFirstPwdField.getText();
                String pwd2 = ((Text) event.getSource()).getText();
                if ((pwd1 == null && pwd2 == null) || pwd2.equals(pwd1)) {
                    errDispatcher.removeMessage("dockerHostFirstPwdField", dockerHostFirstPwdField);
                    errDispatcher.removeMessage("dockerHostSecondPwdField", dockerHostSecondPwdField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostSecondPwdField", AzureDockerValidationUtils.getDockerHostPasswordTip(), null, IMessageProvider.ERROR, dockerHostSecondPwdField);
                    setErrorMessage("Invalid confirmation password");
                    setPageComplete(false);
                }
            }
        });
        dockerHostNoSshRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostPwdLabel.setText("(Required)");
                dockerHostImportSSHTextField.setEnabled(false);
                dockerHostImportSSHBrowseButton.setEnabled(false);
                errDispatcher.removeMessage("dockerHostImportSSHTextField", dockerHostImportSSHTextField);
                setErrorMessage(null);
                setPageComplete(doValidate());
            }
        });
        dockerHostAutoSshRadioButton.setSelection(true);
        dockerHostAutoSshRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostPwdLabel.setText("(Optional)");
                dockerHostImportSSHTextField.setEnabled(false);
                dockerHostImportSSHBrowseButton.setEnabled(false);
                errDispatcher.removeMessage("dockerHostImportSSHTextField", dockerHostImportSSHTextField);
                setErrorMessage(null);
                setPageComplete(doValidate());
            }
        });
        dockerHostImportSshRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostPwdLabel.setText("(Optional)");
                dockerHostImportSSHTextField.setEnabled(true);
                dockerHostImportSSHBrowseButton.setEnabled(true);
                setPageComplete(doValidate());
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
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostImportSSHTextField", AzureDockerValidationUtils.getDockerHostSshDirectoryTip(), null, IMessageProvider.ERROR, dockerHostImportSSHTextField);
                    setErrorMessage("SSH key files not found in the specified directory");
                    setPageComplete(false);
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
                setPageComplete(doValidate());
            }
        });
        dockerDaemonPortTextField.setText(newHost.port);
        dockerDaemonPortTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostPortTip());
        dockerDaemonPortTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerHostPort(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerDaemonPortTextField", dockerDaemonPortTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerDaemonPortTextField", AzureDockerValidationUtils.getDockerHostPortTip(), null, IMessageProvider.ERROR, dockerDaemonPortTextField);
                    setErrorMessage("Invalid Docker daemon port setting");
                    setPageComplete(false);
                }
            }
        });
        dockerHostNoTlsRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostImportTLSTextField.setEnabled(false);
                dockerHostImportTLSBrowseButton.setEnabled(false);
                errDispatcher.removeMessage("dockerHostImportTLSTextField", dockerHostImportTLSTextField);
                setErrorMessage(null);
                setPageComplete(doValidate());
            }
        });
        dockerHostAutoTlsRadioButton.setSelection(true);
        dockerHostAutoTlsRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostImportTLSTextField.setEnabled(false);
                dockerHostImportTLSBrowseButton.setEnabled(false);
                errDispatcher.removeMessage("dockerHostImportTLSTextField", dockerHostImportTLSTextField);
                setErrorMessage(null);
                setPageComplete(doValidate());
            }
        });
        dockerHostImportTlsRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostImportTLSTextField.setEnabled(true);
                dockerHostImportTLSBrowseButton.setEnabled(true);
                setPageComplete(doValidate());
            }
        });
        dockerHostImportTLSTextField.setEnabled(false);
        dockerHostImportTLSTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostTlsDirectoryTip());
        dockerHostImportTLSTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerHostTlsDirectory(((Text) event.getSource()).getText())) {
                    errDispatcher.removeMessage("dockerHostImportTLSTextField", dockerHostImportTLSTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostImportTLSTextField", AzureDockerValidationUtils.getDockerHostTlsDirectoryTip(), null, IMessageProvider.ERROR, dockerHostImportTLSTextField);
                    setErrorMessage("TLS certificate files not found in the specified directory");
                    setPageComplete(false);
                }
            }
        });
        dockerHostImportTLSBrowseButton.setEnabled(false);
        dockerHostImportTLSBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog directoryDialog = new DirectoryDialog(dockerHostImportTLSBrowseButton.getShell());
                directoryDialog.setText("Select TLS Certificate Directory");
                directoryDialog.setFilterPath(System.getProperty("user.home"));
                String path = directoryDialog.open();
                if (path == null) {
                    return;
                }
                dockerHostImportTLSTextField.setText(path);
                setPageComplete(doValidate());
            }
        });
        dockerHostSaveCredsCheckBox.setSelection(true);
        dockerHostSaveCredsCheckBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dockerHostNewKeyvaultTextField.setEnabled(dockerHostSaveCredsCheckBox.getSelection());
                setPageComplete(doValidate());
            }
        });
        dockerHostNewKeyvaultTextField.setText(newHost.certVault.name);
        dockerHostNewKeyvaultTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostKeyvaultNameTip());
        dockerHostNewKeyvaultTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                if (AzureDockerValidationUtils.validateDockerHostKeyvaultName(((Text) event.getSource()).getText(), dockerManager, false)) {
                    errDispatcher.removeMessage("dockerHostNewKeyvaultTextField", dockerHostNewKeyvaultTextField);
                    setErrorMessage(null);
                    setPageComplete(doValidate());
                } else {
                    errDispatcher.addMessage("dockerHostNewKeyvaultTextField", AzureDockerValidationUtils.getDockerHostPortTip(), null, IMessageProvider.ERROR, dockerHostNewKeyvaultTextField);
                    setErrorMessage("Invalid key vault name");
                    setPageComplete(false);
                }
            }
        });

    }


    public boolean doValidate() {
        if (dockerHostImportKeyvaultCredsRadioButton.getSelection()) {
            // read key vault secrets and set the credentials for the new host
            AzureDockerCertVault certVault = null;
            if (dockerHostImportKeyvaultComboBox.getItemCount() > 0) {
                certVault = (AzureDockerCertVault) dockerHostImportKeyvaultComboBox.getData(dockerHostImportKeyvaultComboBox.getText());
            }
            if (certVault == null) {
                errDispatcher.addMessage("dockerHostImportKeyvaultComboBox", AzureDockerValidationUtils.getDockerHostKeyvaultNameTip(), null, IMessageProvider.ERROR, dockerHostImportKeyvaultComboBox);
                setErrorMessage("No Key Vault found");
                return false;
            } else {
                errDispatcher.removeMessage("dockerHostImportKeyvaultComboBox", dockerHostImportKeyvaultComboBox);
                setErrorMessage(null);
                newHost.certVault.name = certVault.name;
                newHost.certVault.resourceGroupName = certVault.resourceGroupName;
                newHost.certVault.region = certVault.region;
                newHost.certVault.uri = certVault.uri;
                AzureDockerCertVaultOps.copyVaultLoginCreds(newHost.certVault, certVault);
                AzureDockerCertVaultOps.copyVaultSshKeys(newHost.certVault, certVault);
                AzureDockerCertVaultOps.copyVaultTlsCerts(newHost.certVault, certVault);
                // create a weak link (resource tag) between the virtual machine and
                // the key vault
                // we will not create/update the key vault unless the user checks
                // the specific option
                newHost.certVault.hostName = null;
                newHost.hasKeyVault = true;
            }
        } else {
            // reset key vault info
            newHost.hasKeyVault = false;
            newHost.certVault.name = null;
            newHost.certVault.uri = null;

            // User name
            String vmUsername = dockerHostUsernameTextField.getText();
            if (vmUsername == null || vmUsername.isEmpty() || !AzureDockerValidationUtils.validateDockerHostUserName(vmUsername)) {
                errDispatcher.addMessage("dockerHostUsernameTextField", AzureDockerValidationUtils.getDockerHostUserNameTip(), null, IMessageProvider.ERROR, dockerHostUsernameTextField);
                setErrorMessage("Invalid user name");
                credsTabfolder.setSelection(0);
                return false;
            } else {
                errDispatcher.removeMessage("dockerHostUsernameTextField", dockerHostUsernameTextField);
                setErrorMessage(null);
                newHost.certVault.vmUsername = vmUsername;
            }

            // Password login
            String vmPwd1 = dockerHostFirstPwdField.getText();
            String vmPwd2 = dockerHostSecondPwdField.getText();
            if ((dockerHostNoSshRadioButton.getSelection() || (vmPwd1 != null && !vmPwd1.isEmpty()) || (vmPwd2 != null && !vmPwd2.isEmpty())) &&
                    (vmPwd1.isEmpty() || vmPwd2.isEmpty() || !vmPwd1.equals(vmPwd2) || !AzureDockerValidationUtils.validateDockerHostPassword(vmPwd1))) {
                errDispatcher.addMessage("dockerHostFirstPwdField", AzureDockerValidationUtils.getDockerHostPasswordTip(), null, IMessageProvider.ERROR, dockerHostFirstPwdField);
                setErrorMessage("Invalid password");
                credsTabfolder.setSelection(0);
                return false;
            } else {
                errDispatcher.removeMessage("dockerHostFirstPwdField", dockerHostFirstPwdField);
                errDispatcher.removeMessage("dockerHostSecondPwdField", dockerHostSecondPwdField);
                setErrorMessage(null);
                if (vmPwd1 == null || vmPwd1.isEmpty()) {
                    newHost.certVault.vmPwd = null;
                    newHost.hasPwdLogIn = false;
                } else {
                    newHost.certVault.vmPwd = vmPwd1;
                    newHost.hasPwdLogIn = true;
                }
            }

            if (dockerHostNoSshRadioButton.getSelection()) {
                newHost.hasSSHLogIn = false;
                newHost.certVault.sshKey = null;
                newHost.certVault.sshPubKey = null;
            }

            // SSH key auto generated
            if (dockerHostAutoSshRadioButton.getSelection()) {
                AzureDockerCertVault certVault = AzureDockerCertVaultOps.generateSSHKeys(null, "SSH keys for " + newHost.name);
                AzureDockerCertVaultOps.copyVaultSshKeys(newHost.certVault, certVault);
                newHost.hasSSHLogIn = true;
            }

            // SSH key imported from local file directory
            if (dockerHostImportSshRadioButton.getSelection()) {
                String sshPath = dockerHostImportSSHTextField.getText();
                if (sshPath == null || sshPath.isEmpty() || !AzureDockerValidationUtils.validateDockerHostSshDirectory(sshPath)) {
                    errDispatcher.addMessage("dockerHostImportSSHTextField", AzureDockerValidationUtils.getDockerHostSshDirectoryTip(), null, IMessageProvider.ERROR, dockerHostImportSSHTextField);
                    setErrorMessage("SSH key files not found in the specified directory");
                    credsTabfolder.setSelection(0);
                    return false;
                } else {
                    try {
                        AzureDockerCertVault certVault = AzureDockerCertVaultOps.getSSHKeysFromLocalFile(sshPath);
                        AzureDockerCertVaultOps.copyVaultSshKeys(newHost.certVault, certVault);
                        newHost.hasSSHLogIn = true;
                    } catch (Exception e) {
                        errDispatcher.addMessage("dockerHostImportSSHTextField", AzureDockerValidationUtils.getDockerHostPasswordTip(), null, IMessageProvider.ERROR, dockerHostImportSSHTextField);
                        setErrorMessage("Unexpected error reading SSH key files from specified directory: " + e.getMessage());
                        return false;
                    }
                    errDispatcher.removeMessage("dockerHostImportSSHTextField", dockerHostImportSSHTextField);
                    setErrorMessage(null);
                }
            }

            // No Docker daemon security
            if (dockerHostNoTlsRadioButton.getSelection()) {
                newHost.isTLSSecured = false;
            }

            // TLS certs auto generated
            if (dockerHostAutoTlsRadioButton.getSelection()) {
                errDispatcher.removeMessage("dockerHostImportTLSTextField", dockerHostImportTLSTextField);
                setErrorMessage(null);
                AzureDockerCertVault certVault = AzureDockerCertVaultOps.generateTLSCerts("TLS certs for " + newHost.name);
                AzureDockerCertVaultOps.copyVaultTlsCerts(newHost.certVault, certVault);
                newHost.isTLSSecured = true;
            }

            // TLS certs imported from local file directory
            if (dockerHostImportTlsRadioButton.getSelection()) {
                String tlsPath = dockerHostImportTLSTextField.getText();
                if ( tlsPath == null || tlsPath.isEmpty() || !AzureDockerValidationUtils.validateDockerHostTlsDirectory(tlsPath)) {
                    errDispatcher.addMessage("dockerHostImportTLSTextField", AzureDockerValidationUtils.getDockerHostTlsDirectoryTip(), null, IMessageProvider.ERROR, dockerHostImportTLSTextField);
                    setErrorMessage("TLS certificate files not found in the specified directory");
                    credsTabfolder.setSelection(1);
                    return false;
                } else {
                    errDispatcher.removeMessage("dockerHostImportTLSTextField", dockerHostImportTLSTextField);
                    setErrorMessage(null);
                    AzureDockerCertVault certVault = AzureDockerCertVaultOps.getTLSCertsFromLocalFile(tlsPath);
                    AzureDockerCertVaultOps.copyVaultTlsCerts(newHost.certVault, certVault);
                    newHost.isTLSSecured = true;
                }
            }

        }

        // Docker daemon port settings
        String port = dockerDaemonPortTextField.getText() ;
        if (port == null || port.isEmpty() || !AzureDockerValidationUtils.validateDockerHostPort(port)) {
            errDispatcher.addMessage("dockerDaemonPortTextField", AzureDockerValidationUtils.getDockerHostPortTip(), null, IMessageProvider.ERROR, dockerDaemonPortTextField);
            setErrorMessage("Invalid Docker daemon port setting");
            credsTabfolder.setSelection(1);
            return false;
        } else {
            errDispatcher.removeMessage("dockerDaemonPortTextField", dockerDaemonPortTextField);
            setErrorMessage(null);
            newHost.port = dockerDaemonPortTextField.getText();
        }

        // create new key vault for storing the credentials
        if (dockerHostSaveCredsCheckBox.getSelection()) {
            String newKeyvault = dockerHostNewKeyvaultTextField.getText();
            if (newKeyvault == null || newKeyvault.isEmpty() || !AzureDockerValidationUtils.validateDockerHostKeyvaultName(newKeyvault, dockerManager, true)) {
                errDispatcher.addMessage("dockerHostNewKeyvaultTextField", AzureDockerValidationUtils.getDockerHostPortTip(), null, IMessageProvider.ERROR, dockerHostNewKeyvaultTextField);
                setErrorMessage("Invalid Key Vault name");
                return false;
            } else {
                errDispatcher.removeMessage("dockerHostNewKeyvaultTextField", dockerHostNewKeyvaultTextField);
                setErrorMessage(null);
                newHost.hasKeyVault = true;
                newHost.certVault.name = dockerHostNewKeyvaultTextField.getText();
                newHost.certVault.hostName = (newHost.name != null) ? newHost.name : null;
                newHost.certVault.region = (newHost.hostVM.region != null) ? newHost.hostVM.region : null;
                newHost.certVault.resourceGroupName = (newHost.hostVM.resourceGroupName != null)
                        ? newHost.hostVM.resourceGroupName : null;
                newHost.certVault.uri = (newHost.hostVM.region != null && newHost.hostVM.resourceGroupName != null)
                        ? "https://" + newHost.certVault.name + ".vault.azure.net" : null;
            }
        } else {
            errDispatcher.removeMessage("dockerHostNewKeyvaultTextField", dockerHostNewKeyvaultTextField);
            setErrorMessage(null);
            newHost.certVault.hostName = null;
        }

        return true;
    }

}
