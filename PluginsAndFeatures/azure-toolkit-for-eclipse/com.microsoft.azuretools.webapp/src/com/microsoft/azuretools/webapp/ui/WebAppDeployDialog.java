package com.microsoft.azuretools.webapp.ui;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.ui.ErrorWindow;
import com.microsoft.azuretools.core.ui.views.AzureDeploymentProgressNotification;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.core.utils.ProgressDialog;
import com.microsoft.azuretools.core.utils.UpdateProgressIndicator;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.CanceledByUserException;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.azuretools.utils.WebAppUtils.WebAppDetails;
import com.microsoft.azuretools.webapp.Activator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jst.j2ee.datamodel.properties.IJ2EEComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@SuppressWarnings("restriction")
public class WebAppDeployDialog extends AzureTitleAreaDialogWrapper {
    private static ILog LOG = Activator.getDefault().getLog();

    private Table table;
    private Browser browserAppServiceDetailes;
    private Button btnDeployToRoot;
    private String browserFontStyle;
    private Button btnDelete;

    private IProject project;
    private Shell parentShell;

    final String ftpLinkString = "ShowFtpCredentials";

    private Map<String, WebAppDetails> webAppDetailsMap = new HashMap<>();

    /**
     * Create the dialog.
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
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Select App Service to deploy to:");
        setTitle("Deploy Web App");
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(2, false));
        GridData gd_container = new GridData(GridData.FILL_BOTH);
        gd_container.widthHint = 750;
        container.setLayoutData(gd_container);

        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        gd_table.heightHint = 300;
        table.setLayoutData(gd_table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn tblclmnName = new TableColumn(table, SWT.LEFT);
        tblclmnName.setWidth(230);
        tblclmnName.setText("Name");

        TableColumn tblclmnJdk = new TableColumn(table, SWT.LEFT);
        tblclmnJdk.setWidth(60);
        tblclmnJdk.setText("JDK");

        TableColumn tblclmnWebContainer = new TableColumn(table, SWT.LEFT);
        tblclmnWebContainer.setWidth(110);
        tblclmnWebContainer.setText("Web container");

        TableColumn tblclmnResourceGroup = new TableColumn(table, SWT.LEFT);
        tblclmnResourceGroup.setWidth(190);
        tblclmnResourceGroup.setText("Resource group");

        Composite composite = new Composite(container, SWT.NONE);
        composite.setLayout(new RowLayout(SWT.VERTICAL));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Button btnCreate = new Button(composite, SWT.NONE);
        btnCreate.setLayoutData(new RowData(90, SWT.DEFAULT));
        btnCreate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendTelemetry("CREATE");
                createAppService(project);
                //cleanError();
            }
        });
        btnCreate.setText("Create...");

        btnDelete = new Button(composite, SWT.NONE);
        btnDelete.setEnabled(false);
        btnDelete.setLayoutData(new RowData(90, SWT.DEFAULT));
        btnDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendTelemetry("DELETE");
                deleteAppService();
                //cleanError();
            }
        });
        btnDelete.setText("Delete...");

        Button btnRefresh = new Button(composite, SWT.NONE);
        btnRefresh.setLayoutData(new RowData(90, SWT.DEFAULT));
        btnRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendTelemetry("REFRESH");
                //cleanError();
                table.removeAll();
                //browserAppServiceDetailes.setText("");
                fillAppServiceDetails();
                AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                fillTable();
            }
        });
        btnRefresh.setText("Refresh");

        Group grpAppServiceDetails = new Group(container, SWT.NONE);
        grpAppServiceDetails.setLayout(new FillLayout(SWT.HORIZONTAL));
        GridData gd_grpAppServiceDetails = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_grpAppServiceDetails.heightHint = 150;
        grpAppServiceDetails.setLayoutData(gd_grpAppServiceDetails);
        grpAppServiceDetails.setText("App service details");

        browserAppServiceDetailes = new Browser(grpAppServiceDetails, SWT.NONE);
        FontData browserFontData = btnRefresh.getFont().getFontData()[0];
        //browserFontStyle = String.format("font-family: '%s';", browserFontData.getHeight(), browserFontData.getName());
        browserFontStyle = String.format("font-family: '%s'; font-size: 9pt;", browserFontData.getName());
        browserAppServiceDetailes.addLocationListener(new LocationListener() {
            @Override
            public void changing(LocationEvent event) {
                try {
                    //System.out.println("LocationEvent.location: " + event.location);
                    if (event.location.contains(ftpLinkString)) {
                        event.doit = false;
                        showFtpCreadentialsWindow();
                    }
                    if (event.location.contains("http")) {
                        event.doit = false;
                        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.location));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "changing@LocationListener@browserAppServiceDetailes@AppServiceCreateDialog", ex));
                }
            }
            @Override
            public void changed(LocationEvent event) {}
        });
        new Label(container, SWT.NONE);

        btnDeployToRoot = new Button(container, SWT.CHECK);
        btnDeployToRoot.setText("Deploy to root");
        try {
            if (MavenUtils.isMavenProject(project) && MavenUtils.getPackaging(project).equals(WebAppUtils.TYPE_JAR)) {
                btnDeployToRoot.setSelection(true);
                btnDeployToRoot.setVisible(false);
            }
        } catch (Exception e) {
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "WebAppDeployDialog", e));
            e.printStackTrace();
        }
        new Label(container, SWT.NONE);

        table.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                fillAppServiceDetails();
            }
        });

        return area;
    }

    @Override
    public void create() {
        super.create();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                System.out.println("fillTable() async");
                fillTable();
            }
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

    private void cleanError() {
        setErrorMessage(null);
    }

    private void fillAppServiceDetails() {
        validated();
        int selectedRow = table.getSelectionIndex();
        if (selectedRow < 0) {
            browserAppServiceDetailes.setText("");
            btnDelete.setEnabled(false);
            return;
        }

        btnDelete.setEnabled(true);
        String appServiceName = table.getItems()[selectedRow].getText(0);
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        SubscriptionDetail sd = wad.subscriptionDetail;
        AppServicePlan asp = wad.appServicePlan;

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"margin: 7px 7px 7px 7px; " + browserFontStyle + "\">");
        sb.append(String.format("App Service name:&nbsp;<b>%s</b>;<br/>", appServiceName));
        sb.append(String.format("Subscription name:&nbsp;<b>%s</b>;&nbsp;id:&nbsp;<b>%s</b>;<br/>", sd.getSubscriptionName(), sd.getSubscriptionId()));
        String aspName = asp == null ? "N/A" : asp.name();
        String aspPricingTier = asp == null ? "N/A" : asp.pricingTier().toString();
        sb.append(String.format("App Service Plan name:&nbsp;<b>%s</b>;&nbsp;Pricing tier:&nbsp;<b>%s</b>;<br/>", aspName, aspPricingTier));

        String link = buildSiteLink(wad.webApp, null);
        sb.append(String.format("Link:&nbsp;<a href=\"%s\">%s</a><br/>", link, link));
        sb.append(String.format("<a href=\"%s\">%s</a>", ftpLinkString, "Show FTP deployment credentials"));
        sb.append("</div>");
        browserAppServiceDetailes.setText(sb.toString());
    }

    private static String buildSiteLink(WebApp webApp, String artifactName) {
        String appServiceLink = "https://" + webApp.defaultHostName();
        if (artifactName != null && !artifactName.isEmpty())
            return appServiceLink + "/" + artifactName;
        else
            return appServiceLink;
    }

    private void updateAndFillTable() {
        try {
            ProgressDialog.get(getShell(), "Update Azure Local Cache Progress").run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Updating Azure local cache...", IProgressMonitor.UNKNOWN);
                    try {
                        if (monitor.isCanceled()) {
                            throw new CanceledByUserException();
                        }

                        AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor));

                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                doFillTable();
                            }
                        });
                    } catch (CanceledByUserException ex) {
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("updateAndFillTable(): Canceled by user");
                                cancelPressed();
                            }
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@updateAndFillTable@AppServiceCreateDialog", ex));
                    }
                    monitor.done();
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "updateAndFillTable@AppServiceCreateDialog", ex));
        }
    }

    private void doFillTable() {
        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.getInstance().getSubscriptionToResourceGroupMap();
        Map<ResourceGroup, List<WebApp>> rgwaMap = AzureModel.getInstance().getResourceGroupToWebAppMap();
        Map<ResourceGroup, List<AppServicePlan>> rgaspMap = AzureModel.getInstance().getResourceGroupToAppServicePlanMap();

        webAppDetailsMap.clear();
        table.removeAll();

        for (SubscriptionDetail sd : srgMap.keySet()) {
            if (!sd.isSelected()) continue;

            Map<String, WebAppUtils.AspDetails> aspMap = new HashMap<>();
            for (ResourceGroup rg : srgMap.get(sd)) {
                for (AppServicePlan asp : rgaspMap.get(rg)) {
                    aspMap.put(asp.id(), new WebAppUtils.AspDetails(asp, rg));
                }
            }

            for (ResourceGroup rg : srgMap.get(sd)) {
                for (WebApp wa : rgwaMap.get(rg)) {
                    TableItem item = new TableItem(table, SWT.NULL);

                    if (wa.javaVersion() != JavaVersion.OFF) {
                        item.setText(new String[] {
                            wa.name(),
                            wa.javaVersion().toString(),
                            wa.javaContainer() + " " + wa.javaContainerVersion(),
                            wa.resourceGroupName()
                        });
                    } else {
                        item.setText(new String[] {
                            wa.name(),
                            "Off",
                            "N/A",
                            wa.resourceGroupName()
                        });
                    }

                    WebAppDetails webAppDetails = new WebAppDetails();
                    webAppDetails.webApp = wa;
                    webAppDetails.subscriptionDetail = sd;
                    webAppDetails.resourceGroup = rg;
                    webAppDetails.appServicePlan = aspMap.get(wa.appServicePlanId()).getAsp();
                    webAppDetails.appServicePlanResourceGroup = aspMap.get(wa.appServicePlanId()).getRg();
                    webAppDetailsMap.put(wa.name(), webAppDetails);
                }
            }
        }
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
        cleanError();
        int selectedRow = table.getSelectionIndex();
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (selectedRow < 0) {
            //setErrorMessage("Select App Service to deploy to.");
            okButton.setEnabled(false);
            return false;
        }
        String appServiceName = table.getItems()[selectedRow].getText(0);
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);
        if (wad.webApp.javaVersion()  == JavaVersion.OFF ) {
            setErrorMessage("Select java based App Service");
            okButton.setEnabled(false);
            return false;
        }
        okButton.setEnabled(true);
        return true;
    }

    @Override
    protected void okPressed () {
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
            deploy(artifactName, destinationPath);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "okPressed@AppServiceCreateDialog", ex));
        };
        super.okPressed();
    }

    private void export(String projectName, String destinationPath) throws Exception {

        System.out.println("Building project '" + projectName + "'...");
//        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
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
        WebApp webApp = wad.webApp;
        boolean isDeployToRoot = btnDeployToRoot.getSelection();
        String errTitle = "Deploy Web App Error";
        String sitePath = buildSiteLink(wad.webApp,  isDeployToRoot ? null : artifactName);
        //Map<String, String> threadParams = new HashMap<>();
        //threadParams.put("sitePath", sitePath);
        String jobDescription = String.format("Web App '%s' deployment", webApp.name());
        String deploymentName = UUID.randomUUID().toString();
        AzureDeploymentProgressNotification.createAzureDeploymentProgressNotification(deploymentName, jobDescription);

        Job job = new Job(jobDescription) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                String message = "Packaging Artifact...";
                String cancelMessage = "Interrupted by user";
                String successMessage = "";
                String errorMessage = "Error";
                Map<String, String> postEventProperties = new HashMap<String, String>();
                postEventProperties.put("Java App Name", project.getName());

                monitor.beginTask(message, IProgressMonitor.UNKNOWN);
                try {
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 5, message);
                    if (MavenUtils.isMavenProject(project)) {
                        MavenUtils.executePackageGoal(project);
                    } else {
                        export(artifactName, artifactPath);
                    }
                    message = "Deploying Web App...";
                    monitor.setTaskName(message);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 35, message);
                    PublishingProfile pp = webApp.getPublishingProfile();
                    webApp.stop();
                    WebAppUtils.deployArtifact(artifactName, artifactPath,
                            pp, isDeployToRoot, new UpdateProgressIndicator(monitor));
                    webApp.start();

                    if (monitor.isCanceled()) {
                        AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, null, -1, cancelMessage);
                        return Status.CANCEL_STATUS;
                    }

                    message = "Checking Web App availability...";
                    monitor.setTaskName(message);
                    //monitor.subTask("Link: " + sitePath);
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 50, message);

                    // to make warn up cancelable
                    int stepLimit = 5;
                    int sleepMs = 1000;
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (int step = 0; step < stepLimit; ++step) {
                                    if (WebAppUtils.isUrlAccessible(sitePath))  { // warm up
                                        break;
                                    }
                                    Thread.sleep(sleepMs);
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@Thread@run@ProgressDialog@deploy@AppServiceCreateDialog@SingInDialog", ex));
                            } catch (InterruptedException ex) {
                                System.out.println("The thread is interupted");
                            }
                        }
                    });
                    thread.start();
                    while (thread.isAlive()) {
                        if (monitor.isCanceled()) {
                            // it's published but not warmed up yet - consider as success
                            AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 100, successMessage);
                            return Status.CANCEL_STATUS;
                        }
                        else Thread.sleep(sleepMs);
                    }

                    monitor.done();
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, sitePath, 100, successMessage);
                } catch (Exception ex) {
                    //threadParams.put("sitePath", null);
                    postEventProperties.put("PublishError", ex.getMessage());
                    ex.printStackTrace();
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@deploy@AppServiceCreateDialog", ex));
                    AzureDeploymentProgressNotification.notifyProgress(this, deploymentName, null, -1, errorMessage);
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            ErrorWindow.go(parentShell, ex.getMessage(), errTitle);;
                        }
                    });
                }
                AppInsightsClient.create("Deploy as WebApp", "", postEventProperties);
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void deleteAppService() {
        int selectedRow = table.getSelectionIndex();
        if (selectedRow < 0) {
            return;
        }
        String appServiceName = table.getItems()[selectedRow].getText(0);
        WebAppDetails wad = webAppDetailsMap.get(appServiceName);

        boolean confirmed = MessageDialog.openConfirm(getShell(),
                "Delete App Service",
                "Do you really want to delete the App Service '" + appServiceName + "'?");
        if (!confirmed) {
            return;
        }

        String errTitle = "Delete App Service Error";
        try{
            ProgressDialog.get(this.getShell(), "Delete App Service Progress").run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) {
                    monitor.beginTask("Deleting App Service...", IProgressMonitor.UNKNOWN);

                    try {
                        WebAppUtils.deleteAppService(wad);
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                table.remove(selectedRow);
                                //browserAppServiceDetailes.setText("");
                                fillAppServiceDetails();
                            };
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@deleteAppService@AppServiceCreateDialog", ex));
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                ErrorWindow.go(getShell(), ex.getMessage(), errTitle);;
                            }
                        });

                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "deleteAppService@AppServiceCreateDialog", ex));
            ErrorWindow.go(getShell(), ex.getMessage(), errTitle);
        }
    }

    private void sendTelemetry(String action){
        final Map<String, String> properties = new HashMap<>();
        properties.put("Window", this.getClass().getSimpleName());
        properties.put("Title", this.getShell().getText());
        AppInsightsClient.createByType(AppInsightsClient.EventType.Dialog, this.getClass().getSimpleName(), action, properties);
    }
}
