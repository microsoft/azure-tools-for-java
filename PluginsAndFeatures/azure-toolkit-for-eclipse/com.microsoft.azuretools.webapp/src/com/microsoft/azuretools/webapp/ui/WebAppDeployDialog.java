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

package com.microsoft.azuretools.webapp.ui;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_WEBAPP_SLOT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.DEPLOY_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.OPEN_CREATEWEBAPP_DIALOG;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.REFRESH_METADATA;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.core.ui.ErrorWindow;
import com.microsoft.azuretools.core.ui.views.AzureDeploymentProgressNotification;
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
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.CanceledByUserException;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.azuretools.utils.WebAppUtils.WebAppDetails;
import com.microsoft.azuretools.webapp.Activator;
import com.microsoft.azuretools.webapp.util.CommonUtils;


@SuppressWarnings("restriction")
public class WebAppDeployDialog extends AppServiceBaseDialog {

    private static ILog LOG = Activator.getDefault().getLog();

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
    private static final String INVALID_SLOT_NAME =
        "The slot name is invalid, it needs to match the pattern " + SLOT_NAME_REGEX;
    private static final String DEPLOYMENT_SLOT_HOVER = "Deployment slots are live apps with their own hostnames. App"
        + " content and configurations elements can be swapped between two deployment slots, including the production "
        + "slot.";

    private Map<String, WebAppDetails> webAppDetailsMap = new HashMap<>();
    private WebAppSettingModel webAppSettingModel;
    private boolean isDeployToSlot = false;
    private boolean isCreateNewSlot = false;

    private Map<String, List<DeploymentSlot>> slotMap = new ConcurrentHashMap<>();

    /**
     * Create the dialog.
     *
     * @param parentShell
     */
    private WebAppDeployDialog(Shell parentShell, IProject project) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
        this.project = project;
        this.parentShell = parentShell;
    }

    public static WebAppDeployDialog go(Shell parentShell, IProject project) {
        WebAppDeployDialog d = new WebAppDeployDialog(parentShell, project);
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

    private void createAppGroup(Composite container) {
        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gridTable = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        gridTable.heightHint = 250;
        table.setLayoutData(gridTable);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.addListener(SWT.Selection, (e) -> {
            fillAppServiceDetails();
            fillSlot();
        });

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
                    AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                    fillTable();
                    slotMap.clear();
                    AppServiceCreateDialog.initAspCache();
                });
            }
        });
        btnRefresh.setText("Refresh");

        btnDeployToRoot = new Button(composite, SWT.CHECK);
        btnDeployToRoot.setSelection(true);
        btnDeployToRoot.setText("Deploy to root");

        int size = btnDeployToRoot.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        btnCreate.setLayoutData(new RowData(size, SWT.DEFAULT));
        btnDelete.setLayoutData(new RowData(size, SWT.DEFAULT));
        btnRefresh.setLayoutData(new RowData(size, SWT.DEFAULT));
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
            if (MavenUtils.isMavenProject(project) && MavenUtils.getPackaging(project).equals(WebAppUtils.TYPE_JAR)) {
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

        ToolBarManager barMgr = new ToolBarManager(SWT.FLAT);
        ToolBar toolBar = barMgr.createControl(compositeSlotCb);
        ToolItem item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(scaleImage(compositeSlotCb.getDisplay(), compositeSlotCb.getBackground(),
                compositeSlotCb.getDisplay().getSystemImage(SWT.ICON_INFORMATION), 15, 15));
        toolBar.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(AccessibleEvent e) {
                e.result = DEPLOYMENT_SLOT_HOVER;
            }
        });

        DefaultToolTip iconTooltip = new DefaultToolTip(toolBar, SWT.NONE, false);
        iconTooltip.setText(DEPLOYMENT_SLOT_HOVER);
        toolBar.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                iconTooltip.show(new Point(20, 20));
            }

            @Override
            public void focusLost(FocusEvent e) {
                iconTooltip.hide();
            }
        });

        toolBar.getParent().setTabList(new Control[] {btnDeployToSlot, toolBar });

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
        decComboSlotConf = decorateContorolAndRegister(comboSlotConf);

        scrolledComposite.setContent(grpSlot);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setMinSize(grpSlot.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        fillSlot();
        radioSlotLogic();
    }

    private Image scaleImage(Display device, Color color, Image orig, int scaledWidth, int scaledHeight) {
        try {
            Rectangle origBounds = orig.getBounds();
            if (origBounds.width == scaledWidth && origBounds.height == scaledHeight) {
                return orig;
            }

            ImageData origData = orig.getImageData();
            ImageData imData = new ImageData(scaledWidth, scaledHeight, origData.depth, origData.palette);
            if (origData.alphaData != null) {
                imData.alphaData = new byte[imData.width * imData.height];
                for (int row = 0; row < imData.height; row++) {
                    for (int col = 0; col < imData.width; col++) {
                        int origRow = row * origData.height / imData.height;
                        int origCol = col * origData.width / imData.width;
                        byte origAlpha = origData.alphaData[origRow * origData.width + origCol];
                        imData.alphaData[row * imData.width + col] = origAlpha;
                    }
                }
            }
            final Image scaled = new Image(device, imData);
            GC gc = new GC(scaled);
            gc.setAntialias(SWT.ON);
            gc.setInterpolation(SWT.HIGH);
            gc.setBackground(color);
            gc.fillRectangle(0, 0, scaledWidth, scaledHeight);
            gc.drawImage(orig, 0, 0, origBounds.width, origBounds.height, 0, 0, scaledWidth, scaledHeight);
            gc.dispose();
            return scaled;
        } catch (Exception ignore) {
            return orig;
        }
    }

    @Override
    public void create() {
        super.create();
        Display.getDefault().asyncExec(() -> {
            fillTable();
            fillUserSettings();
            AppServiceCreateDialog.initAspCache();
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
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        FtpCredentialsWindow w = new FtpCredentialsWindow(getShell(), wad.webApp);
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
        String appServiceName = table.getItems()[selectedRow].getText(0);

        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        SubscriptionDetail sd = wad.subscriptionDetail;
        AppServicePlan asp = wad.appServicePlan;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("App Service name: %s \n", appServiceName));
        sb.append(String.format("Subscription name: %s ; id: %s \n", sd.getSubscriptionName(), sd.getSubscriptionId()));
        String aspName = asp == null ? "N/A" : asp.name();
        String aspPricingTier = asp == null ? "N/A" : asp.pricingTier().toString();
        sb.append(String.format("App Service Plan name: %s ; Pricing tier: %s \n", aspName, aspPricingTier));
        String link = buildSiteLink(wad.webApp, null);
        sb.append(String.format("Link: <a href=\"%s\">%s</a> \n", link, link));
        sb.append(String.format("<a href=\"%s\">%s</a> \n", ftpLinkString, "Show FTP deployment credentials"));
        browserAppServiceDetails.setText(sb.toString());
    }

    private static String buildSiteLink(WebAppBase webApp, String artifactName) {
        String appServiceLink = "https://" + webApp.defaultHostName();
        if (artifactName != null && !artifactName.isEmpty()) {
            return appServiceLink + "/" + artifactName;
        } else {
            return appServiceLink;
        }
    }

    private void updateAndFillTable() {
        try {
            ProgressDialog.get(getShell(), "Update Azure Local Cache Progress").run(true, true,
                (monitor) -> {
                    monitor.beginTask("Updating Azure local cache...", IProgressMonitor.UNKNOWN);
                    try {
                        if (monitor.isCanceled()) {
                            throw new CanceledByUserException();
                        }
                        AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor));
                        Display.getDefault().asyncExec(() -> doFillTable());
                    } catch (CanceledByUserException ex) {
                        Display.getDefault().asyncExec(() -> {
                            System.out.println("updateAndFillTable(): Canceled by user");
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "run@ProgressDialog@updateAndFillTable@AppServiceCreateDialog", ex));
                    }
                    monitor.done();
                });
        } catch (Exception ex) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "updateAndFillTable@AppServiceCreateDialog", ex));
        }
    }

    private void doFillTable() {
        try {
            Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.getInstance()
                .getSubscriptionToResourceGroupMap();
            Map<ResourceGroup, List<WebApp>> rgwaMap = AzureModel.getInstance().getResourceGroupToWebAppMap();
            Map<ResourceGroup, List<AppServicePlan>> rgaspMap = AzureModel.getInstance()
                .getResourceGroupToAppServicePlanMap();

            webAppDetailsMap.clear();
            table.removeAll();

            List<WebAppDetails> webAppDetailsList = new ArrayList<>();
            for (SubscriptionDetail sd : srgMap.keySet()) {
                if (!sd.isSelected() || srgMap.get(sd) == null) {
                    continue;
                }
                for (ResourceGroup rg : srgMap.get(sd)) {
                    for (WebApp webApp : rgwaMap.get(rg)) {
                        if (WebAppUtils.isJavaWebApp(webApp)) {
                            WebAppDetails webAppDetails = new WebAppDetails();
                            webAppDetails.webApp = webApp;
                            webAppDetails.subscriptionDetail = sd;
                            webAppDetails.resourceGroup = rg;
                            webAppDetails.appServicePlan = findAppSevicePlanByID(webApp.appServicePlanId(), rgaspMap);
                            webAppDetails.appServicePlanResourceGroup = findResourceGroupByName(
                                webApp.resourceGroupName(), srgMap.get(sd));
                            webAppDetailsList.add(webAppDetails);
                        }
                    }
                }
            }
            Collections.sort(webAppDetailsList, (o1, o2) -> o1.webApp.name().compareTo(o2.webApp.name()));
            for (WebAppDetails webAppDetails : webAppDetailsList) {
                TableItem item = new TableItem(table, SWT.NULL);
                WebApp webApp = webAppDetails.webApp;
                item.setText(new String[]{webApp.name(),
                    webApp.javaVersion() != JavaVersion.OFF ? webApp.javaVersion().toString()
                        : WebAppUtils.getJavaRuntime(webApp),
                    WebAppUtils.getJavaRuntime(webApp), webApp.resourceGroupName()});
                webAppDetailsMap.put(webApp.name(), webAppDetails);
            }

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
        String appServiceName = table.getItems()[selectedRow].getText(0);
        WebAppDetails webAppDetail = webAppDetailsMap.get(appServiceName);
        if (webAppDetail == null || webAppDetail.webApp == null) {
            return;
        }
        WebApp webApp = webAppDetail.webApp;
        List<DeploymentSlot> deploymentSlots;
        if (slotMap.containsKey(webApp.name())) {
            deploymentSlots = slotMap.get(webApp.name());
        } else {
            deploymentSlots = WebAppUtils.getDeployments(webApp);
            slotMap.put(webApp.name(), deploymentSlots);
        }

        for (DeploymentSlot deploymentSlot : deploymentSlots) {
            comboSlot.add(deploymentSlot.name());
            comboSlotConf.add(deploymentSlot.name());
        }
        if (comboSlot.getItemCount() > 0) {
            comboSlot.select(0);
        }
        comboSlotConf.add(webApp.name());
        comboSlotConf.add(DONOT_CLONE_SLOT_CONF);
        comboSlotConf.select(0);
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
                    CommonUtils
                        .setPreference(CommonUtils.SLOT_CONF, webAppSettingModel.getNewSlotConfigurationSource());
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

    private AppServicePlan findAppSevicePlanByID(String id, Map<ResourceGroup, List<AppServicePlan>> rgaspMap) {
        if (rgaspMap == null) {
            return null;
        }
        for (List<AppServicePlan> appServicePlans : rgaspMap.values()) {
            for (AppServicePlan appServicePlan : appServicePlans) {
                if (appServicePlan.id().equals(id)) {
                    return appServicePlan;
                }
            }
        }
        return null;
    }

    private ResourceGroup findResourceGroupByName(String rgName, List<ResourceGroup> rgs) {
        if (rgs == null) {
            return null;
        }
        for (ResourceGroup rg : rgs) {
            if (rg.name().equals(rgName)) {
                return rg;
            }
        }
        return null;
    }

    private void createAppService(IProject project) {
        AppServiceCreateDialog d = AppServiceCreateDialog.go(getShell(), project);
        if (d == null) {
            // something went wrong - report an error!
            return;
        }
        WebApp wa = d.getWebApp();
        doFillTable();
        selectTableRowWithWebAppName(wa.name());
        fillAppServiceDetails();
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
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        if (wad != null && wad.webApp != null && !WebAppUtils.isJavaWebApp(wad.webApp)) {
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

    private void fillTable() {
        if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
            updateAndFillTable();
        } else {
            doFillTable();
        }
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
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        String jobDescription = String.format("Web App '%s' deployment", wad.webApp.name());
        if (isDeployToSlot) {
            jobDescription = String.format("Web App '%s' deploy to slot '%s'", wad.webApp.name(),
                isCreateNewSlot ? webAppSettingModel.getNewSlotName() : webAppSettingModel.getSlotName());
        }
        String deploymentName = UUID.randomUUID().toString();
        AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(deploymentName, jobDescription);
        boolean isDeployToRoot = btnDeployToRoot.getSelection();

        Job job = new Job(jobDescription) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                String message = "Packaging Artifact...";
                String cancelMessage = "Interrupted by user";
                String successMessage = "";
                String errorMessage = "Error";
                Map<String, String> postEventProperties = new HashMap<>();
                try {
                    boolean isJar = MavenUtils.isMavenProject(project) && MavenUtils.getPackaging(project)
                        .equals(WebAppUtils.TYPE_JAR);
                    postEventProperties.put(TelemetryConstants.JAVA_APPNAME, project.getName());
                    postEventProperties.put(TelemetryConstants.FILETYPE, isJar ? "jar" : "war");
                    postEventProperties.put(TelemetryConstants.RUNTIME,
                        wad.webApp.operatingSystem() == OperatingSystem.LINUX ? "linux-" + wad.webApp.linuxFxVersion()
                            : "windows-" + wad.webApp.javaContainer());
                    postEventProperties
                        .put(TelemetryConstants.WEBAPP_DEPLOY_TO_SLOT, Boolean.valueOf(isDeployToSlot).toString());
                } catch (Exception e) {
                }

                String errTitle = "Deploy Web App Error";
                monitor.beginTask(message, IProgressMonitor.UNKNOWN);
                WebAppBase webApp = null;
                Operation operation = TelemetryManager.createOperation(WEBAPP, DEPLOY_WEBAPP);

                try {
                    operation.start();
                    webApp = getRealWebApp(wad, this, monitor, deploymentName);
                    String sitePath = buildSiteLink(webApp, isDeployToRoot ? null : artifactName);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 5, message);
                    if (!MavenUtils.isMavenProject(project)) {
                        export(artifactName, artifactPath);
                    }
                    message = "Deploying Web App...";
                    if (isDeployToSlot) {
                        message = "Deploying Web App to Slot...";
                    }
                    monitor.setTaskName(message);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 30, message);
                    WebAppUtils.deployArtifactsToAppService(webApp, new File(artifactPath),
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
                                if (monitor.isCanceled() || WebAppUtils.isUrlAccessible(sitePath)) { // warm up
                                    break;
                                }
                                Thread.sleep(sleepMs);
                            }
                        } catch (Exception ex) {
                            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                "run@Thread@run@ProgressDialog@deploy@AppServiceCreateDialog@SingInDialog",
                                ex));
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
                    if (webApp != null) {
                        webApp.start();
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

    private WebAppBase getRealWebApp(WebAppDetails webAppDetails, Object parent, IProgressMonitor monitor,
        String deploymentName) {
        if (isDeployToSlot) {
            if (isCreateNewSlot) {
                String message = "Creating Deployment Slot...";
                monitor.setTaskName(message);
                AzureDeploymentProgressNotification.notifyProgress(parent, deploymentName, "", 30, message);
                return createDeploymentSlot(webAppDetails);
            } else {
                return webAppDetails.webApp.deploymentSlots().getByName(webAppSettingModel.getSlotName());
            }
        } else {
            return webAppDetails.webApp;
        }
    }

    private DeploymentSlot createDeploymentSlot(WebAppDetails webAppDetails) {
        return EventUtil.executeWithLog(WEBAPP, CREATE_WEBAPP_SLOT, (operation) -> {
            webAppSettingModel.setSubscriptionId(webAppDetails.subscriptionDetail.getSubscriptionId());
            webAppSettingModel.setWebAppId(webAppDetails.webApp.id());
            return AzureWebAppMvpModel.getInstance().createDeploymentSlot(webAppSettingModel);
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
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);

        boolean confirmed = MessageDialog.openConfirm(getShell(), "Delete App Service",
            "Do you really want to delete the App Service '" + appServiceName + "'?");
        if (!confirmed) {
            return;
        }

        String errTitle = "Delete App Service Error";
        try {
            ProgressDialog.get(this.getShell(), "Delete App Service Progress").run(true, true,
                (monitor) -> {
                    monitor.beginTask("Deleting App Service...", IProgressMonitor.UNKNOWN);
                    EventUtil.executeWithLog(WEBAPP, DELETE_WEBAPP, (operation) -> {
                        WebAppUtils.deleteAppService(wad);
                        Display.getDefault().asyncExec(() -> {
                            table.remove(selectedRow);
                            fillAppServiceDetails();
                            fillSlot();
                            slotMap.remove(wad.webApp.name());
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
}
