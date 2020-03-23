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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.docker.model.AzureDockerCertVault;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;

import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class AzureExportDockerSshKeysDialog extends AzureTitleAreaDialogWrapper {
    private static final Logger log =  Logger.getLogger(AzureExportDockerSshKeysDialog.class.getName());

    private final String pathToolTip = "Directory to save/export \"id_rsa\" and \"id_rsa.pub\" file keys into:";

    private Text exportSshPathTextField;

    private ManagedForm managedForm;
    private ScrolledForm errMsgForm;
    private IMessageManager errDispatcher;

    private IProject project;
    private String path;

    /**
     * Create the dialog.
     * @param parentShell
     */
    public AzureExportDockerSshKeysDialog(Shell parentShell, IProject project) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.MIN | SWT.RESIZE);

        this.project = project;
        this.project = project;
        try {
            path = project != null ? project.getLocation().toString() : "";
        } catch (Exception ex) {
            path = "";
        }
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Export SSH Keys");
        setMessage(pathToolTip);

        Composite area = (Composite) super.createDialogArea(parent);
        Composite mainContainer = new Composite(area, SWT.NONE);
        mainContainer.setLayout(new GridLayout(2, false));
        mainContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        exportSshPathTextField = new Text(mainContainer, SWT.BORDER);
        exportSshPathTextField.setToolTipText(pathToolTip);
        exportSshPathTextField.setText(path);
        exportSshPathTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                path = (((Text) event.getSource()).getText());
                if (Files.isDirectory(Paths.get(path))) {
                    errDispatcher.removeMessage("exportSshPathTextField", exportSshPathTextField);
                    setErrorMessage(null);
                } else {
                    errDispatcher.addMessage("exportSshPathTextField", pathToolTip, null, IMessageProvider.ERROR, exportSshPathTextField);
                    setErrorMessage("Invalid directory path");
                }
            }
        });
        GridData gd_exportSshPathTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_exportSshPathTextField.verticalIndent = 5;
        gd_exportSshPathTextField.horizontalIndent = 5;
        exportSshPathTextField.setLayoutData(gd_exportSshPathTextField);

        Button exportSshPathBrowseButton = new Button(mainContainer, SWT.NONE);
        exportSshPathBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog directoryDialog = new DirectoryDialog(exportSshPathTextField.getShell());
                directoryDialog.setText("Select SSH Keys Directory");
                directoryDialog.setFilterPath(System.getProperty("user.home"));
                String pathSelected = directoryDialog.open();
                if (pathSelected == null) {
                    return;
                }
                path = pathSelected;
                exportSshPathTextField.setText(path);
            }
        });
        GridData gd_exportSshPathBrowseButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_exportSshPathBrowseButton.verticalIndent = 5;
        exportSshPathBrowseButton.setLayoutData(gd_exportSshPathBrowseButton);
        exportSshPathBrowseButton.setText("Browse...");

        Label lblNote = new Label(mainContainer, SWT.NONE);
        GridData gd_lblNote = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblNote.verticalIndent = 5;
        gd_lblNote.horizontalIndent = 5;
        lblNote.setLayoutData(gd_lblNote);
        lblNote.setText("Note:");
        new Label(mainContainer, SWT.NONE);

        Label lblAnyExistingidrsa = new Label(mainContainer, SWT.NONE);
        GridData gd_lblAnyExistingidrsa = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblAnyExistingidrsa.horizontalIndent = 20;
        lblAnyExistingidrsa.setLayoutData(gd_lblAnyExistingidrsa);
        lblAnyExistingidrsa.setText("Any existing \"id_rsa\" and \"id_rsa.pub\" files in the selected directory");

        Label lblWillBeOverwritten = new Label(mainContainer, SWT.NONE);
        GridData gd_lblWillBeOverwritten = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblWillBeOverwritten.horizontalIndent = 20;
        lblWillBeOverwritten.setLayoutData(gd_lblWillBeOverwritten);
        lblWillBeOverwritten.setText("will be overwritten!");
        new Label(mainContainer, SWT.NONE);

        FormToolkit toolkit = new FormToolkit(mainContainer.getDisplay());
        toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
                HyperlinkSettings.UNDERLINE_HOVER);
        managedForm = new ManagedForm(mainContainer);
        errMsgForm = managedForm.getForm();
        errMsgForm.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        errMsgForm.setBackground(mainContainer.getBackground());
        errDispatcher = managedForm.getMessageManager();

//        return area;
        return super.createDialogArea(parent);
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Save", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(450, 275);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    public boolean doValidate() {
        return true;
    }

    public String getPath() {
        return path;
    }
}
