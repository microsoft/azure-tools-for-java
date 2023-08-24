/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.appservice.ui;

import static com.microsoft.azuretools.appservice.util.CommonUtils.ASP_CREATE_LOCATION;
import static com.microsoft.azuretools.appservice.util.CommonUtils.ASP_CREATE_PRICING;
import static com.microsoft.azuretools.appservice.util.CommonUtils.getSelectedItem;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azuretools.appservice.Activator;
import com.microsoft.azuretools.appservice.util.CommonUtils;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.core.ui.ErrorWindow;
import com.microsoft.azuretools.core.utils.AccessibilityUtils;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.core.utils.ProgressDialog;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class AppServiceCreateDialog extends AppServiceBaseDialog {

    // validation error string constants
    private static final String SELECT_RESOURCE_GROUP = "Select a valid resource group.";
    private static final String ENTER_RESOURCE_GROUP = "Enter a valid resource group name";
    private static final String SELECT_APP_SERVICE_PLAN = "Select a valid App Service Plan.";
    private static final String SELECT_LOCATION = "Select a location.";
    private static final String SELECT_A_VALID_SUBSCRIPTION = "Select a valid subscription.";
    private static final String ENTER_APP_SERVICE_PLAN_NAME = "Enter a valid App Service Plan name.";
    private static final String NAME_ALREADY_TAKEN = "The name is already taken";
    private static final String APP_SERVICE_PLAN_NAME_MUST_UNUQUE = "App service plan name must be unuque in each subscription.";
    private static final String APP_SERVICE_PLAN_NAME_INVALID_MSG = "App Service Plan name can only include alphanumeric characters and hyphens.";
    private static final String RESOURCE_GROUP_NAME_INVALID_MSG = "Resounce group name can only include alphanumeric characters, periods, underscores, " +
            "hyphens, and parenthesis and can't end in a period.";
    private static final String WEB_APP_NAME_INVALID_MSG = "The name can contain letters, numbers and hyphens but the first and last characters must be " +
            "a letter or number. The length must be between 2 and 60 characters.";
    private static final String REFRESHING = "Refreshing...";

    // validation regex
    private static final String WEB_APP_NAME_REGEX = "^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$";
    private static final String APP_SERVICE_PLAN_NAME_REGEX = "^[A-Za-z0-9-]*[A-Za-z0-9-]$";
    private static final String RESOURCE_GROUP_NAME_REGEX = "^[A-Za-z0-9-_()\\.]*[A-Za-z0-9-_()]$";

    // widgets
    private static final String LBL_WEB_CONTAINER = "Web container";
    private static final String LBL_JAVA = "Java version";
    private static final String LBL_PRICING_TIER = "Pricing tier";
    private static final String LBL_LOCATION = "Location";
    private static final String LBL_APP_NAME = "Enter name";
    private static final String LBL_SUBSCRIPTION = "Subscription";
    private static final String LBL_LINUXRUNTIME = "Linux Runtime";

    private static final String TXT_APP_NAME_MSG = "<enter name>";

    private static final String BTN_USE_EXISTING = "Use existing";
    private static final String BTN_CREATE_NEW = "Create new";
    private static final String BTN_RUNTIME_OS_WIN = "Windows";
    private static final String BTN_RUNTIME_OS_LINUX = "Linux";
    private static final String BTN_OK = "Create";

    private static final String GROUP_APP_SERVICE_PLAN = "App service plan";
    private static final String GROUP_RESOURCE_GROUP = "Resource group";
    private static final String GROUP_RUNTIME = "Runtime";
    private static final String GROUP_APPSETTING = "App Settings";

    private static final String PRICING_URL = "https://azure.microsoft.com/en-us/pricing/details/app-service/";
    public static final PricingTier DEFAULT_PRICINGTIER = PricingTier.BASIC_B2;
    private static final JavaVersion DEFAULT_JAVA_VERSION = JavaVersion.JAVA_17;
    private static final WebContainer DEFAULT_WEB_CONTAINER = WebContainer.JAVA_SE;
    private static final Runtime DEFAULT_LINUX_RUNTIME = Runtime.LINUX_JAVA17;
    public static final Region DEFAULT_REGION = Region.EUROPE_WEST;
    private static final String LNK_PRICING = "<a>App service pricing details</a>";
    private static final String NOT_AVAILABLE = "N/A";
    private static final String RESOURCE_GROUP_PREFIX = "rg-webapp-";
    private static final String APP_SERVICE_PLAN_PREFIX = "asp-";
    private static final String URL_SUFFIX = ".azurewebsites.net";
    private static final String WEB_APP_PREFIX = "webapp-";
    private static final String DATE_FORMAT = "yyMMddHHmmss";

    // dialog
    private static final String CREATING_APP_SERVICE = "Creating App Service....";
    private static final String VALIDATING_FORM_FIELDS = "Validating Form Fields....";
    private static final String CREATE_APP_SERVICE_PROGRESS_TITLE = "Create App Service Progress";
    private static final String ERROR_DIALOG_TITLE = "Create App Service Error";
    private static final String UPDATING_AZURE_LOCAL_CACHE = "Updating Azure local cache...";
    private static final String DIALOG_TITLE = "Create App Service";
    private static final String DIALOG_MESSAGE = "Create Azure App Service";

    // tooltip
    private static final String APPSETTINGS_TOOLTIP = "You can configure application setting here, such as \"JAVA_OPTS\"";
    private static ILog LOG = Activator.getDefault().getLog();

    private IProject project;

    private Text textAppName;
    private Text textResourceGroupName;
    private Text textAppSevicePlanName;
    private Combo comboWebContainer;
    private Combo comboSubscription;
    private Combo comboResourceGroup;
    private Combo comboAppServicePlan;
    private Combo comboAppServicePlanLocation;
    private Combo comboAppServicePlanPricingTier;
    private Combo cbJavaVersion;
    private Combo comboLinuxRuntime;

    private Label lblJavaVersion;
    private Label lblAppSevicePlanLocation;
    private Label lblAppServicePlanPricingTier;
    private Label lblWebContainer;
    private Label lblLinuxRuntime;

    private ControlDecoration dec_textAppName;
    private ControlDecoration dec_textNewResGrName;
    private ControlDecoration dec_textAppSevicePlanName;
    private ControlDecoration dec_comboWebContainer;
    private ControlDecoration dec_comboSubscription;
    private ControlDecoration dec_comboSelectResGr;
    private ControlDecoration dec_comboAppServicePlan;
    private ControlDecoration dec_comboAppServicePlanLocation;
    private ControlDecoration dec_cbJavaVersion;

    // controls to types bindings by index
    private List<JavaVersion> binderJavaVersions;
    private List<WebContainer> binderWebConteiners;
    private List<Subscription> binderSubscriptionDetails;
    private List<ResourceGroup> binderResourceGroup;
    private List<AppServicePlan> binderAppServicePlan;
    private List<Region> binderAppServicePlanLocation;
    private List<PricingTier> binderAppServicePlanPricingTier;
    private List<Runtime> binderRuntimeStacks;

    private Composite compositeAppServicePlan;
    private Button btnAppServiceCreateNew;
    private Label lblAppServiceCreateNewPricingTier;
    private Label lblAppServiceCreateNewLocation;

    private Button btnAppServiceUseExisting;
    private Label lblAppServiceUseExictingLocation;
    private Label lblAppServiceUseExistiogPrisingTier;
    private Link linkAppServicePricing;

    private Composite compositeResourceGroup;
    private Button btnResourceGroupCreateNew;
    private Button btnResourceGroupUseExisting;

    private Button btnOSGroupWin;
    private Button btnOSGroupLinux;

    private Table tblAppSettings;
    private TableEditor appSettingsEditor;
    private Button btnAppSettingsNew;
    private Button btnAppSettingsDel;
    private Composite compositeRuntime;

    private boolean chooseWin = false;
    protected WebApp webApp;
    private String packaging = "war";

    private final String date = new SimpleDateFormat(DATE_FORMAT).format(new Date());
    private static Map<String, List<AppServicePlan>> sidAspMap = new ConcurrentHashMap<>();
    private Map<String, String> appSettings = new HashMap<>();
    protected WebAppSettingModel model = new WebAppSettingModel();

    public WebApp getWebApp() {
        return this.webApp;
    }

    public static AppServiceCreateDialog go(Shell parentShell, IProject project) {
        AppServiceCreateDialog d = new AppServiceCreateDialog(parentShell, project);
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    private AppServiceCreateDialog(Shell parentShell, IProject project) {
        super(parentShell);
        setHelpAvailable(false);
        this.project = project;
        updatePacking();
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        Image image = PluginUtil.getImage("icons/large/Azure.png");
        if (image != null) {
            setTitleImage(image);
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(DIALOG_MESSAGE);
        setTitle(DIALOG_TITLE);

        ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolledComposite.setLayout(new GridLayout(1, false));
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Group group = new Group(scrolledComposite, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Group grpAppService = new Group(group, SWT.NONE);
        grpAppService.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        grpAppService.setLayout(new GridLayout(3, false));

        Label lblAppName = new Label(grpAppService, SWT.NONE);
        lblAppName.setText(LBL_APP_NAME);

        textAppName = new Text(grpAppService, SWT.BORDER);
        textAppName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });

        textAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textAppName.setMessage(TXT_APP_NAME_MSG);
        textAppName.setText(WEB_APP_PREFIX + date);
        dec_textAppName = decorateContorolAndRegister(textAppName);

        Label lblazurewebsitescom = new Label(grpAppService, SWT.NONE);
        lblazurewebsitescom.setText(URL_SUFFIX);

        Label lblSubscription = new Label(grpAppService, SWT.NONE);
        lblSubscription.setText(LBL_SUBSCRIPTION);

        comboSubscription = new Combo(grpAppService, SWT.READ_ONLY);
        comboSubscription.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fillResourceGroups();
                fillAppServicePlans();
                fillAppServicePlansDetails();
                fillRegions();
            }
        });
        dec_comboSubscription = decorateContorolAndRegister(comboSubscription);
        comboSubscription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        createRuntimeGroup(group);
        createASPGroup(group);
        createResourceGroup(group);
        createAppSettingGroup(group);

        scrolledComposite.setContent(group);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        fillSubscriptions();
        fillRegions();
        fillResourceGroups();
        fillJavaVersion();
        fillLinuxRuntime();
        fillWebContainers();
        fillPricingTiers();

        String os = CommonUtils.getPreference(CommonUtils.RUNTIME_OS);
        if (StringUtils.equalsIgnoreCase(os, OperatingSystem.LINUX.toString())) {
            btnOSGroupLinux.setSelection(true);
            btnOSGroupWin.setSelection(false);
        } else if (StringUtils.equalsIgnoreCase(os, OperatingSystem.WINDOWS.toString())) {
            btnOSGroupLinux.setSelection(false);
            btnOSGroupWin.setSelection(true);
        }
        radioRuntimeLogic();

        return scrolledComposite;
    }

    private void createASPGroup(Composite composite) {
        Group group = new Group(composite, SWT.NONE);
        FontDescriptor boldDescriptor = FontDescriptor.createFrom(group.getFont()).setStyle(SWT.BOLD);
        Font boldFont = boldDescriptor.createFont(group.getDisplay());
        group.setFont(boldFont);
        group.setText(GROUP_APP_SERVICE_PLAN);
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        group.setLayout(new FillLayout());

        compositeAppServicePlan = new Composite(group, SWT.NONE);
        compositeAppServicePlan.setLayout(new GridLayout(2, false));

        btnAppServiceUseExisting = new Button(compositeAppServicePlan, SWT.RADIO);
        btnAppServiceUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioAppServicePlanLogic();
            }
        });
        btnAppServiceUseExisting.setText(BTN_USE_EXISTING);
        btnAppServiceUseExisting.setSelection(true);
        comboAppServicePlan = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlan.setEnabled(true);
        comboAppServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboAppServicePlan.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fillAppServicePlansDetails();
            }
        });
        comboAppServicePlan.setBounds(0, 0, 26, 22);
        dec_comboAppServicePlan = decorateContorolAndRegister(comboAppServicePlan);

        AccessibilityUtils.addAccessibilityNameForUIComponent(comboAppServicePlan, "Existing app service plan");
        lblAppServiceUseExictingLocation = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServiceUseExictingLocation.setEnabled(true);
        GridData gdLblAppServiceUseExictingLocation = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdLblAppServiceUseExictingLocation.horizontalIndent = 20;
        lblAppServiceUseExictingLocation.setLayoutData(gdLblAppServiceUseExictingLocation);
        lblAppServiceUseExictingLocation.setText(LBL_LOCATION);

        lblAppSevicePlanLocation = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppSevicePlanLocation.setEnabled(true);
        lblAppSevicePlanLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblAppSevicePlanLocation.setText(NOT_AVAILABLE);

        lblAppServiceUseExistiogPrisingTier = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServiceUseExistiogPrisingTier.setEnabled(true);
        GridData gdLblAppServiceUseExistiogPrisingTier = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdLblAppServiceUseExistiogPrisingTier.horizontalIndent = 20;
        lblAppServiceUseExistiogPrisingTier.setLayoutData(gdLblAppServiceUseExistiogPrisingTier);
        lblAppServiceUseExistiogPrisingTier.setText(LBL_PRICING_TIER);

        lblAppServicePlanPricingTier = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServicePlanPricingTier.setEnabled(true);
        lblAppServicePlanPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblAppServicePlanPricingTier.setText(NOT_AVAILABLE);

        btnAppServiceCreateNew = new Button(compositeAppServicePlan, SWT.RADIO);
        btnAppServiceCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioAppServicePlanLogic();
            }
        });
        btnAppServiceCreateNew.setBounds(0, 0, 90, 16);
        btnAppServiceCreateNew.setText(BTN_CREATE_NEW);
        btnAppServiceCreateNew.setEnabled(true);

        textAppSevicePlanName = new Text(compositeAppServicePlan, SWT.BORDER);
        textAppSevicePlanName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        textAppSevicePlanName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textAppSevicePlanName.setMessage(TXT_APP_NAME_MSG);
        dec_textAppSevicePlanName = decorateContorolAndRegister(textAppSevicePlanName);
        textAppSevicePlanName.setText(APP_SERVICE_PLAN_PREFIX + date);
        textAppSevicePlanName.setEnabled(false);
        AccessibilityUtils.addAccessibilityNameForUIComponent(textAppSevicePlanName, "New app service plan");

        lblAppServiceCreateNewLocation = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gdLblAppServiceCreateNewLocation = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdLblAppServiceCreateNewLocation.horizontalIndent = 20;
        lblAppServiceCreateNewLocation.setLayoutData(gdLblAppServiceCreateNewLocation);
        lblAppServiceCreateNewLocation.setText(LBL_LOCATION);
        lblAppServiceCreateNewLocation.setEnabled(false);

        comboAppServicePlanLocation = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlanLocation.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        comboAppServicePlanLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboAppServicePlanLocation.setEnabled(false);
        dec_comboAppServicePlanLocation = decorateContorolAndRegister(comboAppServicePlanLocation);
        AccessibilityUtils.addAccessibilityNameForUIComponent(comboAppServicePlanLocation, "App service plan location");

        lblAppServiceCreateNewPricingTier = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gdLblAppServiceCreateNewPricingTier = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdLblAppServiceCreateNewPricingTier.horizontalIndent = 20;
        lblAppServiceCreateNewPricingTier.setLayoutData(gdLblAppServiceCreateNewPricingTier);
        lblAppServiceCreateNewPricingTier.setText(LBL_PRICING_TIER);
        lblAppServiceCreateNewPricingTier.setEnabled(false);

        comboAppServicePlanPricingTier = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlanPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboAppServicePlanPricingTier.setEnabled(false);
        AccessibilityUtils.addAccessibilityNameForUIComponent(comboAppServicePlanPricingTier,
                "App service plan pricing tier");

        new Label(compositeAppServicePlan, SWT.NONE);
        linkAppServicePricing = new Link(compositeAppServicePlan, SWT.NONE);
        linkAppServicePricing.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        linkAppServicePricing.setText(LNK_PRICING);
        linkAppServicePricing.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(PRICING_URL));
                } catch (PartInitException | MalformedURLException ex) {
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "widgetSelected@SelectionAdapter@linkAppServicePricing@AppServiceCreateDialog", ex));
                }
            }
        });

    }

    private void createResourceGroup(Composite composite) {
        Group group = new Group(composite, SWT.NONE);
        FontDescriptor boldDescriptor = FontDescriptor.createFrom(group.getFont()).setStyle(SWT.BOLD);
        Font boldFont = boldDescriptor.createFont(group.getDisplay());
        group.setFont(boldFont);
        group.setText(GROUP_RESOURCE_GROUP);
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        group.setLayout(new FillLayout());

        compositeResourceGroup = new Composite(group, SWT.NONE);
        compositeResourceGroup.setLayout(new GridLayout(2, false));

        btnResourceGroupUseExisting = new Button(compositeResourceGroup, SWT.RADIO);
        btnResourceGroupUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioResourceGroupLogic();
            }
        });
        btnResourceGroupUseExisting.setSelection(true);
        btnResourceGroupUseExisting.setText(BTN_USE_EXISTING);
        comboResourceGroup = new Combo(compositeResourceGroup, SWT.READ_ONLY);
        comboResourceGroup.setEnabled(true);
        comboResourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboResourceGroup.setBounds(0, 0, 26, 22);
        dec_comboSelectResGr = decorateContorolAndRegister(comboResourceGroup);
        AccessibilityUtils.addAccessibilityNameForUIComponent(comboResourceGroup, "Existing resource group");

        btnResourceGroupCreateNew = new Button(compositeResourceGroup, SWT.RADIO);
        btnResourceGroupCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioResourceGroupLogic();
            }
        });
        btnResourceGroupCreateNew.setText(BTN_CREATE_NEW);

        textResourceGroupName = new Text(compositeResourceGroup, SWT.BORDER);
        textResourceGroupName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        textResourceGroupName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textResourceGroupName.setBounds(0, 0, 64, 19);
        textResourceGroupName.setMessage(TXT_APP_NAME_MSG);
        textResourceGroupName.setText(RESOURCE_GROUP_PREFIX + date);
        textResourceGroupName.setEnabled(false);
        dec_textNewResGrName = decorateContorolAndRegister(textResourceGroupName);
        AccessibilityUtils.addAccessibilityNameForUIComponent(textResourceGroupName, "New resource group");
    }

    private void createRuntimeGroup(Composite composite) {
        Group group = new Group(composite, SWT.NONE);
        FontDescriptor boldDescriptor = FontDescriptor.createFrom(group.getFont()).setStyle(SWT.BOLD);
        Font boldFont = boldDescriptor.createFont(group.getDisplay());
        group.setFont(boldFont);
        group.setText(GROUP_RUNTIME);
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 3));
        group.setLayout(new FillLayout());

        compositeRuntime = new Composite(group, SWT.NONE);
        compositeRuntime.setLayout(new GridLayout(2, false));

        btnOSGroupLinux = new Button(compositeRuntime, SWT.RADIO);
        btnOSGroupLinux.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioRuntimeLogic();
            }
        });
        btnOSGroupLinux.setText(BTN_RUNTIME_OS_LINUX);
        btnOSGroupLinux.setSelection(true);

        btnOSGroupWin = new Button(compositeRuntime, SWT.RADIO);
        btnOSGroupWin.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioRuntimeLogic();
            }
        });
        btnOSGroupWin.setText(BTN_RUNTIME_OS_WIN);

        lblLinuxRuntime = new Label(compositeRuntime, SWT.NONE);
        lblLinuxRuntime.setText(LBL_LINUXRUNTIME);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        lblLinuxRuntime.setLayoutData(gridData);

        comboLinuxRuntime = new Combo(compositeRuntime, SWT.READ_ONLY);
        comboLinuxRuntime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        lblJavaVersion = new Label(compositeRuntime, SWT.NONE);
        lblJavaVersion.setText(LBL_JAVA);
        lblJavaVersion.setEnabled(false);
        gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        lblJavaVersion.setLayoutData(gridData);

        cbJavaVersion = new Combo(compositeRuntime, SWT.READ_ONLY);
        cbJavaVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        cbJavaVersion.setEnabled(false);
        cbJavaVersion.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                fillWebContainers();
            }

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                fillWebContainers();
            }
        });
        dec_cbJavaVersion = decorateContorolAndRegister(cbJavaVersion);

        lblWebContainer = new Label(compositeRuntime, SWT.NONE);
        lblWebContainer.setText(LBL_WEB_CONTAINER);
        lblWebContainer.setEnabled(false);
        gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        lblWebContainer.setLayoutData(gridData);

        comboWebContainer = new Combo(compositeRuntime, SWT.READ_ONLY);
        comboWebContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        comboWebContainer.setEnabled(false);
        dec_comboWebContainer = decorateContorolAndRegister(comboWebContainer);
    }

    private void createAppSettingGroup(Composite composite) {
        Group group = new Group(composite, SWT.NONE);
        FontDescriptor boldDescriptor = FontDescriptor.createFrom(group.getFont()).setStyle(SWT.BOLD);
        Font boldFont = boldDescriptor.createFont(group.getDisplay());
        group.setFont(boldFont);
        group.setText(GROUP_APPSETTING);
        group.setToolTipText(APPSETTINGS_TOOLTIP);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        group.setLayout(new GridLayout());
        Composite cpAppSettings = new Composite(group, SWT.NONE);
        cpAppSettings.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gridData.heightHint = 150;
        cpAppSettings.setLayoutData(gridData);

        tblAppSettings = new Table(cpAppSettings, SWT.BORDER | SWT.FULL_SELECTION);
        tblAppSettings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        tblAppSettings.setHeaderVisible(true);
        tblAppSettings.setLinesVisible(true);
        tblAppSettings.addListener(SWT.MouseDoubleClick, event -> onTblAppSettingMouseDoubleClick(event));
        tblAppSettings.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                // Edit items when user press enter
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    TableItem[] selection = tblAppSettings.getSelection();
                    if (selection.length > 0) {
                        editingTableItem(selection[0], 0);
                        e.doit = false;
                    }
                }
            }
        });
        AccessibilityUtils.addAccessibilityNameForUIComponent(tblAppSettings, "App settings");

        appSettingsEditor = new TableEditor(tblAppSettings);
        appSettingsEditor.horizontalAlignment = SWT.LEFT;
        appSettingsEditor.grabHorizontal = true;

        TableColumn columnKey = new TableColumn(tblAppSettings, SWT.NONE);
        columnKey.setWidth(300);
        columnKey.setText("Key");

        TableColumn columnValue = new TableColumn(tblAppSettings, SWT.NONE);
        columnValue.setWidth(300);
        columnValue.setText("Value");

        Composite cpTableButtons = new Composite(cpAppSettings, SWT.NONE);
        cpTableButtons.setLayout(new GridLayout(1, false));
        cpTableButtons.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));

        btnAppSettingsNew = new Button(cpTableButtons, SWT.NONE);
        btnAppSettingsNew.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnAppSettingsNew.setText("New");
        btnAppSettingsNew.setToolTipText("New");
        btnAppSettingsNew.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
        btnAppSettingsNew.addListener(SWT.Selection, event -> onBtnNewItemSelection());

        btnAppSettingsDel = new Button(cpTableButtons, SWT.NONE);
        btnAppSettingsDel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnAppSettingsDel.setText("Delete");
        btnAppSettingsDel.setToolTipText("Delete");
        btnAppSettingsDel.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
        btnAppSettingsDel.addListener(SWT.Selection, event -> onBtnDeleteItemSelection());
    }

    private void updateTableActionBtnStatus(boolean enabled) {
        btnAppSettingsNew.setEnabled(enabled);
        btnAppSettingsDel.setEnabled(enabled);
    }

    private void onTblAppSettingMouseDoubleClick(Event event) {
        updateTableActionBtnStatus(false);
        Rectangle clientArea = tblAppSettings.getClientArea();
        Point pt = new Point(event.x, event.y);
        int index = tblAppSettings.getTopIndex();
        while (index < tblAppSettings.getItemCount()) {
            boolean visible = false;
            final TableItem item = tblAppSettings.getItem(index);
            for (int i = 0; i < tblAppSettings.getColumnCount(); i++) {
                Rectangle rect = item.getBounds(i);
                if (rect.contains(pt)) {
                    editingTableItem(item, i);
                    return;
                }
                if (!visible && rect.intersects(clientArea)) {
                    visible = true;
                }
            }
            if (!visible) {
                updateTableActionBtnStatus(true);
                return;
            }
            index++;
        }
        updateTableActionBtnStatus(true);
    }

    @SuppressWarnings("checkstyle:FallThrough")
    private void editingTableItem(TableItem item, int column) {
        final Text text = new Text(tblAppSettings, SWT.NONE);
        Listener textListener = e -> {
            switch (e.type) {
                case SWT.FocusOut:
                    item.setText(column, text.getText());
                    text.dispose();
                    readTblAppSettings();
                    updateTableActionBtnStatus(true);
                    break;
                case SWT.Traverse:
                    switch (e.detail) {
                        case SWT.TRAVERSE_RETURN:
                            item.setText(column, text.getText());
                            text.dispose();
                            e.doit = false;
                            readTblAppSettings();
                            updateTableActionBtnStatus(true);
                            break;
                        case SWT.TRAVERSE_ESCAPE:
                            text.dispose();
                            e.doit = false;
                            readTblAppSettings();
                            updateTableActionBtnStatus(true);
                            break;
                        case SWT.TRAVERSE_TAB_NEXT:
                            if (column < tblAppSettings.getColumnCount()) {
                                editingTableItem(item, column + 1);
                            }
                            break;
                        case SWT.TRAVERSE_TAB_PREVIOUS:
                            if (column > 0) {
                                editingTableItem(item, column - 1);
                            }
                            break;
                        default:
                    }
                    break;
                default:
            }
        };
        text.addListener(SWT.FocusOut, textListener);
        text.addListener(SWT.Traverse, textListener);
        appSettingsEditor.setEditor(text, item, column);
        text.setText(item.getText(column));
        text.selectAll();
        text.setFocus();
    }

    private void onBtnDeleteItemSelection() {
        int seletedIndex = tblAppSettings.getSelectionIndex();
        int itemCount = tblAppSettings.getItemCount();
        if (seletedIndex >= 0 && seletedIndex < tblAppSettings.getItemCount()) {
            updateTableActionBtnStatus(false);
            tblAppSettings.remove(seletedIndex);
            updateTableActionBtnStatus(true);
            readTblAppSettings();
            if (tblAppSettings.getItemCount() > 0) {
                if (seletedIndex == itemCount - 1) {
                    tblAppSettings.setSelection(seletedIndex - 1);
                } else {
                    tblAppSettings.setSelection(seletedIndex);
                }
            }
        }
        tblAppSettings.setFocus();
    }

    private void readTblAppSettings() {
        appSettings.clear();
        int row = 0;
        while (row < tblAppSettings.getItemCount()) {
            TableItem item = tblAppSettings.getItem(row);
            String key = item.getText(0);
            String value = item.getText(1);
            if (key.isEmpty() || appSettings.containsKey(key)) {
                tblAppSettings.remove(row);
                continue;
            }
            appSettings.put(key, value);
            ++row;
        }
    }

    private void onBtnNewItemSelection() {
        updateTableActionBtnStatus(false);
        TableItem item = new TableItem(tblAppSettings, SWT.NONE);
        item.setText(new String[] { "<key>", "<value>" });
        tblAppSettings.setSelection(item);
        editingTableItem(item, 0);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        cleanError();
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText(BTN_OK);
    }

    private void radioRuntimeLogic() {
        cleanError();
        chooseWin = btnOSGroupWin.getSelection();
        boolean enabled = btnOSGroupLinux.getSelection();
        {
            lblLinuxRuntime.setEnabled(enabled);
            comboLinuxRuntime.setEnabled(enabled);
        }
        {
            cbJavaVersion.setEnabled(!enabled);
            lblJavaVersion.setEnabled(!enabled);
            lblWebContainer.setEnabled(!enabled);
            comboWebContainer.setEnabled(!enabled);
        }
        fillAppServicePlans();
    }

    private void radioAppServicePlanLogic() {
        cleanError();
        boolean enabled = btnAppServiceCreateNew.getSelection();
        textAppSevicePlanName.setEnabled(enabled);

        lblAppServiceCreateNewLocation.setEnabled(enabled);
        comboAppServicePlanLocation.setEnabled(enabled);

        lblAppServiceCreateNewPricingTier.setEnabled(enabled);
        comboAppServicePlanPricingTier.setEnabled(enabled);

        comboAppServicePlan.setEnabled(!enabled);

        lblAppServiceUseExictingLocation.setEnabled(!enabled);
        lblAppSevicePlanLocation.setEnabled(!enabled);

        lblAppServiceUseExistiogPrisingTier.setEnabled(!enabled);
        lblAppServicePlanPricingTier.setEnabled(!enabled);
    }

    private void radioResourceGroupLogic() {
        cleanError();
        boolean enabled = btnResourceGroupCreateNew.getSelection();
        textResourceGroupName.setEnabled(enabled);
        comboResourceGroup.setEnabled(!enabled);
    }

    protected static <T> List<T> createListFromClassFields(Class<?> c) throws IllegalAccessException {
        List<T> list = new LinkedList<T>();

        Field[] declaredFields = c.getDeclaredFields();
        for (Field field : declaredFields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers)) {
                @SuppressWarnings("unchecked")
                T value = (T) field.get(null);
                list.add(value);
            }
        }
        return list;
    }

    private void fillSubscriptions() {
        try {
            List<Subscription> selectedSubscriptions = Azure.az(AzureAccount.class).account()
                    .getSelectedSubscriptions();
            // reset model
            if (selectedSubscriptions == null) {
                return;
            }
            comboSubscription.removeAll();
            binderSubscriptionDetails = new ArrayList<>();
            for (Subscription sd : selectedSubscriptions) {
                comboSubscription.add(sd.getName());
                binderSubscriptionDetails.add(sd);
            }
            if (comboSubscription.getItemCount() > 0) {
                comboSubscription.select(0);
            }
            String subscription = CommonUtils.getPreference(CommonUtils.SUBSCRIPTION);
            CommonUtils.selectComboIndex(comboSubscription, subscription);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "doFillSubscriptions@AppServiceCreateDialog", ex));
        }
    }

    protected void fillResourceGroups() {
        Subscription selectedSubscription = getSelectedSubscription();
        if (selectedSubscription == null) {
            return;
        }
        setComboRefreshingStatus(comboResourceGroup, true);
        Mono.fromCallable(() -> {
            List<ResourceGroup> list = Azure.az(AzureResources.class).groups(selectedSubscription.getId()).list();
            list.sort(Comparator.comparing(ResourceGroup::getName));
            return list;
        }).subscribeOn(Schedulers.boundedElastic()).subscribe(groupList -> {
            binderResourceGroup = new ArrayList<>();
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (comboResourceGroup.isDisposed()) {
                    return;
                }
                setComboRefreshingStatus(comboResourceGroup, false);
                comboResourceGroup.setEnabled(btnResourceGroupUseExisting.getSelection());
                comboResourceGroup.removeAll();
                for (ResourceGroup rg : groupList) {
                    comboResourceGroup.add(rg.getName());
                    binderResourceGroup.add(rg);
                }

                if (comboResourceGroup.getItemCount() > 0) {
                    comboResourceGroup.select(0);
                }
                String resourceGroup = CommonUtils.getPreference(CommonUtils.RG_NAME);
                CommonUtils.selectComboIndex(comboResourceGroup, resourceGroup);
            });
        });
    }

    protected void fillAppServicePlans() {
        Subscription selectedSubscription = getSelectedSubscription();
        if (selectedSubscription == null) {
            return;
        }
        OperatingSystem os = getSelectedOS();
        setComboRefreshingStatus(comboAppServicePlan, true);
        Mono.fromCallable(() -> {
            return Azure.az(AzureAppService.class).plans(selectedSubscription.getId()).list().stream()
                    .filter(asp -> asp.getOperatingSystem() == null || asp.getOperatingSystem() == os)
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic())
            .subscribe(appServicePlans -> {
                appServicePlans.sort(Comparator.comparing(AppServicePlan::name));
                DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (comboAppServicePlan.isDisposed()) {
                        return;
                    }
                    setComboRefreshingStatus(comboAppServicePlan, false);
                    comboAppServicePlan.setEnabled(btnAppServiceUseExisting.getSelection());
                    binderAppServicePlan = new ArrayList<>();
                    for (AppServicePlan asp : appServicePlans) {
                        binderAppServicePlan.add(asp);
                        comboAppServicePlan.add(asp.name());
                    }

                    if (comboAppServicePlan.getItemCount() > 0) {
                        comboAppServicePlan.select(0);
                    }
                    String aspName = CommonUtils.getPreference(CommonUtils.ASP_NAME);
                    CommonUtils.selectComboIndex(comboAppServicePlan, aspName);
                    fillAppServicePlansDetails();
                });
            });
    }

    protected void fillAppServicePlansDetails() {
        int i = comboAppServicePlan.getSelectionIndex();
        if (i < 0) {
            lblAppSevicePlanLocation.setText(NOT_AVAILABLE);
            lblAppServicePlanPricingTier.setText(NOT_AVAILABLE);
        } else {
            AppServicePlan asp = binderAppServicePlan.get(i);
            lblAppSevicePlanLocation.setText(asp.getRegion().getLabel());
            lblAppServicePlanPricingTier.setText(asp.getPricingTier().toString());
        }
    }

    protected void fillRegions() {
        Subscription selectedSubscription = getSelectedSubscription();
        if (selectedSubscription == null) {
            return;
        }

        setComboRefreshingStatus(comboAppServicePlanLocation, true);
        Mono.fromCallable(() -> Azure.az(AzureAccount.class).listRegions(selectedSubscription.getId()))
                .subscribeOn(Schedulers.boundedElastic()).subscribe(locl -> {
                    if (locl != null) {
                        binderAppServicePlanLocation = new ArrayList<>();
                        DefaultLoader.getIdeHelper().invokeLater(() -> {
                            if (comboAppServicePlanLocation.isDisposed()) {
                                return;
                            }
                            setComboRefreshingStatus(comboAppServicePlanLocation, false);
                            comboAppServicePlanLocation.setEnabled(btnAppServiceCreateNew.getSelection());
                            for (int i = 0; i < locl.size(); i++) {
                                Region loc = locl.get(i);
                                comboAppServicePlanLocation.add(loc.getLabel());
                                binderAppServicePlanLocation.add(loc);
                                if (Objects.equals(loc, DEFAULT_REGION)) {
                                    comboAppServicePlanLocation.select(i);
                                }
                            }
                            if (comboAppServicePlanLocation.getSelectionIndex() < 0 && comboAppServicePlanLocation.getItemCount() > 0) {
                                comboAppServicePlanLocation.select(0);
                            }
                            String aspLocation = CommonUtils.getPreference(ASP_CREATE_LOCATION);
                            CommonUtils.selectComboIndex(comboAppServicePlanLocation, aspLocation);
                        });
                    }
                });
    }

    protected void fillPricingTiers() {
        try {
            comboAppServicePlanPricingTier.removeAll();
            binderAppServicePlanPricingTier = new ArrayList<>();

            final List<PricingTier> pricingTiers = AzureMvpModel.getInstance().listPricingTier();

            for (int i = 0; i < pricingTiers.size(); i++) {
                PricingTier pricingTier = pricingTiers.get(i);
                comboAppServicePlanPricingTier.add(pricingTier.toString());
                binderAppServicePlanPricingTier.add(pricingTier);
                if (pricingTier.equals(DEFAULT_PRICINGTIER)) {
                    comboAppServicePlanPricingTier.select(i);
                }
            }
            if (comboAppServicePlanPricingTier.getSelectionIndex() < 0 && comboAppServicePlanPricingTier.getItemCount() > 0) {
                comboAppServicePlanPricingTier.select(0);
            }
            String aspPricing = CommonUtils.getPreference(ASP_CREATE_PRICING);
            CommonUtils.selectComboIndex(comboAppServicePlanPricingTier, aspPricing);
        } catch (Exception ex) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "fillAppServicePlanPricingTiers@AppServiceCreateDialog", ex));
        }
    }

    private void fillLinuxRuntime() {
        List<Runtime> runtimeStacks = Runtime.WEBAPP_RUNTIME.stream().filter(runtime -> runtime.isLinux())
                .collect(Collectors.toList());
        binderRuntimeStacks = new ArrayList<Runtime>();
        for (int i = 0; i < runtimeStacks.size(); i++) {
            Runtime runtimeStack = runtimeStacks.get(i);
            comboLinuxRuntime.add(runtimeStack.toString());
            binderRuntimeStacks.add(runtimeStack);
            if (Objects.equals(runtimeStack, DEFAULT_LINUX_RUNTIME)) {
                comboLinuxRuntime.select(i);
            }
        }
        String linuxRuntime = CommonUtils.getPreference(CommonUtils.RUNTIME_LINUX);
        CommonUtils.selectComboIndex(comboLinuxRuntime, linuxRuntime);
    }

    protected void fillWebContainers() {
        final boolean isJarPacking = packaging.equals("jar");
        final JavaVersion jdkModel = getSelectedJavaVersion();
        final List<WebContainer> webContainers = isJarPacking ? Arrays.asList(WebContainer.JAVA_SE)
                : listWindowsWebContainersForWarFile(jdkModel);
        comboWebContainer.removeAll();
        binderWebConteiners = new ArrayList<>();
        for (int i = 0; i < webContainers.size(); i++) {
            WebContainer webContainerMod = webContainers.get(i);
            comboWebContainer.add(webContainerMod.toString());
            binderWebConteiners.add(webContainerMod);
            if (i == 0 || webContainerMod == DEFAULT_WEB_CONTAINER) {
                comboWebContainer.select(i);
            }
        }
        String webContainer = CommonUtils.getPreference(CommonUtils.RUNTIME_WEBCONTAINER);
        CommonUtils.selectComboIndex(comboWebContainer, webContainer);
    }

    private void updatePacking() {
        try {
            if (MavenUtils.isMavenProject(project)) {
                packaging = MavenUtils.getPackaging(project);
            }
        } catch (Exception e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "fillWebContainers@AppServiceCreateDialog", e));
        }
    }

    private List<WebContainer> listWindowsWebContainersForWarFile(JavaVersion javaVersion) {
        return Runtime.WEBAPP_RUNTIME.stream().filter(runtime -> runtime.isWindows())
                .filter(runtime -> !Objects.equals(runtime.getWebContainer(), WebContainer.JAVA_SE))
                .distinct()
                .map(Runtime::getWebContainer).distinct().collect(Collectors.toList());
    }

    protected void fillJavaVersion() {
        binderJavaVersions = new ArrayList<JavaVersion>();
        for (JavaVersion javaVersion : JavaVersion.values()) {
            if (javaVersion == JavaVersion.OFF) {
                continue;
            }
            cbJavaVersion.add(javaVersion.toString());
            binderJavaVersions.add(javaVersion);
            if (Objects.equals(javaVersion, DEFAULT_JAVA_VERSION)) {
                cbJavaVersion.select(binderJavaVersions.size() - 1);
            }
        }
        String javaversion = CommonUtils.getPreference(CommonUtils.RUNTIME_JAVAVERSION);
        CommonUtils.selectComboIndex(cbJavaVersion, javaversion);
    }

    private void recordUserSettings() {
        try {
            CommonUtils.setPreference(CommonUtils.SUBSCRIPTION, getSelectedItem(comboSubscription));
            CommonUtils.setPreference(CommonUtils.RUNTIME_OS, model.getOperatingSystem());
            CommonUtils.setPreference(CommonUtils.RUNTIME_LINUX, getSelectedItem(comboLinuxRuntime));
            CommonUtils.setPreference(CommonUtils.RUNTIME_JAVAVERSION, getSelectedItem(cbJavaVersion));
            CommonUtils.setPreference(CommonUtils.RUNTIME_WEBCONTAINER, getSelectedItem(comboWebContainer));
            CommonUtils.setPreference(CommonUtils.ASP_NAME, getSelectedItem(comboAppServicePlan));
            if (model.isCreatingAppServicePlan()) {
                CommonUtils.setPreference(CommonUtils.ASP_NAME, model.getAppServicePlanName());
                CommonUtils.setPreference(ASP_CREATE_LOCATION, getSelectedItem(comboAppServicePlanLocation));
                CommonUtils.setPreference(ASP_CREATE_PRICING, getSelectedItem(comboAppServicePlanPricingTier));
            }
            CommonUtils.setPreference(CommonUtils.RG_NAME, getSelectedItem(comboResourceGroup));
            if (model.isCreatingResGrp()) {
                CommonUtils.setPreference(CommonUtils.RG_NAME, model.getResourceGroup());
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void okPressed() {
        String errTitle = ERROR_DIALOG_TITLE;
        cleanError();
        collectData();
        recordUserSettings();
        final Map<String, String> properties = model.getTelemetryProperties(new HashMap<String, String>());
        if (!validated()) {
            return;
        }
        try {
            ProgressDialog.get(this.getShell(), CREATE_APP_SERVICE_PROGRESS_TITLE).run(true, true, (monitor) -> {
                monitor.beginTask(VALIDATING_FORM_FIELDS, IProgressMonitor.UNKNOWN);
                monitor.setTaskName(CREATING_APP_SERVICE);
                if (monitor.isCanceled()) {
                    Display.getDefault().asyncExec(() -> AppServiceCreateDialog.super.cancelPressed());
                }

                EventUtil.executeWithLog(WEBAPP, CREATE_WEBAPP, (operation) -> {
                    EventUtil.logEvent(EventType.info, operation, properties);
                    webApp = AzureWebAppMvpModel.getInstance().createWebAppFromSettingModel(model);
                    if (!appSettings.isEmpty()) {
                        final WebAppDraft draft = (WebAppDraft) webApp.update();
                        draft.setAppSettings(appSettings);
                        draft.updateIfExist();
                    }
                    monitor.setTaskName(UPDATING_AZURE_LOCAL_CACHE);
                    Display.getDefault().asyncExec(() -> AppServiceCreateDialog.super.okPressed());
                    if (AzureUIRefreshCore.listeners != null) {
                        AzureUIRefreshCore
                                .execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
                    }
                }, (ex) -> {
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "run@ProgressDialog@okPressed@AppServiceCreateDialog", ex));
                    final String message = ExceptionUtils.getRootCause(ex).getMessage();
                    Display.getDefault().asyncExec(() -> ErrorWindow.go(getShell(), message, errTitle));
                });
            });
        } catch (Exception ex) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "okPressed@AppServiceCreateDialog", ex));
            ErrorWindow.go(getShell(), ex.getMessage(), errTitle);
        }
    }

    protected boolean validated() {
        String webappName = model.getWebAppName();
        if (webappName.length() > 60 || !webappName.matches(WEB_APP_NAME_REGEX)) {
            setError(dec_textAppName, WEB_APP_NAME_INVALID_MSG);
            return false;
        } else {
            for (WebApp wa : Azure.az(AzureWebApp.class).webApps(model.getSubscriptionId()).list()) {
                if (wa != null && wa.name().toLowerCase().equals(webappName.toLowerCase())) {
                    setError(dec_textAppName, NAME_ALREADY_TAKEN);
                    return false;
                }
            }
        }

        if (model.getSubscriptionId() == null || model.getSubscriptionId().isEmpty()) {
            setError(dec_comboSubscription, SELECT_A_VALID_SUBSCRIPTION);
            return false;
        }

        if (model.isCreatingAppServicePlan()) {
            if (model.getAppServicePlanName().isEmpty()) {
                setError(dec_textAppSevicePlanName, ENTER_APP_SERVICE_PLAN_NAME);
                return false;
            } else {
                if (!model.getAppServicePlanName().matches(APP_SERVICE_PLAN_NAME_REGEX)) {
                    setError(dec_textAppSevicePlanName, APP_SERVICE_PLAN_NAME_INVALID_MSG);
                    return false;
                }
                // App service plan name must be unique in each subscription
                List<AppServicePlan> appServicePlans = Azure.az(AzureAppService.class)
                        .plans(model.getSubscriptionId()).list();
                for (AppServicePlan asp : appServicePlans) {
                    if (asp != null && StringUtils.equalsIgnoreCase(asp.name(), model.getAppServicePlanName())) {
                        setError(dec_textAppSevicePlanName, APP_SERVICE_PLAN_NAME_MUST_UNUQUE);
                        return false;
                    }
                }
            }
            if (model.getRegion() == null || model.getRegion().isEmpty()) {
                setError(dec_comboAppServicePlanLocation, SELECT_LOCATION);
                return false;
            }
        } else {
            if (StringUtils.isBlank(model.getAppServicePlanName())) {
                setError(dec_comboAppServicePlan, SELECT_APP_SERVICE_PLAN);
                return false;
            }
        }

        if (model.isCreatingResGrp()) {
            if (model.getResourceGroup() == null || model.getResourceGroup().isEmpty()) {
                setError(dec_textNewResGrName, ENTER_RESOURCE_GROUP);
                return false;
            }
            if (!model.getResourceGroup().matches(RESOURCE_GROUP_NAME_REGEX)) {
                setError(dec_textNewResGrName, RESOURCE_GROUP_NAME_INVALID_MSG);
                return false;
            }
            List<ResourceGroup> list = Azure.az(AzureResources.class).groups(model.getSubscriptionId()).list();
            for (ResourceGroup rg : list) {
                if (rg != null && StringUtils.equalsIgnoreCase(rg.getName(), model.getResourceGroup())) {
                    setError(dec_textNewResGrName, NAME_ALREADY_TAKEN);
                    return false;
                }
            }
        } else {
            if (model.getResourceGroup() == null || model.getResourceGroup().isEmpty()) {
                setError(dec_comboSelectResGr, SELECT_RESOURCE_GROUP);
                return false;
            }
        }
        // todo: add validation for jboss, only v3 pricing is supported
        return true;
    }

    private void collectData() {
        model.setCreatingNew(true);
        model.setWebAppName(textAppName.getText().trim());
        int index = comboSubscription.getSelectionIndex();
        model.setSubscriptionId(index < 0 ? null : binderSubscriptionDetails.get(index).getId());

        // Resource Group
        boolean isCreatingNewResGrp = btnResourceGroupCreateNew.getSelection();
        model.setCreatingResGrp(isCreatingNewResGrp);
        if (isCreatingNewResGrp) {
            model.setResourceGroup(textResourceGroupName.getText().trim());
        } else {
            index = comboResourceGroup.getSelectionIndex();
            model.setResourceGroup(index < 0 ? null : binderResourceGroup.get(index).getName());
        }

        // App Service Plan
        boolean isCreatingAppServicePlan = btnAppServiceCreateNew.getSelection();
        model.setCreatingAppServicePlan(isCreatingAppServicePlan);
        if (isCreatingAppServicePlan) {
            model.setAppServicePlanName(textAppSevicePlanName.getText().trim());

            index = comboAppServicePlanLocation.getSelectionIndex();
            model.setRegion(index < 0 ? null : binderAppServicePlanLocation.get(index).getName());

            model.setPricing(Optional.ofNullable(getSelectedPricingTier()).map(PricingTier::toString)
                    .orElse(APPSETTINGS_TOOLTIP));
        } else {
            index = comboAppServicePlan.getSelectionIndex();
            if(index >= 0) {
                final AppServicePlan plan = binderAppServicePlan.get(index);
                model.setAppServicePlanName(plan.getName());
                model.setAppServicePlanResourceGroupName(plan.getResourceGroupName());
            } else {
                model.setAppServicePlanName(null);
                model.setAppServicePlanResourceGroupName(null);
            }
        }

        // Runtime
        final OperatingSystem os = getSelectedOS();
        final Runtime runtime = os == OperatingSystem.LINUX ? getSelectedRuntimeStack()
                : Runtime.getRuntime(OperatingSystem.WINDOWS, getSelectedWebcontainer(), getSelectedJavaVersion());
        model.saveRuntime(runtime);
    }

    private JavaVersion getSelectedJavaVersion() {
        int index = cbJavaVersion.getSelectionIndex();
        return index < 0 ? null : binderJavaVersions.get(index);
    }

    private WebContainer getSelectedWebcontainer() {
        int index = comboWebContainer.getSelectionIndex();
        return index < 0 ? null : binderWebConteiners.get(index);
    }

    private PricingTier getSelectedPricingTier() {
        final int index = comboAppServicePlanPricingTier.getSelectionIndex();
        return index < 0 ? null : binderAppServicePlanPricingTier.get(index);
    }

    private OperatingSystem getSelectedOS() {
        return btnOSGroupWin.getSelection() ? OperatingSystem.WINDOWS : OperatingSystem.LINUX;
    }

    private Runtime getSelectedRuntimeStack() {
        final int index = comboLinuxRuntime.getSelectionIndex();
        return index < 0 ? null : binderRuntimeStacks.get(index);
    }

    @Nullable
    private Subscription getSelectedSubscription() {
        int i = comboSubscription.getSelectionIndex();
        return i < 0 ? null : binderSubscriptionDetails.get(i);
    }

    private void setComboRefreshingStatus(Combo combo, boolean isRefreshing) {
        combo.removeAll();
        combo.setEnabled(!isRefreshing);
        if (isRefreshing) {
            combo.add(REFRESHING);
            combo.select(0);
        }
    }
}
