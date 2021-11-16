/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.appservice.ui;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_WEBAPP_SLOT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.DEPLOY_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.OPEN_CREATEWEBAPP_DIALOG;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.REFRESH_METADATA;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.eclipse.common.component.EclipseProjectComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServiceBaseEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.core.actions.MavenExecuteAction;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.core.ui.ErrorWindow;
import com.microsoft.azuretools.core.ui.views.AzureDeploymentProgressNotification;
import com.microsoft.azuretools.core.utils.AccessibilityUtils;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.core.utils.ProgressDialog;
import com.microsoft.azuretools.core.utils.UpdateProgressIndicator;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.appservice.Activator;
import com.microsoft.azuretools.appservice.util.CommonUtils;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@SuppressWarnings("restriction")
public class WebAppDeployDialog extends AppServiceBaseDialog {

    private static ILog LOG = Activator.getDefault().getLog();
    private static final String REFRESHING = "Refreshing...";
    private static int connection_read_timeout_ms = 10000;

    private Table table;
    private Link browserAppServiceDetails;
    private Button btnDeployToRoot;
    private Button btnDelete;
    private Button btnDeployToSlot;
    private Combo comboSlot;
    private Combo comboSlotConf;
    private Button btnSlotUseExisting;
    private Button btnSlotCreateNew;
    private Text textSlotName;
    private Label lblSlotConf;
    private Button buildBeforeDeploy;

    private EclipseProjectComboBox projectCombo;
    private ControlDecoration decProjectCombo;
    private ControlDecoration decComboSlotConf;
    private ControlDecoration decComboSlot;
    private ControlDecoration decTextSlotName;

    private IProject project;
    private Shell parentShell;

    private static final String ftpLinkString = "ShowFtpCredentials";
    private static final String DATE_FORMAT = "yyMMddHHmmss";
    private static final String date = new SimpleDateFormat(DATE_FORMAT).format(new Date());

    private static final String DONOT_CLONE_SLOT_CONF = "Do not clone settings";
    private static final String SLOT_NAME_REGEX = "[a-zA-Z0-9-]{1,60}";
    private static final String NAME_ALREADY_TAKEN = "The name is already taken";
    private static final String ENTER_VALID_SLOT_NAME = "Enter a valid slot name.";
    private static final String SELECT_SLOT_NAME = "Select a valid slot name.";
    private static final String SELECT_SLOT_CLONE_SETTING = "Select a valid slot clone settings";
    private static final String INVALID_SLOT_NAME = "The slot name is invalid, it needs to match the pattern "
            + SLOT_NAME_REGEX;
    private static final String DEPLOYMENT_SLOT_HOVER = "Deployment slots are live apps with their own hostnames. App"
            + " content and configurations elements can be swapped between two deployment slots, including the production "
            + "slot.";

    private Map<String, IWebApp> webAppDetailsMap = new HashMap<>();
    private WebAppSettingModel webAppSettingModel;
    private boolean isDeployToSlot = false;
    private boolean isCreateNewSlot = false;

    /**
     * Create the dialog.
     *
     * @param parentShell
     */
    private WebAppDeployDialog(Shell parentShell, IProject project, String id) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
        this.project = project;
        this.parentShell = parentShell;

        // workaround to select web app by default
        Optional.ofNullable(id).map(resourceId -> ResourceId.fromString(resourceId).name())
                .ifPresent(name -> CommonUtils.setPreference(CommonUtils.WEBAPP_NAME, name));
    }

    public static WebAppDeployDialog go(Shell parentShell, IProject project, String id) {
        WebAppDeployDialog d = new WebAppDeployDialog(parentShell, project, id);
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        Image image = PluginUtil.getImage("icons/large/DeploytoAzureWizard.png");
        if (image != null) {
            setTitleImage(image);
        }
    }

    /**
     * Create contents of the dialog.
     *
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Select App Service to deploy to:");
        setTitle("Deploy Web App");

        ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolledComposite.setLayout(new GridLayout(2, false));
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Group container = new Group(scrolledComposite, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        GridData gdContainer = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gdContainer.widthHint = 750;
        gdContainer.heightHint = 1000;
        container.setLayoutData(gdContainer);

        createProjectComposite(container);
        new Label(container, SWT.NONE);
        createAppGroup(container);
        createButton(container);
        createAppDetailGroup(container);
        new Label(container, SWT.NONE);
        createSlotGroup(container);

        scrolledComposite.setContent(container);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return scrolledComposite;
    }

    private void createProjectComposite(Composite container) {
        Composite composite = new Composite(container, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        composite.setLayout(new GridLayout(2, false));

        Label projectLabel = new Label(composite, SWT.NONE);
        projectLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        projectLabel.setText("Project: ");
        projectLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        projectCombo = new EclipseProjectComboBox(composite);
        projectCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        projectCombo.addValueChangedListener(value -> {
            this.project = value;
            if (!projectCombo.isDisposed()) {
                this.buildBeforeDeploy.setVisible(MavenUtils.isMavenProject(value));
            }
        });
        Optional.ofNullable(project)
                .ifPresent(value -> projectCombo.setValue(new ItemReference<>(item -> Objects.equals(value, item))));
        projectCombo.refreshItems();
        decProjectCombo = decorateContorolAndRegister(projectCombo);
    }

    private void createAppGroup(Composite container) {
        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gridTable = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        gridTable.heightHint = 250;
        table.setLayoutData(gridTable);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.addListener(SWT.Selection, (e) -> {
            if (table.getSelectionCount() == 1 && table.getSelection()[0].getText() == REFRESHING) {
                return;
            }
            fillAppServiceDetails();
            fillSlot();
        });
        AccessibilityUtils.addAccessibilityNameForUIComponent(table, "App service");

        TableColumn tblclmnName = new TableColumn(table, SWT.LEFT);
        tblclmnName.setWidth(200);
        tblclmnName.setText("Name");

        TableColumn tblclmnJdk = new TableColumn(table, SWT.LEFT);
        tblclmnJdk.setWidth(80);
        tblclmnJdk.setText("JDK");

        TableColumn tblclmnWebContainer = new TableColumn(table, SWT.LEFT);
        tblclmnWebContainer.setWidth(120);
        tblclmnWebContainer.setText("Web container");

        TableColumn tblclmnResourceGroup = new TableColumn(table, SWT.LEFT);
        tblclmnResourceGroup.setWidth(180);
        tblclmnResourceGroup.setText("Resource group");
    }

    private void createButton(Composite container) {
        Composite composite = new Composite(container, SWT.NONE);
        composite.setLayout(new RowLayout(SWT.VERTICAL));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Button btnCreate = new Button(composite, SWT.NONE);
        btnCreate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendTelemetry("CREATE");
                EventUtil.logEvent(EventType.info, WEBAPP, OPEN_CREATEWEBAPP_DIALOG, buildProperties());
                createAppService(project);
            }
        });
        btnCreate.setText("Create...");

        btnDelete = new Button(composite, SWT.NONE);
        btnDelete.setEnabled(false);
        btnDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendTelemetry("DELETE");
                deleteAppService();
            }
        });
        btnDelete.setText("Delete...");

        Button btnRefresh = new Button(composite, SWT.NONE);
        btnRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendTelemetry("REFRESH");
                EventUtil.executeWithLog(WEBAPP, REFRESH_METADATA, (operation) -> {
                    table.removeAll();
                    fillAppServiceDetails();
                    doFillTable(true);
                });
            }
        });
        btnRefresh.setText("Refresh");

        btnDeployToRoot = new Button(composite, SWT.CHECK);
        btnDeployToRoot.setSelection(true);
        btnDeployToRoot.setText("Deploy to root");

        buildBeforeDeploy = new Button(composite, SWT.CHECK);
        buildBeforeDeploy.setText("Build project");
        buildBeforeDeploy.setSelection(true);

        int size = btnDeployToRoot.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        btnCreate.setLayoutData(new RowData(size, SWT.DEFAULT));
        btnDelete.setLayoutData(new RowData(size, SWT.DEFAULT));
        btnRefresh.setLayoutData(new RowData(size, SWT.DEFAULT));
        buildBeforeDeploy.setLayoutData(new RowData(size, SWT.DEFAULT));
        btnDeployToRoot.setLayoutData(new RowData(size, SWT.DEFAULT));
    }

    private void createAppDetailGroup(Composite container) {
        Group grpAppServiceDetails = new Group(container, SWT.NONE);
        grpAppServiceDetails.setLayout(new FillLayout(SWT.HORIZONTAL));
        GridData gdGrpAppServiceDetails = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gdGrpAppServiceDetails.heightHint = 150;
        grpAppServiceDetails.setLayoutData(gdGrpAppServiceDetails);
        grpAppServiceDetails.setText("App service details");

        browserAppServiceDetails = new Link(grpAppServiceDetails, SWT.MULTI | SWT.FULL_SELECTION);
        browserAppServiceDetails.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    if (e.text.equals(ftpLinkString)) {
                        showFtpCreadentialsWindow();
                    } else if (e.text.contains("http")) {
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "changing@LocationListener@browserAppServiceDetails@AppServiceCreateDialog", ex));
                }
            }
        });

        try {
            if (MavenUtils.isMavenProject(project) && MavenUtils.getPackaging(project).equals("jar")) {
                btnDeployToRoot.setSelection(true);
                btnDeployToRoot.setVisible(false);
                ((RowData) btnDeployToRoot.getLayoutData()).exclude = true;
            }
        } catch (Exception e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "WebAppDeployDialog", e));
            e.printStackTrace();
        }
    }

    private void createSlotGroup(Composite container) {
        ScrolledComposite scrolledComposite = new ScrolledComposite(container, SWT.V_SCROLL);
        scrolledComposite.setLayout(new FillLayout(SWT.HORIZONTAL));
        GridData gdGrpSlot = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gdGrpSlot.heightHint = 140;
        scrolledComposite.setLayoutData(gdGrpSlot);

        Group grpSlot = new Group(scrolledComposite, SWT.NONE);
        grpSlot.setLayout(new FillLayout(SWT.HORIZONTAL));
        grpSlot.setLayoutData(gdGrpSlot);
        grpSlot.setText("Deployment Slot");
        Composite compositeSlot = new Composite(grpSlot, SWT.NONE);
        compositeSlot.setLayout(new GridLayout(2, false));

        RowLayout rowLayout = new RowLayout();
        rowLayout.marginLeft = 0;
        rowLayout.marginTop = 0;
        rowLayout.marginRight = 0;
        rowLayout.marginBottom = 0;
        Composite compositeSlotCb = new Composite(compositeSlot, SWT.LEFT);
        compositeSlotCb.setLayout(rowLayout);

        btnDeployToSlot = new Button(compositeSlotCb, SWT.CHECK);
        btnDeployToSlot.setSelection(false);
        btnDeployToSlot.setText("Deploy to Slot");
        btnDeployToSlot.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioSlotLogic();
                fillSlot();
            }
        });

        DefaultToolTip iconTooltip = new DefaultToolTip(btnDeployToSlot, SWT.NONE, false);
        iconTooltip.setText(DEPLOYMENT_SLOT_HOVER);
        btnDeployToSlot.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                iconTooltip.show(new Point(btnDeployToSlot.getSize().x, 0));
            }

            @Override
            public void focusLost(FocusEvent e) {
                iconTooltip.hide();
            }
        });

        new Label(compositeSlot, SWT.NONE);

        btnSlotUseExisting = new Button(compositeSlot, SWT.RADIO);
        btnSlotUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioSlotLogic();
            }
        });

        btnSlotUseExisting.setSelection(true);
        btnSlotUseExisting.setText("Use existing");
        comboSlot = new Combo(compositeSlot, SWT.READ_ONLY);
        AccessibilityUtils.addAccessibilityNameForUIComponent(comboSlot, "Existing deployment slot");
        comboSlot.setEnabled(false);
        comboSlot.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboSlot.setBounds(0, 0, 26, 22);
        comboSlot.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });

        decComboSlot = decorateContorolAndRegister(comboSlot);

        btnSlotCreateNew = new Button(compositeSlot, SWT.RADIO);
        btnSlotCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioSlotLogic();
            }
        });
        btnSlotCreateNew.setText("Create new");

        textSlotName = new Text(compositeSlot, SWT.BORDER);
        textSlotName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        AccessibilityUtils.addAccessibilityNameForUIComponent(textSlotName, "New depoyment slot");
        textSlotName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textSlotName.setBounds(0, 0, 64, 19);
        textSlotName.setMessage("Slot Name");
        textSlotName.setText("slot-" + date);
        textSlotName.setEnabled(false);
        decTextSlotName = decorateContorolAndRegister(textSlotName);

        lblSlotConf = new Label(compositeSlot, SWT.NONE);
        lblSlotConf.setEnabled(false);
        GridData gdLblSlotConf = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdLblSlotConf.horizontalIndent = 20;
        lblSlotConf.setLayoutData(gdLblSlotConf);
        lblSlotConf.setText("Clone settings from");

        comboSlotConf = new Combo(compositeSlot, SWT.READ_ONLY);
        comboSlotConf.setEnabled(false);
        comboSlotConf.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboSlotConf.setBounds(0, 0, 26, 22);
        comboSlotConf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        AccessibilityUtils.addAccessibilityNameForUIComponent(comboSlotConf, "Deployment slot configuration source");
        decComboSlotConf = decorateContorolAndRegister(comboSlotConf);

        scrolledComposite.setContent(grpSlot);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setMinSize(grpSlot.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        fillSlot();
        radioSlotLogic();
    }

    @Override
    public void create() {
        super.create();
        Display.getDefault().asyncExec(() -> {
            doFillTable(false);
        });
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText("Deploy");
        okButton.setEnabled(false);
    }

    private void showFtpCreadentialsWindow() {
        int selectedRow = table.getSelectionIndex();
        if (selectedRow < 0) {
            return;
        }
        String appServiceName = table.getItems()[selectedRow].getText(0);
        IWebApp webApp = webAppDetailsMap.get(appServiceName);
        FtpCredentialsWindow w = new FtpCredentialsWindow(getShell(), webApp);
        w.open();
    }

    private void fillAppServiceDetails() {
        validated();
        int selectedRow = table.getSelectionIndex();
        if (selectedRow < 0) {
            browserAppServiceDetails.setText("");
            btnDelete.setEnabled(false);
            return;
        }

        btnDelete.setEnabled(true);
        browserAppServiceDetails.setText("Fetching app service information");

        final String appServiceName = table.getItems()[selectedRow].getText(0);
        final IWebApp webApp = webAppDetailsMap.get(appServiceName);
        Mono.fromCallable(() -> {
            IAppServicePlan asp = webApp.plan();
            Subscription subscription = Azure.az(AzureAccount.class).account().getSubscription(webApp.subscriptionId());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("App Service name: %s \n", webApp.name()));
            sb.append(String.format("Subscription name: %s ; id: %s \n", subscription.getName(), subscription.getId()));
            String aspName = asp == null ? "N/A" : asp.name();
            String aspPricingTier = asp == null ? "N/A" : asp.entity().getPricingTier().toString();
            sb.append(String.format("App Service Plan name: %s ; Pricing tier: %s \n", aspName, aspPricingTier));
            String link = buildSiteLink(webApp, null);
            sb.append(String.format("Link: <a href=\"%s\">%s</a> \n", link, link));
            sb.append(String.format("<a href=\"%s\">%s</a> \n", ftpLinkString, "Show FTP deployment credentials"));
            return sb.toString();
        }).subscribeOn(Schedulers.boundedElastic()).subscribe(content -> AzureTaskManager.getInstance().runLater(() -> {
            if (browserAppServiceDetails.isDisposed()) {
                return;
            }
            browserAppServiceDetails.setText(content);
        }));
    }

    private static String buildSiteLink(IWebAppBase<? extends AppServiceBaseEntity> webApp, String artifactName) {
        String appServiceLink = "https://" + webApp.hostName();
        if (artifactName != null && !artifactName.isEmpty()) {
            return appServiceLink + "/" + artifactName;
        } else {
            return appServiceLink;
        }
    }

    private void doFillTable(boolean forceRefresh) {
        try {
            webAppDetailsMap.clear();
            table.removeAll();
            TableItem refreshingItem = new TableItem(table, SWT.NULL);
            refreshingItem.setText(REFRESHING);
            Mono.fromCallable(() -> {
                return Azure.az(AzureAppService.class).webapps(forceRefresh).stream().parallel()
                        .filter(webApp -> !webApp.getRuntime().isDocker()
                                && !Objects.equals(webApp.getRuntime().getJavaVersion(), JavaVersion.OFF))
                        .sorted((o1, o2) -> o1.name().compareTo(o2.name())).collect(Collectors.toList());
            }).subscribeOn(Schedulers.boundedElastic()).subscribe(webAppDetailsList -> {
                AzureTaskManager.getInstance().runLater(() -> {
                    if (table.isDisposed()) {
                        return;
                    }
                    table.removeAll();
                    for (IWebApp webApp : webAppDetailsList) {
                        TableItem item = new TableItem(table, SWT.NULL);
                        item.setText(new String[] { webApp.name(), webApp.getRuntime().getWebContainer().getValue(),
                                webApp.getRuntime().getJavaVersion().getValue(), webApp.resourceGroup() });
                        webAppDetailsMap.put(webApp.name(), webApp);
                    }
                    fillUserSettings();
                });
            });
        } catch (Exception e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "updateAndFillTable@AppServiceCreateDialog", e));
        }
    }

    private void fillSlot() {
        if (!isDeployToSlot) {
            return;
        }
        int selectedRow = table.getSelectionIndex();
        comboSlot.removeAll();
        comboSlotConf.removeAll();
        if (selectedRow < 0) {
            return;
        }
        refreshComboBox(comboSlot);
        refreshComboBox(comboSlotConf);
        String appServiceName = table.getItems()[selectedRow].getText(0);
        IWebApp webApp = webAppDetailsMap.get(appServiceName);
        if (webApp == null) {
            return;
        }
        Mono.fromCallable(() -> webApp.deploymentSlots(false)).subscribeOn(Schedulers.boundedElastic())
                .subscribe(slots -> {
                    AzureTaskManager.getInstance().runLater(() -> {
                        if (comboSlot.isDisposed()) {
                            return;
                        }
                        comboSlot.removeAll();
                        comboSlot.setEnabled(btnSlotUseExisting.getSelection());
                        comboSlotConf.removeAll();
                        comboSlotConf.setEnabled(btnSlotCreateNew.getSelection());
                        for (IWebAppDeploymentSlot deploymentSlot : slots) {
                            comboSlot.add(deploymentSlot.name());
                            comboSlotConf.add(deploymentSlot.name());
                        }
                        if (comboSlot.getItemCount() > 0) {
                            comboSlot.select(0);
                        }
                        comboSlotConf.add(webApp.name());
                        comboSlotConf.add(DONOT_CLONE_SLOT_CONF);
                        comboSlotConf.select(0);
                    });
                });
    }

    private void refreshComboBox(Combo target) {
        target.setEnabled(false);
        target.removeAll();
        target.add(REFRESHING);
        target.select(0);
    }

    private void fillUserSettings() {
        try {
            selectTableRowWithWebAppName(CommonUtils.getPreference(CommonUtils.WEBAPP_NAME));
            fillAppServiceDetails();
            String slotName = CommonUtils.getPreference(CommonUtils.SLOT_NAME);
            String slotConf = CommonUtils.getPreference(CommonUtils.SLOT_CONF);
            CommonUtils.selectComboIndex(comboSlot, slotName);
            CommonUtils.selectComboIndex(comboSlotConf, slotConf);
        } catch (Exception ignore) {
        }
    }

    private void recordUserSettings() {
        try {
            int selectedRow = table.getSelectionIndex();
            String appServiceName = table.getItems()[selectedRow].getText(0);
            CommonUtils.setPreference(CommonUtils.WEBAPP_NAME, appServiceName);

            if (isDeployToSlot) {
                if (isCreateNewSlot) {
                    CommonUtils.setPreference(CommonUtils.SLOT_CONF,
                            webAppSettingModel.getNewSlotConfigurationSource());
                    CommonUtils.setPreference(CommonUtils.SLOT_NAME, webAppSettingModel.getNewSlotName());
                } else {
                    CommonUtils.setPreference(CommonUtils.SLOT_NAME, webAppSettingModel.getSlotName());
                }
            }
        } catch (Exception ignore) {
        }
    }

    private boolean validate() {
        cleanError();
        if (project == null) {
            setError(decProjectCombo, "Please select target project to deploy");
            return false;
        }
        if (!isDeployToSlot) {
            return true;
        }
        if (isCreateNewSlot) {
            String slotName = webAppSettingModel.getNewSlotName();
            if (StringUtils.isNullOrWhiteSpace(slotName)) {
                setError(decTextSlotName, ENTER_VALID_SLOT_NAME);
                return false;
            }
            if (!slotName.matches(SLOT_NAME_REGEX)) {
                setError(decTextSlotName, INVALID_SLOT_NAME);
                return false;
            }
            for (String slot : comboSlot.getItems()) {
                if (slotName.equals(slot)) {
                    setError(decTextSlotName, NAME_ALREADY_TAKEN);
                    return false;
                }
            }
            if (StringUtils.isNullOrWhiteSpace(webAppSettingModel.getNewSlotConfigurationSource())) {
                setError(decComboSlotConf, SELECT_SLOT_CLONE_SETTING);
                return false;
            }
        } else {
            if (StringUtils.isNullOrWhiteSpace(webAppSettingModel.getSlotName())) {
                setError(decComboSlot, SELECT_SLOT_NAME);
                return false;
            }
        }
        return true;
    }

    private void radioSlotLogic() {
        cleanError();
        boolean enable = btnDeployToSlot.getSelection();
        boolean enableUseExisting = btnSlotUseExisting.getSelection();

        btnSlotUseExisting.setEnabled(enable);
        btnSlotCreateNew.setEnabled(enable);
        comboSlot.setEnabled(enable && enableUseExisting);
        comboSlotConf.setEnabled(enable && !enableUseExisting);
        textSlotName.setEnabled(enable && !enableUseExisting);
        lblSlotConf.setEnabled(enable && !enableUseExisting);

        isDeployToSlot = enable;
        isCreateNewSlot = btnSlotCreateNew.getSelection();
    }

    private void createAppService(IProject project) {
        AppServiceCreateDialog d = AppServiceCreateDialog.go(getShell(), project);
        if (d == null) {
            // something went wrong - report an error!
            return;
        }
        IWebApp webApp = d.getWebApp();
        CommonUtils.setPreference(CommonUtils.WEBAPP_NAME, webApp.name());
        doFillTable(true);
    }

    private boolean validated() {
        setErrorMessage(null);
        int selectedRow = table.getSelectionIndex();
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (selectedRow < 0) {
            okButton.setEnabled(false);
            return false;
        }
        String appServiceName = table.getItems()[selectedRow].getText(0);
        IWebApp wad = webAppDetailsMap.get(appServiceName);
        if (wad != null && Objects.equals(wad.getRuntime().getJavaVersion(), JavaVersion.OFF)) {
            setErrorMessage("Select java based App Service");
            okButton.setEnabled(false);
            return false;
        }
        okButton.setEnabled(true);
        return true;
    }

    @Override
    protected void okPressed() {
        try {
            String artifactName;
            String destinationPath;
            if (MavenUtils.isMavenProject(project)) {
                artifactName = MavenUtils.getFinalName(project);
                destinationPath = MavenUtils.getTargetPath(project);
            } else {
                artifactName = project.getName();
                destinationPath = project.getLocation() + "/" + artifactName + ".war";
            }
            collectData();
            recordUserSettings();
            if (!validate()) {
                return;
            }
            deploy(artifactName, destinationPath);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "okPressed@AppServiceCreateDialog", ex));
        }
        super.okPressed();
    }

    private void export(String projectName, String destinationPath) throws Exception {

        System.out.println("Building project '" + projectName + "'...");
        project.build(IncrementalProjectBuilder.FULL_BUILD, null);

        System.out.println("Exporting to WAR...");
        IDataModel dataModel = DataModelFactory.createDataModel(new WebComponentExportDataModelProvider());
        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.PROJECT_NAME, projectName);
        dataModel.setProperty(IJ2EEComponentExportDataModelProperties.ARCHIVE_DESTINATION, destinationPath);

        dataModel.getDefaultOperation().execute(null, null);
        System.out.println("Done.");
    }

    private void selectTableRowWithWebAppName(String webAppName) {
        for (int ri = 0; ri < table.getItemCount(); ++ri) {
            String waName = table.getItem(ri).getText(0);
            if (waName.equals(webAppName)) {
                table.select(ri);
                break;
            }
        }
    }

    private void deploy(String artifactName, String artifactPath) {
        int selectedRow = table.getSelectionIndex();
        String appServiceName = table.getItems()[selectedRow].getText(0);
        IWebApp webApp = webAppDetailsMap.get(appServiceName);
        String jobDescription = String.format("Web App '%s' deployment", webApp.name());
        if (isDeployToSlot) {
            jobDescription = String.format("Web App '%s' deploy to slot '%s'", webApp.name(),
                    isCreateNewSlot ? webAppSettingModel.getNewSlotName() : webAppSettingModel.getSlotName());
        }
        String deploymentName = UUID.randomUUID().toString();
        AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(deploymentName, jobDescription);
        boolean isDeployToRoot = btnDeployToRoot.getSelection();
        boolean isBuildBeforeDeploy = buildBeforeDeploy.getSelection();

        Job job = new Job(jobDescription) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                String message = "Packaging Artifact...";
                String cancelMessage = "Interrupted by user";
                String successMessage = "";
                String errorMessage = "Error";
                Map<String, String> postEventProperties = new HashMap<>();
                try {
                    boolean isJar = MavenUtils.isMavenProject(project)
                            && MavenUtils.getPackaging(project).equals("jar");
                    postEventProperties.put(TelemetryConstants.JAVA_APPNAME, project.getName());
                    postEventProperties.put(TelemetryConstants.FILETYPE, isJar ? "jar" : "war");
                    Runtime runtime = webApp.getRuntime();
                    String osValue = (String) Optional.ofNullable(runtime.getOperatingSystem())
                            .map(com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem::toString).orElse("");
                    String webContainerValue = (String) Optional.ofNullable(runtime.getWebContainer())
                            .map(WebContainer::getValue).orElse("");
                    String javaVersionValue = (String) Optional.ofNullable(runtime.getJavaVersion())
                            .map(com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion::getValue).orElse("");
                    postEventProperties.put(TelemetryConstants.RUNTIME,
                            String.format("%s-%s-%s", osValue, webContainerValue, javaVersionValue));
                    postEventProperties.put(TelemetryConstants.WEBAPP_DEPLOY_TO_SLOT,
                            Boolean.valueOf(isDeployToSlot).toString());
                } catch (Exception e) {
                }

                String errTitle = "Deploy Web App Error";
                monitor.beginTask(message, IProgressMonitor.UNKNOWN);
                IWebAppBase<? extends AppServiceBaseEntity> deployTarget = null;
                Operation operation = TelemetryManager.createOperation(WEBAPP, DEPLOY_WEBAPP);

                try {
                    operation.start();
                    deployTarget = getRealWebApp(webApp, this, monitor, deploymentName);
                    String sitePath = buildSiteLink(deployTarget, isDeployToRoot ? null : artifactName);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 5, message);
                    if (!MavenUtils.isMavenProject(project)) {
                        export(artifactName, artifactPath);
                    } else if (isBuildBeforeDeploy) {
                        MavenExecuteAction action = new MavenExecuteAction("package");
                        CountDownLatch countDownLatch = new CountDownLatch(1);
                        action.launch(MavenUtils.getPomFile(project).getParent(), () -> {
                            countDownLatch.countDown();
                            return null;
                        });
                        countDownLatch.await();
                    }
                    message = "Deploying Web App...";
                    if (isDeployToSlot) {
                        message = "Deploying Web App to Slot...";
                    }
                    monitor.setTaskName(message);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 30, message);
                    AzureWebAppMvpModel.getInstance().deployArtifactsToWebApp(deployTarget, new File(artifactPath),
                            isDeployToRoot, new UpdateProgressIndicator(monitor));

                    if (monitor.isCanceled()) {
                        AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, null, -1,
                                cancelMessage);
                        return Status.CANCEL_STATUS;
                    }

                    message = "Checking Web App availability...";
                    monitor.setTaskName(message);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 20, message);

                    // to make warn up cancelable
                    int stepLimit = 5;
                    int sleepMs = 1000;
                    CountDownLatch countDownLatch = new CountDownLatch(1);
                    new Thread(() -> {
                        try {
                            for (int step = 0; step < stepLimit; ++step) {
                                if (monitor.isCanceled() || isUrlAccessible(sitePath)) { // warm up
                                    break;
                                }
                                Thread.sleep(sleepMs);
                            }
                        } catch (Exception ex) {
                            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                    "run@Thread@run@ProgressDialog@deploy@AppServiceCreateDialog@SingInDialog", ex));
                        } finally {
                            countDownLatch.countDown();
                        }
                    }).start();

                    try {
                        countDownLatch.await();
                    } catch (Exception ignore) {
                    }

                    if (monitor.isCanceled()) {
                        // it's published but not warmed up yet - consider as success
                        AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 100,
                                successMessage);
                        return Status.CANCEL_STATUS;
                    }

                    monitor.done();
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 100,
                            successMessage);
                    AppInsightsClient.create("Deploy as WebApp", "", postEventProperties);
                } catch (Exception ex) {
                    postEventProperties.put("PublishError", ex.getMessage());
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "run@ProgressDialog@deploy@AppServiceCreateDialog", ex));
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, null, -1, errorMessage);
                    if (deployTarget != null) {
                        deployTarget.start();
                    }
                    Display.getDefault().asyncExec(() -> ErrorWindow.go(parentShell, ex.getMessage(), errTitle));
                    EventUtil.logError(operation, ErrorType.systemError, ex, postEventProperties, null);
                } finally {
                    EventUtil.logEvent(EventType.info, operation, postEventProperties);
                    operation.complete();
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private IWebAppBase<? extends AppServiceBaseEntity> getRealWebApp(IWebApp webApp, Object parent,
            IProgressMonitor monitor, String deploymentName) {
        if (isDeployToSlot) {
            if (isCreateNewSlot) {
                String message = "Creating Deployment Slot...";
                monitor.setTaskName(message);
                AzureDeploymentProgressNotification.notifyProgress(parent, deploymentName, "", 30, message);
                return createDeploymentSlot(webApp);
            } else {
                return webApp.deploymentSlot(webAppSettingModel.getSlotName());
            }
        } else {
            return webApp;
        }
    }

    private IWebAppDeploymentSlot createDeploymentSlot(IWebApp webApp) {
        return EventUtil.executeWithLog(WEBAPP, CREATE_WEBAPP_SLOT, (operation) -> {
            return AzureWebAppMvpModel.getInstance().createDeploymentSlotFromSettingModel(webApp, webAppSettingModel);
        }, (e) -> {
            throw new RuntimeException("create slot failed", e);
        });
    }

    private void collectData() {
        if (isDeployToSlot) {
            webAppSettingModel = new WebAppSettingModel();
            if (isCreateNewSlot) {
                int index = comboSlotConf.getSelectionIndex();
                webAppSettingModel.setNewSlotConfigurationSource(index < 0 ? "" : comboSlotConf.getItem(index));
                webAppSettingModel.setNewSlotName(textSlotName.getText() == null ? "" : textSlotName.getText().trim());
            } else {
                int index = comboSlot.getSelectionIndex();
                webAppSettingModel.setSlotName(index < 0 ? "" : comboSlot.getItem(index));
            }
        }
    }

    private void deleteAppService() {
        int selectedRow = table.getSelectionIndex();
        if (selectedRow < 0) {
            return;
        }
        String appServiceName = table.getItems()[selectedRow].getText(0);
        IWebApp webApp = webAppDetailsMap.get(appServiceName);

        boolean confirmed = MessageDialog.openConfirm(getShell(), "Delete App Service",
                "Do you really want to delete the App Service '" + appServiceName + "'?");
        if (!confirmed) {
            return;
        }

        String errTitle = "Delete App Service Error";
        try {
            ProgressDialog.get(this.getShell(), "Delete App Service Progress").run(true, true, (monitor) -> {
                monitor.beginTask("Deleting App Service...", IProgressMonitor.UNKNOWN);
                EventUtil.executeWithLog(WEBAPP, DELETE_WEBAPP, (operation) -> {
                    webApp.delete();
                    Display.getDefault().asyncExec(() -> {
                        table.remove(selectedRow);
                        fillAppServiceDetails();
                        fillSlot();
                    });
                }, (ex) -> {
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "run@ProgressDialog@deleteAppService@AppServiceCreateDialog", ex));
                    Display.getDefault().asyncExec(() -> ErrorWindow.go(getShell(), ex.getMessage(), errTitle));
                });
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "deleteAppService@AppServiceCreateDialog", ex));
            ErrorWindow.go(getShell(), ex.getMessage(), errTitle);
        }
    }

    private void sendTelemetry(String action) {
        final Map<String, String> properties = new HashMap<>();
        properties.put("Window", this.getClass().getSimpleName());
        properties.put("Title", this.getShell().getText());
        AppInsightsClient.createByType(AppInsightsClient.EventType.Dialog, this.getClass().getSimpleName(), action,
                properties);
    }

    private Map<String, String> buildProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("Window", this.getClass().getSimpleName());
        properties.put("Title", this.getShell().getText());
        return properties;
    }

    private static boolean isUrlAccessible(String url) throws IOException {
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("HEAD");
        con.setReadTimeout(connection_read_timeout_ms);
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        } finally {
            con.disconnect();
        }
        return true;
    }
}
