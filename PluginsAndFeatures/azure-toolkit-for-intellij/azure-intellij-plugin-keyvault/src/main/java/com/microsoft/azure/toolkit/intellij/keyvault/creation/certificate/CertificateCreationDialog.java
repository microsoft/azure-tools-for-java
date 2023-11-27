package com.microsoft.azure.toolkit.intellij.keyvault.creation.certificate;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBPasswordField;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.intellij.keyvault.KeyVaultCredentialActions;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.keyvault.certificate.CertificateDraft;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class CertificateCreationDialog extends AzureDialog<CertificateDraft.Config> implements AzureFormPanel<CertificateDraft.Config> {
    public static final String CERTIFICATE_FILE_HELP = "In Azure Key Vault, supported certificate formats are PFX and PEM.\r\n" +
            "\t .pem file format contains one or more X509 certificate files. " +
            "\t .pfx file format is an archive file format for storing several cryptographic objects in a single file i.e. server certificate (issued for your domain), a matching private key, and may optionally include an intermediate CA.";
    private JPanel pnlRoot;
    private AzureTextInput txtName;
    private JLabel lblName;
    private AzureFileInput txtCertificate;
    private AzurePasswordFieldInput txtPassword;
    private JBPasswordField passwordField;
    private JLabel lblCertificateFile;
    private JLabel lblPassword;
    private TitledSeparator titleInstance;
    private final String title;

    public CertificateCreationDialog(String title) {
        super();
        this.title = title;
        setTitle(this.title);
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        super.init();
        lblName.setIcon(AllIcons.General.ContextHelp);
        lblCertificateFile.setIcon(AllIcons.General.ContextHelp);
        lblCertificateFile.setToolTipText(CERTIFICATE_FILE_HELP);

        txtName.setRequired(true);
        txtName.addValidator(() -> KeyVaultCredentialActions.validateCredentialName(txtName));
        txtCertificate.setRequired(true);
        txtPassword.setRequired(false);
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                .withFileFilter(file -> StringUtils.equalsAnyIgnoreCase(file.getExtension(), "pem", "pfx"));
        txtCertificate.addBrowseFolderListener("Select Certificate File", "Select Certificate File", null, descriptor);
        this.lblCertificateFile.setLabelFor(txtCertificate);
        this.lblName.setLabelFor(txtName);
        this.lblPassword.setLabelFor(passwordField);
    }

    @Override
    public AzureForm<CertificateDraft.Config> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return title;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public void setValue(CertificateDraft.Config data) {
        txtName.setValue(data.getName());
        txtCertificate.setValue(data.getPath().toString());
        txtPassword.setValue(data.getPassword());
    }

    @Override
    @Nullable
    public CertificateDraft.Config getValue() {
        final CertificateDraft.Config data = new CertificateDraft.Config();
        data.setName(txtName.getValue());
        data.setPath(Paths.get(txtCertificate.getValue()));
        data.setPassword(txtPassword.getValue());
        return data;
    }

    public void setFixedName(final String name) {
        txtName.setValue(name);
        txtName.setEnabled(false);
        txtName.setEditable(false);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, txtCertificate, txtPassword);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.passwordField = new JBPasswordField();
        this.txtPassword = new AzurePasswordFieldInput(this.passwordField);
    }
}
