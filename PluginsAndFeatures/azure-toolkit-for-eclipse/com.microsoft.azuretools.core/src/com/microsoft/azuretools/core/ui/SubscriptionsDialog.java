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
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SELECT_SUBSCRIPTIONS;

import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.utils.ProgressDialog;

public class SubscriptionsDialog extends AzureTitleAreaDialogWrapper {
    private static ILog LOG = Activator.getDefault().getLog();

    private Table table;

    private SubscriptionManager subscriptionManager;
    private List<SubscriptionDetail> sdl;

    /**
     * Create the dialog.
     * @param parentShell
     */
    private SubscriptionsDialog(Shell parentShell, SubscriptionManager subscriptionManage) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
        this.subscriptionManager = subscriptionManage;
    }

    public static SubscriptionsDialog go(Shell parentShell, SubscriptionManager subscriptionManager) {
        SubscriptionsDialog d = new SubscriptionsDialog(parentShell, subscriptionManager);
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Select subscription(s) you want to use.");
        setTitle("Your Subscriptions");
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        table = new Table(container, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
        GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_table.heightHint = 300;
        table.setLayoutData(gd_table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn tblclmnNewColumn = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn.setWidth(300);
        tblclmnNewColumn.setText("Subscription Name");

        TableColumn tblclmnNewColumn_1 = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn_1.setWidth(270);
        tblclmnNewColumn_1.setText("Subscription ID");

        Button btnRefresh = new Button(container, SWT.NONE);
        btnRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshSubscriptions();
            }
        });
        GridData gd_btnRefresh = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
        gd_btnRefresh.widthHint = 78;
        btnRefresh.setLayoutData(gd_btnRefresh);
        btnRefresh.setText("Refresh");

        return area;
    }

    @Override
    public void create() {
        super.create();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
              System.out.println("refreshSubscriptionsAsync");
              refreshSubscriptionsAsync();
              setSubscriptionDetails();
            }
          });
    }

    public void refreshSubscriptionsAsync() {
        try {
            ProgressDialog.get(getShell(), "Update Azure Local Cache Progress").run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Reading subscriptions...", IProgressMonitor.UNKNOWN);
                    EventUtil.executeWithLog(TelemetryConstants.ACCOUNT, TelemetryConstants.GET_SUBSCRIPTIONS, (operation) -> {
                        subscriptionManager.getSubscriptionDetails();
                    }, (ex) -> {
                        ex.printStackTrace();
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@efreshSubscriptionsAsync@SubscriptionDialog", ex));
                    });
                    monitor.done();
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
            ex.printStackTrace();
            //LOGGER.log(LogService.LOG_ERROR, "run@refreshSubscriptionsAsync@SubscriptionDialog", e);
             LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@refreshSubscriptionsAsync@SubscriptionDialog", ex));
        }
    }

    private void setSubscriptionDetails() {
        try {
            sdl = subscriptionManager.getSubscriptionDetails();
            for (SubscriptionDetail sd : sdl) {
                TableItem item = new TableItem(table, SWT.NULL);
                item.setText(new String[] {sd.getSubscriptionName(), sd.getSubscriptionId()});
                item.setChecked(sd.isSelected());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "setSubscriptionDetails@SubscriptionDialog", ex));
        }
    }

    private void refreshSubscriptions() {
        try {
            System.out.println("refreshSubscriptions");
            table.removeAll();
            subscriptionManager.cleanSubscriptions();
            refreshSubscriptionsAsync();
            setSubscriptionDetails();
            subscriptionManager.setSubscriptionDetails(sdl);
        } catch (IOException ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "refreshSubscriptions@SubscriptionDialog", ex));
        }
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText("Select");
    }

    @Override
    public void okPressed() {
        EventUtil.logEvent(EventType.info, ACCOUNT, SELECT_SUBSCRIPTIONS, null);
        TableItem[] tia = table.getItems();
        int chekedCount = 0;
        for (TableItem ti : tia) {
            if (ti.getChecked()) {
                chekedCount++;
            }
        }

        if (chekedCount == 0) {
            this.setErrorMessage("Select at least one subscription");
            return;
        }

        for (int i = 0; i < tia.length; ++i) {
            this.sdl.get(i).setSelected(tia[i].getChecked());
        }

        try {
            subscriptionManager.setSubscriptionDetails(sdl);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "okPressed@SubscriptionDialog", ex));
        }

        super.okPressed();
    }
}
