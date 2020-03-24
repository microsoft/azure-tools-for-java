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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;

public class AzureExportDockerTlsKeysDialog extends AzureTitleAreaDialogWrapper {
    private static final Logger log =  Logger.getLogger(AzureExportDockerTlsKeysDialog.class.getName());

    private final String pathToolTip = "Directory to save/export TLS file certificates into:";

    private Text exportTlsPathTextField;

    private ManagedForm managedForm;
    private ScrolledForm errMsgForm;
    private IMessageManager errDispatcher;

    private IProject project;
    private String path;


    /**
     * Create the dialog.
     * @param parentShell
     */
    public AzureExportDockerTlsKeysDialog(Shell parentShell, IProject project) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.MIN | SWT.RESIZE);

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
        setTitle("Export TLS Certificates");
        setMessage(pathToolTip);

        Composite area = (Composite) super.createDialogArea(parent);
        Composite mainContainer = new Composite(area, SWT.NONE);
        mainContainer.setLayout(new GridLayout(2, false));
        GridData gd_mainContainer = new GridData(GridData.FILL_BOTH);
        gd_mainContainer.widthHint = 524;
        mainContainer.setLayoutData(gd_mainContainer);

        exportTlsPathTextField = new Text(mainContainer, SWT.BORDER);
        exportTlsPathTextField.setToolTipText(pathToolTip);
        exportTlsPathTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent event) {
                path = (((Text) event.getSource()).getText());
                if (path != null && Files.isDirectory(Paths.get(path))) {
                    errDispatcher.removeMessage("exportTlsPathTextField", exportTlsPathTextField);
                    setErrorMessage(null);
                } else {
                    errDispatcher.addMessage("exportTlsPathTextField", pathToolTip, null, IMessageProvider.ERROR, exportTlsPathTextField);
                    setErrorMessage("Invalid directory path");
                }
            }
        });
        GridData gd_exportTlsPathTextField = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_exportTlsPathTextField.widthHint = 380;
        gd_exportTlsPathTextField.verticalIndent = 5;
        gd_exportTlsPathTextField.horizontalIndent = 5;
        exportTlsPathTextField.setLayoutData(gd_exportTlsPathTextField);

        Button exportTlsPathBrowseButton = new Button(mainContainer, SWT.NONE);
        exportTlsPathBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog directoryDialog = new DirectoryDialog(exportTlsPathTextField.getShell());
                directoryDialog.setText("Select TLS Certificates Directory");
                directoryDialog.setFilterPath(System.getProperty("user.home"));
                String pathSelected = directoryDialog.open();
                if (pathSelected == null) {
                    return;
                }
                path = pathSelected;
                exportTlsPathTextField.setText(path);
            }
        });
        GridData gd_exportTlsPathBrowseButton = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_exportTlsPathBrowseButton.verticalIndent = 5;
        exportTlsPathBrowseButton.setLayoutData(gd_exportTlsPathBrowseButton);
        exportTlsPathBrowseButton.setText("Browse...");

        Label lblNote = new Label(mainContainer, SWT.NONE);
        GridData gd_lblNote = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblNote.verticalIndent = 5;
        gd_lblNote.horizontalIndent = 5;
        lblNote.setLayoutData(gd_lblNote);
        lblNote.setText("Note:");
        new Label(mainContainer, SWT.NONE);

        Label lblAnyExisting = new Label(mainContainer, SWT.NONE);
        GridData gd_lblAnyExisting = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblAnyExisting.horizontalIndent = 20;
        lblAnyExisting.setLayoutData(gd_lblAnyExisting);
        lblAnyExisting.setText("Any existing \"ca.pem\", \"ca-key.pem\", \"cert.pem\",\"key.pem\", \"server.pem\" and \"server-key.pem\"");

        Label lblWillBeOverwritten = new Label(mainContainer, SWT.NONE);
        GridData gd_lblWillBeOverwritten = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_lblWillBeOverwritten.horizontalIndent = 20;
        lblWillBeOverwritten.setLayoutData(gd_lblWillBeOverwritten);
        lblWillBeOverwritten.setText("certificate files in the selected directory will be overwritten!");

        FormToolkit toolkit = new FormToolkit(mainContainer.getDisplay());
        toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
                HyperlinkSettings.UNDERLINE_HOVER);
        managedForm = new ManagedForm(mainContainer);
        errMsgForm = managedForm.getForm();
        errMsgForm.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        errMsgForm.setBackground(mainContainer.getBackground());
        errDispatcher = managedForm.getMessageManager();

        return area;
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
        return new Point(580, 280);
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
