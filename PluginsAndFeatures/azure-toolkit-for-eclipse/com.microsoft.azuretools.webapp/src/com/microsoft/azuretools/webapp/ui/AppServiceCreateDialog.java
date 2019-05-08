/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azuretools.webapp.ui;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;
import static com.microsoft.azuretools.webapp.util.CommonUtils.ASP_CREATE_LOCATION;
import static com.microsoft.azuretools.webapp.util.CommonUtils.ASP_CREATE_PRICING;
import static com.microsoft.azuretools.webapp.util.CommonUtils.getSelectedItem;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.JdkModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.core.ui.ErrorWindow;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.core.utils.ProgressDialog;
import com.microsoft.azuretools.core.utils.UpdateProgressIndicator;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.azuretools.utils.WebAppUtils.WebContainerMod;
import com.microsoft.azuretools.webapp.Activator;
import com.microsoft.azuretools.webapp.util.CommonUtils;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import rx.Observable;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;

public class AppServiceCreateDialog extends AppServiceBaseDialog {

    private static final String WEB_CONFIG_PACKAGE_PATH = "/webapp/web.config";

    // validation error string constants
    private static final String SELECT_WEB_CONTAINER = "Select a valid web container.";
    private static final String SELECT_JAVA_VERSION = "Select Java version.";
    private static final String SELECT_RESOURCE_GROUP = "Select a valid resource group.";
    private static final String ENTER_RESOURCE_GROUP = "Enter a valid resource group name";
    private static final String SELECT_APP_SERVICE_PLAN = "Select a valid App Service Plan.";
    private static final String SELECT_LOCATION = "Select a location.";
    private static final String SELECT_A_VALID_SUBSCRIPTION = "Select a valid subscription.";
    private static final String ENTER_APP_SERVICE_PLAN_NAME = "Enter a valid App Service Plan name.";
    private static final String NAME_ALREADY_TAKEN = "The name is already taken";
    private static final String APP_SERVICE_PLAN_NAME_MUST_UNUQUE = "App service plan name must be unuque in each "
        + "subscription.";
    private static final String APP_SERVICE_PLAN_NAME_INVALID_MSG = "App Service Plan name can only include "
        + "alphanumeric characters and hyphens.";
    private static final String RESOURCE_GROUP_NAME_INVALID_MSG = "Resounce group name can only include alphanumeric "
        + "characters, periods, underscores, hyphens, and parenthesis and can't end in a period.";
    private static final String WEB_APP_NAME_INVALID_MSG = "The name can contain letters, numbers and hyphens but the"
        + " first and last characters must be a letter or number. The length must be between 2 and 60 characters.";

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
    public static final PricingTier DEFAULT_PRICINGTIER = new PricingTier("Premium", "P1V2");
    private static final JavaVersion DEFAULT_JAVA_VERSION = JavaVersion.JAVA_8_NEWEST;
    private static final RuntimeStack DEFAULT_LINUX_RUNTIME = RuntimeStack.TOMCAT_8_5_JRE8;
    private static final WebContainerMod DEFAULT_WEB_CONTAINER = WebContainerMod.Newest_Tomcat_85;
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
    private static final String GETTING_APP_SERVICES = "Getting App Services...";
    private static final String DIALOG_TITLE = "Create App Service";
    private static final String DIALOG_MESSAGE = "Create Azure App Service";

    // tooltip
    private static final String APPSETTINGS_TOOLTIP = "You can configure application setting here, such as "
        + "\"JAVA_OPTS\"";


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
    private List<WebAppUtils.WebContainerMod> binderWebConteiners;
    private List<SubscriptionDetail> binderSubscriptionDetails;
    private List<ResourceGroup> binderResourceGroup;
    private List<AppServicePlan> binderAppServicePlan;
    private List<Location> binderAppServicePlanLocation;
    private List<PricingTier> binderAppServicePlanPricingTier;
    private List<JdkModel> javaVersions;

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
    private String packaging = WebAppUtils.TYPE_WAR;

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
                fillAppServicePlanLocations();
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

        fillLinuxRuntime();
        fillWebContainers();
        fillSubscriptions();
        String os = CommonUtils.getPreference(CommonUtils.RUNTIME_OS);
        if (os.equalsIgnoreCase(OperatingSystem.LINUX.toString())) {
            btnOSGroupLinux.setSelection(true);
            btnOSGroupWin.setSelection(false);
        } else if (os.equalsIgnoreCase(OperatingSystem.WINDOWS.toString())) {
            btnOSGroupLinux.setSelection(false);
            btnOSGroupWin.setSelection(true);
        }
        radioRuntimeLogic();
        fillResourceGroups();
        fillAppServicePlans();
        fillAppServicePlansDetails();
        fillAppServicePlanLocations();
        fillAppServicePlanPricingTiers();
        fillJavaVersion();
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

        lblAppServiceUseExictingLocation = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServiceUseExictingLocation.setEnabled(true);
        GridData gd_lblAppServiceUseExictingLocation = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceUseExictingLocation.horizontalIndent = 20;
        lblAppServiceUseExictingLocation.setLayoutData(gd_lblAppServiceUseExictingLocation);
        lblAppServiceUseExictingLocation.setText(LBL_LOCATION);

        lblAppSevicePlanLocation = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppSevicePlanLocation.setEnabled(true);
        lblAppSevicePlanLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblAppSevicePlanLocation.setText(NOT_AVAILABLE);

        lblAppServiceUseExistiogPrisingTier = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServiceUseExistiogPrisingTier.setEnabled(true);
        GridData gd_lblAppServiceUseExistiogPrisingTier = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceUseExistiogPrisingTier.horizontalIndent = 20;
        lblAppServiceUseExistiogPrisingTier.setLayoutData(gd_lblAppServiceUseExistiogPrisingTier);
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

        lblAppServiceCreateNewLocation = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gd_lblAppServiceCreateNewLocation = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceCreateNewLocation.horizontalIndent = 20;
        lblAppServiceCreateNewLocation.setLayoutData(gd_lblAppServiceCreateNewLocation);
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

        lblAppServiceCreateNewPricingTier = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gd_lblAppServiceCreateNewPricingTier = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceCreateNewPricingTier.horizontalIndent = 20;
        lblAppServiceCreateNewPricingTier.setLayoutData(gd_lblAppServiceCreateNewPricingTier);
        lblAppServiceCreateNewPricingTier.setText(LBL_PRICING_TIER);
        lblAppServiceCreateNewPricingTier.setEnabled(false);

        comboAppServicePlanPricingTier = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlanPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboAppServicePlanPricingTier.setEnabled(false);

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
                            // FALL THROUGH
                        case SWT.TRAVERSE_ESCAPE:
                            text.dispose();
                            e.doit = false;
                            readTblAppSettings();
                            updateTableActionBtnStatus(true);
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
        item.setText(new String[]{"<key>", "<value>"});
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
            if (packaging.equals(WebAppUtils.TYPE_JAR)) {
                lblWebContainer.setEnabled(false);
                comboWebContainer.setEnabled(false);
            }
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

    protected void fillWebContainers() {
        try {
            if (MavenUtils.isMavenProject(project)) {
                packaging = MavenUtils.getPackaging(project);
            }
        } catch (Exception e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "fillWebContainers@AppServiceCreateDialog", e));
        }
        if (packaging.equals(WebAppUtils.TYPE_JAR)) {
            lblWebContainer.setVisible(false);
            comboWebContainer.setVisible(false);
        } else {
            comboWebContainer.removeAll();
            binderWebConteiners = new ArrayList<>();
            WebContainerMod[] webContainers = WebContainerMod.values();
            for (int i = 0; i < webContainers.length; i++) {
                WebContainerMod webContainerMod = webContainers[i];
                comboWebContainer.add(webContainerMod.toString());
                binderWebConteiners.add(webContainerMod);
                if (webContainerMod == DEFAULT_WEB_CONTAINER) {
                    comboWebContainer.select(i);
                }
            }
            String webContainer = CommonUtils.getPreference(CommonUtils.RUNTIME_WEBCONTAINER);
            CommonUtils.selectComboIndex(comboWebContainer, webContainer);
        }
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

    protected void fillSubscriptions() {
        if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
            updateAndFillSubscriptions();
        } else {
            doFillSubscriptions();
        }
    }

    private void updateAndFillSubscriptions() {
        try {
            ProgressDialog.get(this.getShell(), GETTING_APP_SERVICES).run(true, true, (monitor) -> {
                monitor.beginTask(UPDATING_AZURE_LOCAL_CACHE, IProgressMonitor.UNKNOWN);
                if (monitor.isCanceled()) {
                    AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                    Display.getDefault().asyncExec(() -> AppServiceCreateDialog.super.cancelPressed());
                }
                try {
                    AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor));
                    Display.getDefault().asyncExec(() -> doFillSubscriptions());
                } catch (Exception ex) {
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "run@ProgressDialog@updateAndFillSubscriptions@AppServiceCreateDialog", ex));
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "updateAndFillSubscriptions@AppServiceCreateDialog",
                ex));
        }
    }

    private void doFillSubscriptions() {
        try {
            // reset model
            Set<SubscriptionDetail> sdl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().keySet();
            if (sdl == null) {
                return;
            }

            comboSubscription.removeAll();
            binderSubscriptionDetails = new ArrayList<>();
            for (SubscriptionDetail sd : sdl) {
                if (sd != null && sd.isSelected()) {
                    comboSubscription.add(sd.getSubscriptionName());
                    binderSubscriptionDetails.add(sd);
                }
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
        int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            return;
        }

        List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap()
            .get(binderSubscriptionDetails.get(i));
        if (rgl == null) {
            return;
        }
        Collections.sort(rgl, Comparator.comparing(ResourceGroup::name));

        comboResourceGroup.removeAll();
        binderResourceGroup = new ArrayList<>();
        for (ResourceGroup rg : rgl) {
            comboResourceGroup.add(rg.name());
            binderResourceGroup.add(rg);
        }

        if (comboResourceGroup.getItemCount() > 0) {
            comboResourceGroup.select(0);
        }
        String resourceGroup = CommonUtils.getPreference(CommonUtils.RG_NAME);
        CommonUtils.selectComboIndex(comboResourceGroup, resourceGroup);
    }

    protected void fillAppServicePlans() {
        int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            return;
        }

        List<AppServicePlan> appServicePlans = getAppservicePlanBySID(
            binderSubscriptionDetails.get(i).getSubscriptionId());
        if (appServicePlans == null) {
            return;
        }

        comboAppServicePlan.removeAll();
        binderAppServicePlan = new ArrayList<>();
        appServicePlans.sort(Comparator.comparing(AppServicePlan::name));
        for (AppServicePlan asp : appServicePlans) {
            if (chooseWin) {
                if (asp.operatingSystem() == null || asp.operatingSystem() == OperatingSystem.WINDOWS) {
                    binderAppServicePlan.add(asp);
                    comboAppServicePlan.add(asp.name());
                }
            } else {
                if (asp.operatingSystem() == null || asp.operatingSystem() == OperatingSystem.LINUX) {
                    binderAppServicePlan.add(asp);
                    comboAppServicePlan.add(asp.name());
                }
            }
        }

        if (comboAppServicePlan.getItemCount() > 0) {
            comboAppServicePlan.select(0);
        }
        String aspName = CommonUtils.getPreference(CommonUtils.ASP_NAME);
        CommonUtils.selectComboIndex(comboAppServicePlan, aspName);
        fillAppServicePlansDetails();
    }

    private List<AppServicePlan> getAppservicePlanBySID(String sid) {
        try {
            if (sidAspMap.containsKey(sid)) {
                return sidAspMap.get(sid);
            }
            List<AppServicePlan> appServicePlans = AzureWebAppMvpModel.getInstance()
                .listAppServicePlanBySubscriptionId(sid);
            sidAspMap.put(sid, appServicePlans);
            return appServicePlans;
        } catch (Exception e) {
            return null;
        }
    }

    public static void initAspCache() {
        try {
            Map<String, List<AppServicePlan>> map = new ConcurrentHashMap<>();
            Set<SubscriptionDetail> sdl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().keySet();
            Observable.from(sdl).flatMap((sd) ->
                Observable.create((subscriber) -> {
                    try {
                        List<AppServicePlan> appServicePlans = AzureWebAppMvpModel.getInstance()
                            .listAppServicePlanBySubscriptionId(sd.getSubscriptionId());
                        map.put(sd.getSubscriptionId(), appServicePlans);
                        subscriber.onCompleted();
                    } catch (Exception e) {
                        Exceptions.propagate(e);
                    }
                }).subscribeOn(Schedulers.io()), sdl.size()).subscribeOn(Schedulers.io()).toBlocking().subscribe();
            sidAspMap = map;
        } catch (Exception ignore) {
        }
    }

    protected void fillAppServicePlansDetails() {
        int i = comboAppServicePlan.getSelectionIndex();
        if (i < 0) {
            lblAppSevicePlanLocation.setText(NOT_AVAILABLE);
            lblAppServicePlanPricingTier.setText(NOT_AVAILABLE);
        } else {
            AppServicePlan asp = binderAppServicePlan.get(i);
            lblAppSevicePlanLocation.setText(asp.region().label());
            lblAppServicePlanPricingTier.setText(asp.pricingTier().toString());
        }
    }

    protected void fillAppServicePlanLocations() {
        int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            return;
        }

        comboAppServicePlanLocation.removeAll();
        binderAppServicePlanLocation = new ArrayList<>();
        Map<SubscriptionDetail, List<Location>> sdlocMap = AzureModel.getInstance().getSubscriptionToLocationMap();
        SubscriptionDetail sd = binderSubscriptionDetails.get(i);
        List<Location> locl = sdlocMap.get(sd);
        if (locl != null) {
            for (i = 0; i < locl.size(); i++) {
                Location loc = locl.get(i);
                comboAppServicePlanLocation.add(loc.displayName());
                binderAppServicePlanLocation.add(loc);
                if (loc.name().equals(DEFAULT_REGION.name())) {
                    comboAppServicePlanLocation.select(i);
                }
            }
            if (comboAppServicePlanLocation.getSelectionIndex() < 0 && comboAppServicePlanLocation.getItemCount() > 0) {
                comboAppServicePlanLocation.select(0);
            }
            String aspLocation = CommonUtils.getPreference(ASP_CREATE_LOCATION);
            CommonUtils.selectComboIndex(comboAppServicePlanLocation, aspLocation);
        }
    }

    protected void fillAppServicePlanPricingTiers() {
        try {
            comboAppServicePlanPricingTier.removeAll();
            binderAppServicePlanPricingTier = new ArrayList<>();
            List<PricingTier> pricingTiers = AzureMvpModel.getInstance().listPricingTier();
            for (int i = 0; i < pricingTiers.size(); i++) {
                PricingTier pricingTier = pricingTiers.get(i);
                comboAppServicePlanPricingTier.add(pricingTier.toString());
                binderAppServicePlanPricingTier.add(pricingTier);
                if (pricingTier.equals(DEFAULT_PRICINGTIER)) {
                    comboAppServicePlanPricingTier.select(i);
                }
            }
            if (comboAppServicePlanPricingTier.getSelectionIndex() < 0
                && comboAppServicePlanPricingTier.getItemCount() > 0) {
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
        List<RuntimeStack> runtimeStacks = WebAppUtils.getAllJavaLinuxRuntimeStacks();
        for (int i = 0; i < runtimeStacks.size(); i++) {
            RuntimeStack runtimeStack = runtimeStacks.get(i);
            comboLinuxRuntime.add(runtimeStack.stack() + " " + runtimeStack.version());
            if (runtimeStack == DEFAULT_LINUX_RUNTIME) {
                comboLinuxRuntime.select(i);
            }
        }
        String linuxRuntime = CommonUtils.getPreference(CommonUtils.RUNTIME_LINUX);
        CommonUtils.selectComboIndex(comboLinuxRuntime, linuxRuntime);
    }

    protected void fillJavaVersion() {
        javaVersions = AzureWebAppMvpModel.getInstance().listJdks();
        for (int i = 0; i < javaVersions.size(); i++) {
            JdkModel jdk = javaVersions.get(i);
            cbJavaVersion.add(jdk.toString());
            if (jdk.getJavaVersion() == DEFAULT_JAVA_VERSION) {
                cbJavaVersion.select(i);
            }
        }
        String javaversion = CommonUtils.getPreference(CommonUtils.RUNTIME_JAVAVERSION);
        CommonUtils.selectComboIndex(cbJavaVersion, javaversion);
    }

    private void recordUserSettings() {
        try {
            CommonUtils.setPreference(CommonUtils.SUBSCRIPTION, getSelectedItem(comboSubscription));
            CommonUtils.setPreference(CommonUtils.RUNTIME_OS, model.getOS().toString());
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
        Map<String, String> properties = new HashMap<>();
        properties.put("runtime",
            model.getOS() == OperatingSystem.LINUX ? "Linux-" + model.getLinuxRuntime().toString()
                : "windows-" + model.getWebContainer());
        if (!validated()) {
            return;
        }
        try {
            ProgressDialog.get(this.getShell(), CREATE_APP_SERVICE_PROGRESS_TITLE).run(true, true,
                (monitor) -> {
                    monitor.beginTask(VALIDATING_FORM_FIELDS, IProgressMonitor.UNKNOWN);
                    monitor.setTaskName(CREATING_APP_SERVICE);
                    if (monitor.isCanceled()) {
                        AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                        Display.getDefault().asyncExec(() -> AppServiceCreateDialog.super.cancelPressed());
                    }

                    EventUtil.executeWithLog(WEBAPP, CREATE_WEBAPP, (operation) -> {
                        EventUtil.logEvent(EventType.info, operation, properties);
                        webApp = AzureWebAppMvpModel.getInstance().createWebApp(model);
                        if (!appSettings.isEmpty()) {
                            webApp.update().withAppSettings(appSettings).apply();
                        }
                        if (webApp != null && packaging.equals(WebAppUtils.TYPE_JAR) && chooseWin) {
                            webApp.stop();
                            try (InputStream webConfigInput = WebAppUtils.class
                                .getResourceAsStream(WEB_CONFIG_PACKAGE_PATH)) {
                                WebAppUtils.uploadWebConfig(webApp, webConfigInput,
                                    new UpdateProgressIndicator(monitor));
                            }
                            webApp.start();
                        }
                        monitor.setTaskName(UPDATING_AZURE_LOCAL_CACHE);
                        AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor));
                        initAspCache();
                        Display.getDefault().asyncExec(() -> AppServiceCreateDialog.super.okPressed());
                        if (AzureUIRefreshCore.listeners != null) {
                            AzureUIRefreshCore.execute(
                                new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
                        }
                    }, (ex) -> {
                            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                "run@ProgressDialog@okPressed@AppServiceCreateDialog", ex));
                            Display.getDefault().asyncExec(() -> ErrorWindow.go(getShell(), ex.getMessage(), errTitle));
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
        } else if (AzureModel.getInstance().getResourceGroupToWebAppMap() != null) {
            for (List<WebApp> wal : AzureModel.getInstance().getResourceGroupToWebAppMap().values()) {
                for (WebApp wa : wal) {
                    if (wa != null && wa.name().toLowerCase().equals(webappName.toLowerCase())) {
                        setError(dec_textAppName, NAME_ALREADY_TAKEN);
                        return false;
                    }
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
                List<AppServicePlan> appServicePlans = getAppservicePlanBySID(model.getSubscriptionId());
                for (AppServicePlan asp : appServicePlans) {
                    if (asp != null
                        && asp.name().toLowerCase().equals(model.getAppServicePlanName().toLowerCase())) {
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
            if (model.getAppServicePlanId() == null || model.getAppServicePlanId().isEmpty()) {
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
            for (ResourceGroup rg : AzureMvpModel.getInstance()
                .getResourceGroupsBySubscriptionId(model.getSubscriptionId())) {
                if (rg != null && rg.name().toLowerCase().equals(model.getResourceGroup().toLowerCase())) {
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

        if (model.getJdkVersion() == null && chooseWin) {
            setError(dec_cbJavaVersion, SELECT_JAVA_VERSION);
            return false;
        }

        if (model.getWebContainer() == null || model.getWebContainer().isEmpty() && chooseWin) {
            setError(dec_comboWebContainer, SELECT_WEB_CONTAINER);
            return false;
        }

        return true;
    }

    private void collectData() {
        model.setCreatingNew(true);
        model.setWebAppName(textAppName.getText().trim());
        int index = comboSubscription.getSelectionIndex();
        model.setSubscriptionId(index < 0 ? null : binderSubscriptionDetails.get(index).getSubscriptionId());

        // Resource Group
        boolean isCreatingNewResGrp = btnResourceGroupCreateNew.getSelection();
        model.setCreatingResGrp(isCreatingNewResGrp);
        if (isCreatingNewResGrp) {
            model.setResourceGroup(textResourceGroupName.getText().trim());
        } else {
            index = comboResourceGroup.getSelectionIndex();
            model.setResourceGroup(index < 0 ? null : binderResourceGroup.get(index).name());
        }

        // App Service Plan
        boolean isCreatingAppServicePlan = btnAppServiceCreateNew.getSelection();
        model.setCreatingAppServicePlan(isCreatingAppServicePlan);
        if (isCreatingAppServicePlan) {
            model.setAppServicePlanName(textAppSevicePlanName.getText().trim());

            index = comboAppServicePlanLocation.getSelectionIndex();
            model.setRegion(index < 0 ? null : binderAppServicePlanLocation.get(index).name());

            index = comboAppServicePlanPricingTier.getSelectionIndex();
            model.setPricing(index < 0 ? null : binderAppServicePlanPricingTier.get(index).toString());
        } else {
            index = comboAppServicePlan.getSelectionIndex();
            model.setAppServicePlanId(index < 0 ? null : binderAppServicePlan.get(index).id());
        }

        // Runtime
        chooseWin = btnOSGroupWin.getSelection();
        model.setOS(chooseWin ? OperatingSystem.WINDOWS : OperatingSystem.LINUX);
        if (chooseWin) {
            index = cbJavaVersion.getSelectionIndex();
            model.setJdkVersion(index < 0 ? null : javaVersions.get(index).getJavaVersion());

            // Windows does not provider java se parameter, and the api here needs a parameter, so here just use
            // TOMCAT_8.5_NEWEST.
            // The App services itself start a jar file based on the web.config
            if (packaging.equals(WebAppUtils.TYPE_JAR)) {
                model.setWebContainer(WebContainer.TOMCAT_8_5_NEWEST.toString());
            } else {
                index = comboWebContainer.getSelectionIndex();
                model.setWebContainer(index < 0 ? null : binderWebConteiners.get(index).toWebContainer().toString());
            }
        } else {
            String linuxRuntime = comboLinuxRuntime.getItem(comboLinuxRuntime.getSelectionIndex());
            String[] part = linuxRuntime.split(" ");
            model.setStack(part[0]);
            model.setVersion(part[1]);
        }
    }

}
