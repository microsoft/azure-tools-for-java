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

package com.microsoft.azuretools.core.ui.commoncontrols;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azuretools.azurecommons.util.WAEclipseHelperMethods;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.azurecommons.util.Utils;

public class NewCertificateDialog extends AzureTitleAreaDialogWrapper {

    private Text txtPwd;
    private Text txtConfirmPwd;
    private Text txtCNName;
    private Text txtCertFile;
    private Text txtPFXFile;
    private String errorTitle;
    private String errorMessage;
    private IProject selProject;
    private NewCertificateDialogData newCertificateDialogHolder;
    private String jdkPath;

    public NewCertificateDialog(Shell parentShell,
            NewCertificateDialogData newCertificateDialogHolder,
            String jdkPath) {
        super(parentShell);
        this.newCertificateDialogHolder = newCertificateDialogHolder;
        this.jdkPath = jdkPath;
    }

    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.newCertDlgCertTxt);
        newShell.setLocation(200, 200);
        Image image;
        try {
            URL imgUrl = Activator.getDefault().
                    getBundle().getEntry(Messages.newCertDlgImg);
            URL imgFileURL = FileLocator.toFileURL(imgUrl);
            URL path = FileLocator.resolve(imgFileURL);
            String imgpath = path.getFile();
            image = new Image(null, new FileInputStream(imgpath));
            setTitleImage(image);
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(getShell(),
                    com.microsoft.azuretools.core.utils.Messages.err,
                    Messages.imgErr, e);
        }
    }

    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.newCertDlgCertTxt);
        setMessage(Messages.newCertDlgCertMsg);
        //display help contents
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
                "com.microsoftopentechnologies.wacommon."
                + "new_cert_dialog");

        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        GridData gridData = new GridData();
        gridLayout.numColumns = 2;
        gridData.verticalIndent = 10;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        Label pwdLabel = new Label(container, SWT.LEFT);
        pwdLabel.setText(Messages.newCertDlgPwdLbl);
        gridData = new GridData();
        gridData.horizontalIndent = 10;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.widthHint = 350;

        txtPwd = new Text(container, SWT.BORDER | SWT.PASSWORD);
        txtPwd.setLayoutData(gridData);

        Label confirmPwdLbl = new Label(container, SWT.LEFT);
        confirmPwdLbl.setText(Messages.newCertDlgCnfPwdLbl);
        txtConfirmPwd = new Text(container, SWT.BORDER | SWT.PASSWORD);
        txtConfirmPwd.setLayoutData(gridData);

        // For common name
        Label cnNameLabel = new Label(container, SWT.LEFT);
        cnNameLabel.setText(Messages.newCertDlgCNNameLbl);
        txtCNName = new Text(container, SWT.BORDER);
        txtCNName.setLayoutData(gridData);
        txtCNName.setText(Utils.getDefaultCNName());


        Group group =  new Group(container, SWT.SHADOW_ETCHED_IN);
        GridLayout groupGridLayout = new GridLayout();
        GridData groupGridData = new GridData();
        groupGridData.grabExcessHorizontalSpace = true;
        groupGridData.horizontalAlignment = SWT.FILL;
        groupGridLayout.numColumns = 3;
        groupGridData.horizontalSpan = 2;
        groupGridData.verticalIndent = 10;
        groupGridData.widthHint = 450;
        group.setText(Messages.newCertDlgGrpLbl);
        group.setLayout(groupGridLayout);
        group.setLayoutData(groupGridData);

        createCertFileComp(group);

        createPfxFileComp(group);

        return super.createDialogArea(parent);
    }

    /**
     * Method for creating .pfx file component.
     * @param group
     */
    private void createPfxFileComp(Group group) {
        Label pfxFileLabel = new Label(group, SWT.LEFT);
        pfxFileLabel.setText(Messages.newCertDlgPFXLbl);
        GridData groupGridData = new GridData();
        groupGridData.grabExcessHorizontalSpace = true;
        groupGridData.horizontalAlignment = SWT.FILL;
        groupGridData.horizontalSpan = 3;
        groupGridData.widthHint = 350;
        groupGridData.verticalIndent = 5;
        pfxFileLabel.setLayoutData(groupGridData);

        txtPFXFile = new Text(group, SWT.BORDER | SWT.LEFT);
        groupGridData = new GridData();
        groupGridData.grabExcessHorizontalSpace = true;
        groupGridData.horizontalAlignment = SWT.FILL;
        groupGridData.widthHint = 350;
        txtPFXFile.setLayoutData(groupGridData);

        Button pfxBrowseButton =  new Button(group, SWT.PUSH
                | SWT.CENTER | SWT.END);
        pfxBrowseButton.setText(Messages.newCertDlgBrwsBtn);
        groupGridData = new GridData();
        groupGridData.widthHint = 100;
        groupGridData.grabExcessHorizontalSpace = true;
        groupGridData.horizontalAlignment = SWT.FILL;
        pfxBrowseButton.setLayoutData(groupGridData);
        pfxBrowseButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                String location = browseBtnListener("*.pfx");
                if (location != null) {
                    if (location.endsWith(".pfx") || location.endsWith(".PFX")) {
                        txtPFXFile.setText(location);
                    } else {
                        StringBuffer stringBuffer = new StringBuffer(location);
                        stringBuffer.append(".pfx");
                        txtPFXFile.setText(stringBuffer.toString());
                    }
                }

                // Set default value for cert text field
                if ( (txtPFXFile.getText() != null  &&
                        (txtCertFile.getText() == null || txtCertFile.getText().isEmpty()))) {
                    if (txtPFXFile.getText().endsWith(".pfx")) {
                        txtCertFile.setText(Utils.replaceLastSubString(txtPFXFile.getText(), ".pfx", ".cer"));
                    } else if (txtPFXFile.getText().endsWith(".PFX")) {
                        txtCertFile.setText(Utils.replaceLastSubString(txtPFXFile.getText(), ".PFX", ".CER"));
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });
    }

    /**
     * Method for creating .cer file component.
     * @param group
     */
    private void createCertFileComp(Group group) {
        Label certFileLabel = new Label(group, SWT.LEFT);
        certFileLabel.setText(Messages.newCertDlgCertLbl);
        GridData groupGridData = new GridData();
        groupGridData.grabExcessHorizontalSpace = true;
        groupGridData.horizontalSpan = 3;
        groupGridData.widthHint = 350;
        groupGridData.verticalIndent = 5;
        certFileLabel.setLayoutData(groupGridData);

        txtCertFile = new Text(group, SWT.BORDER | SWT.LEFT);
        groupGridData = new GridData();
        groupGridData.grabExcessHorizontalSpace = true;
        groupGridData.horizontalAlignment = SWT.FILL;
        groupGridData.widthHint = 350;
        txtCertFile.setLayoutData(groupGridData);

        Button certBrowseButton = new Button(group, SWT.PUSH
                | SWT.CENTER | SWT.END);
        certBrowseButton.setText(Messages.newCertDlgBrwsBtn);
        groupGridData = new GridData();
        groupGridData.widthHint = 100;
        groupGridData.grabExcessHorizontalSpace = true;
        groupGridData.horizontalAlignment = SWT.FILL;
        certBrowseButton.setLayoutData(groupGridData);
        certBrowseButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                String location = browseBtnListener("*.cer");
                if (location != null) {
                    if (location.endsWith(".cer") || location.endsWith(".CER")) {
                        txtCertFile.setText(location);
                    } else {
                        StringBuffer strBfr = new StringBuffer(location);
                        strBfr.append(".cer");
                        txtCertFile.setText(strBfr.toString());
                    }
                }

                // Set default value for pfx text field
                if ( (txtCertFile.getText() != null  &&
                        (txtPFXFile.getText() == null || txtPFXFile.getText().isEmpty()))) {
                    if (txtCertFile.getText().endsWith(".cer")) {
                        txtPFXFile.setText(Utils.replaceLastSubString(txtCertFile.getText(), ".cer", ".pfx"));
                    } else if (txtCertFile.getText().endsWith(".CER")) {
                        txtPFXFile.setText(Utils.replaceLastSubString(txtCertFile.getText(), ".CER", ".PFX"));
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });
    }

    /**
     * Listener for Browse Button.
     * @param ext
     * @return
     */
    protected String browseBtnListener(String ext) {
        FileDialog dialog = new FileDialog(this.getShell(), SWT.SAVE);
        String []extensions = new String [2];
        extensions[0] = ext.toLowerCase(Locale.US);
        extensions[1] = ext.toUpperCase(Locale.US);
        dialog.setOverwrite(true);
        selProject = PluginUtil.getSelectedProject();
        if (selProject != null) {
            String path = selProject.getLocation().toPortableString();
            dialog.setFilterPath(path);
        }
        dialog.setText(Messages.newCertDlgBrwFldr);
        dialog.setFilterExtensions(extensions);
        return dialog.open();
    }

    @Override
    protected void okPressed() {
        if (txtPwd.getText() == null
                || txtPwd.getText().isEmpty()) {
            errorTitle = Messages.newCertDlgCrtErTtl;
            errorMessage = Messages.newCertDlgPwNul;
            PluginUtil.displayErrorDialog(this.getShell(),
                    errorTitle, errorMessage);
            return;
        } else if (txtConfirmPwd.getText() == null
                || txtConfirmPwd.getText().isEmpty()) {
            errorTitle = Messages.newCertDlgCrtErTtl;
            errorMessage = Messages.newCertDlgCfPwNul;
            PluginUtil.displayErrorDialog(this.getShell(),
                    errorTitle, errorMessage);
            return;
        } else if (!(txtPwd.getText() == null)
                && !(txtPwd.getText().isEmpty())) {
            // check for password length
            if (txtPwd.getText().length() < 6) {
                PluginUtil.displayErrorDialog(getShell(),
                        Messages.newCertDlgPwdWrng, Messages.newCertDlgPwLength);
                return;
            }
            Pattern pattern = Pattern.compile("^\\S+$");
            Matcher match = pattern.matcher(txtPwd.getText());
            if (!match.find()) {
                errorTitle = Messages.newCertDlgPwdWrng;
                errorMessage = Messages.newCertDlgPwNtCor;
                PluginUtil.displayErrorDialog(getShell(),
                        errorTitle, errorMessage);
                return;
            }
        }
        if (!(txtConfirmPwd.getText() == null)
                && !(txtConfirmPwd.getText().isEmpty())) {
            // check for password length
            if (txtConfirmPwd.getText().length() < 6) {
                PluginUtil.displayErrorDialog(getShell(),
                        Messages.newCertDlgPwdWrng, Messages.newCertDlgPwLength);
                return;
            }
            Pattern pattern = Pattern.compile("^\\S+$");
            Matcher match = pattern.matcher(txtConfirmPwd.getText());
            if (!match.find()) {
                errorTitle = Messages.newCertDlgPwdWrng;
                errorMessage = Messages.newCertDlgPwNtCor;
                PluginUtil.displayErrorDialog(getShell(),
                        errorTitle, errorMessage);
                return;
            }
        }
        if (!(txtPwd.getText()).equals(txtConfirmPwd.getText())) {
            errorTitle = Messages.newCertDlgPwNtMtch;
            errorMessage = Messages.newCerDlgPwNtMsg;
            PluginUtil.displayErrorDialog(getShell(),
                    errorTitle, errorMessage);
            return;
        }
        if (txtCNName.getText() == null || txtCNName.getText().isEmpty()) {
             errorTitle = Messages.newCertDlgCrtErTtl;
             errorMessage = Messages.newCertDlgCNNull;
             PluginUtil.displayErrorDialog(this.getShell(),
                     errorTitle, errorMessage);
             return;
        }
        if (txtCertFile.getText() == null || txtCertFile.getText().isEmpty()) {
            errorTitle = Messages.newCertDlgCrtErTtl;
            errorMessage = Messages.newCertDlgCerNul;
            PluginUtil.displayErrorDialog(this.getShell(),
                    errorTitle, errorMessage);
            return;
        } else if (txtPFXFile.getText() == null
                || txtPFXFile.getText().isEmpty()) {
            errorTitle = Messages.newCertDlgCrtErTtl;
            errorMessage = Messages.newCertDlgPFXNull;
            PluginUtil.displayErrorDialog(this.getShell(),
                    errorTitle, errorMessage);
            return;
        }
        String certFilePath = txtCertFile.getText();
        String pfxFilePath = txtPFXFile.getText();
        if (certFilePath.lastIndexOf(File.separator) == -1
                || pfxFilePath.lastIndexOf(File.separator) == -1) {
            errorTitle = Messages.newCertDlgCrtErTtl;
            errorMessage = Messages.newCerDlgInvldPth;
            PluginUtil.displayErrorDialog(this.getShell(),
                    errorTitle, errorMessage);
            return;
        }
        if ((!(certFilePath.endsWith(".cer") || certFilePath.endsWith(".CER")))
                || (!(pfxFilePath.endsWith(".pfx") || pfxFilePath.endsWith(".PFX")))) {
            errorTitle = Messages.newCertDlgCrtErTtl;
            errorMessage = Messages.newCerDlgInvdFlExt;
            PluginUtil.displayErrorDialog(this.getShell(),
                    errorTitle, errorMessage);
            return;
        }
        String certFolder = certFilePath.substring(0,
                certFilePath.lastIndexOf(File.separator));
        String pfxFolder = pfxFilePath.substring(0,
                pfxFilePath.lastIndexOf(File.separator));
        File certFile = new File(certFolder);
        File pfxFile = new File(pfxFolder);
        if (!(certFile.exists() && pfxFile.exists())) {
            errorTitle = Messages.newCertDlgCrtErTtl;
            errorMessage = Messages.newCerDlgInvldPth;
            PluginUtil.displayErrorDialog(this.getShell(),
                    errorTitle, errorMessage);
            return;
        }
        else {
            try {
                PluginUtil.showBusy(true, getShell());
                String alias = Messages.newCertDlgAlias;
                // fix for #2663
                if (jdkPath == null || jdkPath.isEmpty()) {
                    jdkPath = WAEclipseHelperMethods.jdkDefaultDirectory(null);
                }
                com.microsoft.azuretools.azurecommons.util.CerPfxUtil.createCertificate(txtCertFile.getText(),
                        txtPFXFile.getText(), alias , txtPwd.getText(), txtCNName.getText(), jdkPath);

                //At this point certificates are created , populate the values for caller
                if(newCertificateDialogHolder != null ){
                    newCertificateDialogHolder.setCerFilePath(txtCertFile.getText());
                    newCertificateDialogHolder.setPfxFilePath(txtPFXFile.getText());
                    newCertificateDialogHolder.setPassword(txtPFXFile.getText());
                    newCertificateDialogHolder.setCnName(txtCNName.getText());
                }
                PluginUtil.showBusy(false, getShell());
            } catch (Exception e) {
                PluginUtil.showBusy(false, getShell());
                Activator.getDefault().log(e.getMessage(), e);
                errorTitle = Messages.newCertDlgCrtErTtl;
                errorMessage = Messages.newCerDlgCrtCerEr;
                PluginUtil.displayErrorDialog(this.getShell(),
                        errorTitle, errorMessage);
                return;
            }
        }
        super.okPressed();
    }
}
