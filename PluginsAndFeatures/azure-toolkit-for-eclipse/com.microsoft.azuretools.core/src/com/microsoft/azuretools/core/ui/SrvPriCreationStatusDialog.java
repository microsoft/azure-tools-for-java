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

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.azuretools.authmanage.srvpri.SrvPriManager;
import com.microsoft.azuretools.authmanage.srvpri.report.IListener;
import com.microsoft.azuretools.authmanage.srvpri.step.Status;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;


public class SrvPriCreationStatusDialog extends AzureTitleAreaDialogWrapper {
    private static ILog LOG = Activator.getDefault().getLog();
    private Table table;
    org.eclipse.swt.widgets.List listCreatedFiles;

    private String destinationFolder;
    private Map<String, List<String> > tidSidsMap;
    private final AccessTokenAzureManager preAccessTokenAzureManager;

    private String selectedAuthFilePath;

    public String getSelectedAuthFilePath() {
        return selectedAuthFilePath;
    }

    public static SrvPriCreationStatusDialog go(AccessTokenAzureManager preAccessTokenAzureManager,
                                                Shell parentShell,
                                                Map<String, List<String> > tidSidsMap,
                                                String destinationFolder) {
        SrvPriCreationStatusDialog d = new SrvPriCreationStatusDialog(preAccessTokenAzureManager, parentShell);
        d.tidSidsMap = tidSidsMap;
        d.destinationFolder = destinationFolder;
        d.create();
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    /**
     * Create the dialog.
     * @param parentShell
     */
    private SrvPriCreationStatusDialog(AccessTokenAzureManager preAccessTokenAzureManager, Shell parentShell) {
        super(parentShell);
        this.preAccessTokenAzureManager = preAccessTokenAzureManager;
        setHelpAvailable(false);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Service Principal Creation Status");
        setTitle("Service Principal Creation Status");
        getShell().setText("Service Principal Creation Status");
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_table.heightHint = 300;
        table.setLayoutData(gd_table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn tblclmnStep = new TableColumn(table, SWT.NONE);
        tblclmnStep.setWidth(250);
        tblclmnStep.setText("Step");

        TableColumn tblclmnResult = new TableColumn(table, SWT.NONE);
        tblclmnResult.setWidth(100);
        tblclmnResult.setText("Result");

        TableColumn tblclmnDetails = new TableColumn(table, SWT.NONE);
        tblclmnDetails.setWidth(250);
        tblclmnDetails.setText("Details");

        Label lblCreatedAuthenticationFiles = new Label(container, SWT.NONE);
        lblCreatedAuthenticationFiles.setText("Created authentication file(s) (One per Active Directory instance/tenant):");

        listCreatedFiles = new org.eclipse.swt.widgets.List(container, SWT.BORDER);
        listCreatedFiles.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        return area;
    }

    @Override
    public void create() {
        super.create();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
              System.out.println("Starting createServicePrincipalAsync()...");
              createServicePrincipalAsync();
            }
          });
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button button = createButton(parent, IDialogConstants.FINISH_ID, IDialogConstants.OK_LABEL, true);
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onOk();
            }
        });
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private void onOk() {
        if (listCreatedFiles.getItemCount() > 0) {
            // select the first in the list
            selectedAuthFilePath = listCreatedFiles.getItem(0);
            // select the first selected
            for (String path : listCreatedFiles.getSelection()) {
                selectedAuthFilePath = path;
                break;
            }
        }
        super.okPressed();
    }

    private void createServicePrincipalAsync() {
        try {
            class StatusTask implements IRunnableWithProgress, IListener<Status> {
                IProgressMonitor progressIndicator = null;

                @Override
                public void listen(Status status) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (progressIndicator != null) {
                                progressIndicator.setTaskName(status.getAction());
                            }
                            // if only action was set in the status - the info for progress indicator only - igonre for table
                            if (status.getResult() != null) {
                                TableItem item = new TableItem(table, SWT.NULL);
                                item.setText(new String[] {status.getAction(), status.getResult().toString(), status.getDetails()});
                            }
                        }
                    });
                }

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    progressIndicator = monitor;
                    monitor.beginTask("Creating Service Principal...", IProgressMonitor.UNKNOWN);
                    for (String tid : tidSidsMap.keySet()) {
                        if (monitor.isCanceled()) {
                            Display.getDefault().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    TableItem item = new TableItem(table, SWT.NULL);
                                    item.setText(new String[] {"!!! Canceled by user"});
                                }
                            });
                            return;
                        }
                        List <String> sidList = tidSidsMap.get(tid);
                        if (!sidList.isEmpty()) {
                            try {
                                Display.getDefault().asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        TableItem item = new TableItem(table, SWT.NULL);
                                        item.setText(new String[] {"tenant ID: " + tid + " ==="});
                                    }
                                });
                                Date now = new Date();
                                String suffix = new SimpleDateFormat("yyyyMMddHHmmss").format(now);;
                                String authFilepath = SrvPriManager.createSp(
                                        preAccessTokenAzureManager, tid, sidList, suffix, this, destinationFolder);
                                if (authFilepath != null) {
                                    Display.getDefault().asyncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                            listCreatedFiles.add(authFilepath);
                                            listCreatedFiles.setSelection(0);
                                        }
                                    });
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                LOG.log(new org.eclipse.core.runtime.Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@createServicePrincipalAsync@SrvPriCreationStatusDialog", ex));

                            }
                        }
                    }
                }
            }
            new ProgressMonitorDialog(this.getShell()).run(true, true, new StatusTask());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
