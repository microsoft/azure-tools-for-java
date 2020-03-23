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

package com.microsoft.applicationinsights.preference;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.applicationinsights.ui.activator.Activator;
import com.microsoft.applicationinsights.ui.config.AIResourceChangeListener;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.applicationinsights.ApplicationInsightsPreferences;
import com.microsoft.azuretools.core.applicationinsights.ApplicationInsightsResourceRegistryEclipse;
import com.microsoft.azuretools.core.handlers.SelectSubsriptionsCommandHandler;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.handlers.SignOutCommandHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;

/**
 * Class for Application Insights preference page. Creates UI components and their listeners.
 */
public class ApplicationInsightsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    private static final String ICON_SIGNIN = "/icons/SignInLight_16.png";
    private static final String ICON_SIGNOUT = "/icons/SignOutLight_16.png";
    private static final String ICON_SELECT_SUBSCRIPTIONS = "/icons/ConnectAccountsLight_16.png";
    private Button signInOutBtn;
    private Button selectSubscriptionsBtn;
    private Table table;
    private TableViewer tableViewer;
    private Button btnNew;
    private Button btnAdd;
    private Button btnDetails;
    private Button btnRemove;
    public static int selIndex = -1;

    @Override
    public void init(IWorkbench arg0) {
    }

    @Override
    protected Control createContents(Composite parent) {
        noDefaultAndApplyButton();
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(gridLayout);
        composite.setLayoutData(gridData);
        createBtnCmpnt(composite);
        createApplicationInsightsResourceTable(composite);
        return null;
    }

    /**
     * createBtnCmpnt.
     */
    public void createBtnCmpnt(Composite parent) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.LEFT;
        gridData.horizontalSpan = 2;
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);
        signInOutBtn = new Button(container, SWT.PUSH | SWT.LEFT);
        gridData = new GridData();
        gridData.horizontalIndent = 3;
        boolean isSignedIn = false;
        try {
            isSignedIn = AuthMethodManager.getInstance().isSignedIn();
        } catch (Exception e) {
            // ignore
        }
        signInOutBtn.setImage(PluginUtil.getImage(isSignedIn ? ICON_SIGNOUT : ICON_SIGNIN));
        signInOutBtn.setToolTipText(isSignedIn ? "Sign Out" : "Sign In");
        signInOutBtn.setLayoutData(gridData);
        signInOutBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                signInOutBtnListener();
            }
        });
        selectSubscriptionsBtn = new Button(container, SWT.PUSH | SWT.LEFT);
        gridData = new GridData();
        gridData.horizontalIndent = 3;
        selectSubscriptionsBtn.setImage(PluginUtil.getImage(ICON_SELECT_SUBSCRIPTIONS));
        selectSubscriptionsBtn.setToolTipText("Select Subscriptions");
        selectSubscriptionsBtn.setLayoutData(gridData);
        selectSubscriptionsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                selectSubscriptionListener();
            }
        });
    }

    /**
     * createApplicationInsightsResourceTable.
     */
    public void createApplicationInsightsResourceTable(Composite parent) {
        table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        GridData gridData = new GridData();
        gridData.heightHint = 360;
        gridData.verticalIndent = 15;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = false;
        table.setLayoutData(gridData);

        TableColumn strNameCol = new TableColumn(table, SWT.FILL);
        strNameCol.setText(Messages.resrcName);
        strNameCol.setWidth(150);

        TableColumn strUrlDisCol = new TableColumn(table, SWT.FILL);
        strUrlDisCol.setText(Messages.instrKey);
        strUrlDisCol.setWidth(190);

        tableViewer = new TableViewer(table);
        tableViewer.setContentProvider(new IStructuredContentProvider() {

            @Override
            public void inputChanged(Viewer viewer, Object obj, Object obj1) {
            }

            @Override
            public void dispose() {
            }

            @Override
            public Object[] getElements(Object obj) {
                return getTableContent();
            }
        });

        tableViewer.setLabelProvider(new ITableLabelProvider() {

            @Override
            public void removeListener(ILabelProviderListener ilabelproviderlistener) {
            }

            @Override
            public boolean isLabelProperty(Object element, String s) {
                return false;
            }

            @Override
            public void dispose() {
            }

            @Override
            public void addListener(ILabelProviderListener ilabelproviderlistener) {
            }

            @Override
            public String getColumnText(Object element, int i) {
                ApplicationInsightsPageTableElement rowElement = (ApplicationInsightsPageTableElement) element;
                String result = "";
                switch (i) {
                    case 0:
                        result = rowElement.getResourceName();
                        break;

                    case 1:
                        result = rowElement.getInstrumentationKey();
                        break;

                    default:
                        break;
                }
                return result;
            }

            @Override
            public Image getColumnImage(Object element, int i) {
                return null;
            }
        });

        tableViewer.setInput(getTableContent());

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.END;
        gridData.verticalAlignment = GridData.BEGINNING;
        gridData.verticalIndent = 15;
        Composite containerButtons = new Composite(parent, SWT.NONE);
        containerButtons.setLayout(gridLayout);
        containerButtons.setLayoutData(gridData);

        btnNew = new Button(containerButtons, SWT.PUSH);
        btnNew.setText(Messages.btnNewLbl);
        gridData = new GridData();
        gridData.widthHint = 70;
        btnNew.setLayoutData(gridData);
        btnNew.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                newButtonListener();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });

        btnAdd = new Button(containerButtons, SWT.PUSH);
        btnAdd.setText(Messages.btnAddLbl);
        gridData = new GridData();
        gridData.widthHint = 70;
        btnAdd.setLayoutData(gridData);
        btnAdd.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                addButtonListener();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });

        btnDetails = new Button(containerButtons, SWT.PUSH);
        btnDetails.setEnabled(false);
        btnDetails.setText(Messages.btnDtlsLbl);
        gridData = new GridData();
        gridData.widthHint = 70;
        btnDetails.setLayoutData(gridData);
        btnDetails.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                detailsButtonListener();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });

        btnRemove = new Button(containerButtons, SWT.PUSH);
        btnRemove.setEnabled(false);
        btnRemove.setText(Messages.btnRmvLbl);
        gridData = new GridData();
        gridData.widthHint = 70;
        btnRemove.setLayoutData(gridData);
        btnRemove.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                removeButtonListener();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });

        table.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                selIndex = tableViewer.getTable().getSelectionIndex();
                btnDetails.setEnabled(true);
                btnRemove.setEnabled(true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent selectionchangedevent) {
                if (selectionchangedevent.getSelection().isEmpty()) {
                    btnDetails.setEnabled(false);
                    btnRemove.setEnabled(false);
                }
            }
        });
    }

    private void signInOutBtnListener() {
        try {
            AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
            boolean isSignedIn = authMethodManager.isSignedIn();
            if (isSignedIn) {
                SignOutCommandHandler.doSignOut(PluginUtil.getParentShell());
            } else {
                SignInCommandHandler.doSignIn(PluginUtil.getParentShell());
            }
            signInOutBtn.setImage(PluginUtil.getImage(authMethodManager.isSignedIn() ? ICON_SIGNOUT : ICON_SIGNIN));
            tableViewer.refresh();
        } catch (Exception ex) {
            Activator.getDefault().log(ex.getMessage(), ex);
        }
    }

    private void selectSubscriptionListener() {
        // selectSubscriptionAction = new Action("Select Subscriptions",
        // com.microsoft.azuretools.core.Activator.getImageDescriptor("icons/ConnectAccountsLight_16.png")) {
        // public void run() {
        try {
            if (AuthMethodManager.getInstance().isSignedIn()) {
                SelectSubsriptionsCommandHandler.onSelectSubscriptions(PluginUtil.getParentShell());
                tableViewer.refresh();
            }
        } catch (Exception ex) {
            Activator.getDefault().log(ex.getMessage(), ex);
        }
    }

    /**
     * Method opens dialog to create new application insights resource.
     */
    protected void newButtonListener() {
        createNewDilaog();
    }

    /**
     * createNewDilaog.
     */
    public void createNewDilaog() {
        ApplicationInsightsNewDialog dialog = new ApplicationInsightsNewDialog(getShell());
        dialog.setOnCreate(new Runnable() {

            @Override
            public void run() {
                ApplicationInsightsResource resource = ApplicationInsightsNewDialog.getResource();
                if (resource != null
                        && !ApplicationInsightsResourceRegistry.getAppInsightsResrcList().contains(resource)) {
                    ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resource);
                    ApplicationInsightsPreferences.save();
                }
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tableViewer.refresh();
                    }
                });
            }
        });
        dialog.open();
    }

    /**
     * Method opens dialog to add existing application insights resource in list.
     */
    protected void addButtonListener() {
        ApplicationInsightsAddDialog dialog = new ApplicationInsightsAddDialog(getShell());
        dialog.open();
        tableViewer.refresh();
    }

    /**
     * Method opens dialog to show details of application insights resource.
     */
    protected void detailsButtonListener() {
        int index = tableViewer.getTable().getSelectionIndex();
        ApplicationInsightsResource resource = ApplicationInsightsResourceRegistry.getAppInsightsResrcList().get(index);
        ApplicationInsightsDetailsDialog dialog = new ApplicationInsightsDetailsDialog(getShell(), resource);
        dialog.open();
    }

    /**
     * Method removes application insight resource from local cache.
     */
    protected void removeButtonListener() {
        int curSelIndex = tableViewer.getTable().getSelectionIndex();
        if (curSelIndex > -1) {
            String keyToRemove = ApplicationInsightsResourceRegistry.getKeyAsPerIndex(curSelIndex);
            String projName = AIResourceChangeListener.getProjectNameAsPerKey(keyToRemove);
            if (projName != null && !projName.isEmpty()) {
                PluginUtil.displayErrorDialog(getShell(), Messages.appTtl,
                        String.format(Messages.rsrcUseMsg, projName));
            } else {
                boolean choice = MessageDialog.openConfirm(getShell(), Messages.appTtl, Messages.rsrcRmvMsg);
                if (choice) {
                    ApplicationInsightsResourceRegistry.getAppInsightsResrcList().remove(curSelIndex);
                    ApplicationInsightsPreferences.save();
                    tableViewer.refresh();
                    selIndex = -1;
                }
            }
        }
    }

    private Object[] getTableContent() {
        ApplicationInsightsPreferences.load();
        ApplicationInsightsPageTableElements elements = getPrefPageTableElements();
        return elements.getElements().toArray();
    }

    /**
     * getPrefPageTableElements.
     */
    public static ApplicationInsightsPageTableElements getPrefPageTableElements() {
        List<ApplicationInsightsResource> resourceList = ApplicationInsightsResourceRegistry.getAppInsightsResrcList();
        List<ApplicationInsightsPageTableElement> tableRowElements = new ArrayList<>();
        for (ApplicationInsightsResource resource : resourceList) {
            if (resource != null) {
                ApplicationInsightsPageTableElement ele = new ApplicationInsightsPageTableElement();
                ele.setResourceName(resource.getResourceName());
                ele.setInstrumentationKey(resource.getInstrumentationKey());
                tableRowElements.add(ele);
            }
        }
        ApplicationInsightsPageTableElements elements = new ApplicationInsightsPageTableElements();
        elements.setElements(tableRowElements);
        return elements;
    }

    @Override
    public boolean performCancel() {
        /*
         * Do not remember selection index if cancel is pressed.
         */
        selIndex = -1;
        return true;
    }

    public static int getSelIndex() {
        return selIndex;
    }

    @Override
    public void setVisible(boolean visible) {
        // reload information if its new eclipse session.
        loadInfoFirstTime();
        selIndex = -1;
        tableViewer.refresh();
        super.setVisible(visible);
    }

    private void loadInfoFirstTime() {
        try {
            if (AuthMethodManager.getInstance().isSignedIn()) {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                List<SubscriptionDetail> subList = azureManager.getSubscriptionManager().getSubscriptionDetails();
                if (subList.size() > 0) {
                    // if (!ApplicationInsightsPreferences.isLoaded()) {
                    // authenticated using AD. Proceed for updating application insights registry.
                    ApplicationInsightsResourceRegistryEclipse.updateApplicationInsightsResourceRegistry(subList);
                } else {
                    ApplicationInsightsResourceRegistryEclipse.keeepManuallyAddedList();
                }
                // } else {
                // // show list from preferences - getTableContent() does it. So nothing to handle here
                // }
            } else {
                // just show manually added list from preferences
                ApplicationInsightsResourceRegistryEclipse.keeepManuallyAddedList();
            }
        } catch (Exception ex) {
            Activator.getDefault().log(Messages.importErrMsg, ex);
        }
    }

    private void createSubscriptionDialog(boolean invokeSignIn) {
        /*
         * try { ManageSubscriptionDialog dialog = new ManageSubscriptionDialog(getShell(), false, invokeSignIn);
         * dialog.create(); dialog.open(); List<Subscription> subList =
         * AzureManagerImpl.getManager().getSubscriptionList(); if (subList.size() == 0) {
         * ApplicationInsightsResourceRegistryEclipse.keeepManuallyAddedList(); } else {
         * ApplicationInsightsResourceRegistryEclipse.updateApplicationInsightsResourceRegistry(subList); }
         * tableViewer.refresh(); } catch(Exception ex) { PluginUtil.displayErrorDialogWithAzureMsg(getShell(),
         * Messages.appTtl, Messages.importErrMsg, ex); }
         */
    }
}
