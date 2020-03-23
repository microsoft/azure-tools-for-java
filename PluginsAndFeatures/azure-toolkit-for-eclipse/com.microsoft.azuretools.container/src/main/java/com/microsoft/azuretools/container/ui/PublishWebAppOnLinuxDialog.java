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

package com.microsoft.azuretools.container.ui;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DEPLOY_WEBAPP_CONTAINER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.exceptions.InvalidFormDataException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.container.ConsoleLogger;
import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.container.DockerProgressHandler;
import com.microsoft.azuretools.container.ui.common.ContainerSettingComposite;
import com.microsoft.azuretools.container.utils.DockerUtil;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.WebAppOnLinuxDeployPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.WebAppOnLinuxDeployView;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

import rx.Observable;

public class PublishWebAppOnLinuxDialog extends AzureTitleAreaDialogWrapper implements WebAppOnLinuxDeployView {

    private static final String NEED_SIGN_IN = "Please sign in with your Azure account.";
    private static final String MISSING_SERVER_URL = "Please specify a valid Server URL.";
    private static final String MISSING_USERNAME = "Please specify Username.";
    private static final String MISSING_PASSWORD = "Please specify Password.";
    private static final String MISSING_IMAGE_WITH_TAG = "Please specify Image and Tag.";
    private static final String MISSING_WEB_APP = "Please specify Web App on Linux.";
    private static final String MISSING_SUBSCRIPTION = "Please specify Subscription.";
    private static final String MISSING_RESOURCE_GROUP = "Please specify Resource Group.";
    private static final String MISSING_APP_SERVICE_PLAN = "Please specify App Service Plan.";
    private static final String INVALID_IMAGE_WITH_TAG = "Image and Tag name is invalid";
    private static final String INVALID_DOCKER_FILE = "Please specify a valid docker file.";
    // TODO: move to util
    private static final String MISSING_ARTIFACT = "A web archive (.war|.jar) artifact has not been configured.";
    private static final String INVALID_WAR_FILE = "The artifact name %s is invalid. "
            + "An artifact name may contain only the ASCII letters 'a' through 'z' (case-insensitive), "
            + "and the digits '0' through '9', '.', '-' and '_'.";
    private static final String CANNOT_END_WITH_COLON = "Image and tag name cannot end with ':'";
    private static final String REPO_LENGTH_INVALID = "The length of repository name must be at least one character "
            + "and less than 256 characters";
    private static final String CANNOT_END_WITH_SLASH = "The repository name should not end with '/'";
    private static final String REPO_COMPONENT_INVALID = "Invalid repository component: %s, should follow: %s";
    private static final String TAG_LENGTH_INVALID = "The length of tag name must be no more than 128 characters";
    private static final String TAG_INVALID = "Invalid tag: %s, should follow: %s";
    private static final String ARTIFACT_NAME_REGEX = "^[.A-Za-z0-9_-]+\\.(war|jar)$";
    private static final String DOMAIN_NAME_REGEX = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";
    private static final String REPO_COMPONENTS_REGEX = "[a-z0-9]+(?:[._-][a-z0-9]+)*";
    private static final String TAG_REGEX = "^[\\w]+[\\w.-]*$";
    private static final String APP_NAME_PREFIX = "webapp-linux";
    private static final String RESOURCE_GROUP_NAME_PREFIX = "rg-web-linux";
    private static final String APP_SERVICE_PLAN_NAME_PREFIX = "appsp-linux";
    private static final int TAG_LENGTH = 128;
    private static final int REPO_LENGTH = 255;

    private final WebAppOnLinuxDeployPresenter<PublishWebAppOnLinuxDialog> webAppOnLinuxDeployPresenter;
    private final WebAppOnLinuxDeployModel model;
    private String basePath;
    // cached lists of resources
    private List<AppServicePlan> appServicePlanList;
    private List<Location> locationList;
    private List<Subscription> subscriptionList;
    private List<PricingTier> pricingTierList;
    private List<ResourceGroup> resourceGroupList;
    private List<ResourceEx<WebApp>> webAppList;
    // Widgets
    private Button rdoExistingWebApp;
    private Button rdoNewWebApp;
    private WebAppTableComposite cpExisting;
    private NewWebAppComposite cpNew;
    private Composite cpWebApp;
    private ExpandItem webappHolder;
    private ContainerSettingComposite cpAcr;
    private String targetPath;
    private ExpandBar expandBar;
    private ExpandItem acrHolder;

    /**
     * Create the dialog.
     */
    public PublishWebAppOnLinuxDialog(Shell parentShell, String basePath, String targetPath) {
        super(parentShell);
        setShellStyle(SWT.RESIZE | SWT.TITLE);
        this.basePath = basePath;
        this.targetPath = targetPath;
        model = new WebAppOnLinuxDeployModel();
        webAppOnLinuxDeployPresenter = new WebAppOnLinuxDeployPresenter<>();
        webAppOnLinuxDeployPresenter.onAttachView(this);
        setHelpAvailable(false);
    }

    @Override
    public boolean close() {
        boolean ret = super.close();
        if (ret) {
            webAppOnLinuxDeployPresenter.onDetachView();
        }
        return ret;
    }

    @Override
    protected void okPressed() {
        apply();
        try {
            validate();
            execute();
            super.okPressed();
        } catch (InvalidFormDataException e) {
            this.onErrorWithException("Validation Failure", e);
        }
    }

    /**
     * Create contents of the dialog.
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Run on Web App for Containers");
        setMessage("Run on web app for containers");
        Composite area = (Composite) super.createDialogArea(parent);

        expandBar = new ExpandBar(area, SWT.V_SCROLL);
        GridData gdExpandBar = new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1);
        gdExpandBar.minimumWidth = 550;

        expandBar.setLayoutData(gdExpandBar);
        expandBar.setBackground(area.getBackground());

        acrHolder = new ExpandItem(expandBar, SWT.NONE);
        acrHolder.setExpanded(true);
        acrHolder.setText("Azure Container Registry");

        cpAcr = new ContainerSettingComposite(expandBar, SWT.BORDER, basePath);
        acrHolder.setControl(cpAcr);
        acrHolder.setHeight(acrHolder.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

        webappHolder = new ExpandItem(expandBar, SWT.NONE);
        webappHolder.setExpanded(true);
        webappHolder.setText("Web App for Container");

        cpWebApp = new Composite(expandBar, SWT.BORDER);
        webappHolder.setControl(cpWebApp);
        cpWebApp.setLayout(new GridLayout(1, false));

        Composite cpRadioGroup = new Composite(cpWebApp, SWT.NONE);
        cpRadioGroup.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));
        cpRadioGroup.setLayout(new GridLayout(2, false));

        rdoExistingWebApp = new Button(cpRadioGroup, SWT.RADIO);
        rdoExistingWebApp.setSelection(true);
        rdoExistingWebApp.setText("Use Exisiting");

        rdoNewWebApp = new Button(cpRadioGroup, SWT.RADIO);
        rdoNewWebApp.setText("Create New");

        cpExisting = new WebAppTableComposite(cpWebApp, SWT.NONE);
        cpExisting.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));

        cpNew = new NewWebAppComposite(cpWebApp, SWT.NONE);
        cpNew.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));

        updateWebAppHolderWidget();
        resourceGroupRadioGroupLogic();
        aspRadioGroupLogic();

        // Listeners
        // webapp radio group
        rdoNewWebApp.addListener(SWT.Selection, event -> webAppRadioGroupLogic());
        rdoExistingWebApp.addListener(SWT.Selection, event -> webAppRadioGroupLogic());
        // resource group radio group
        cpNew.rdoNewResourceGroup.addListener(SWT.Selection, event -> resourceGroupRadioGroupLogic());
        cpNew.rdoExistingResourceGroup.addListener(SWT.Selection, event -> resourceGroupRadioGroupLogic());
        // app service plan radio group
        cpNew.rdoNewAppServicePlan.addListener(SWT.Selection, event -> aspRadioGroupLogic());
        cpNew.rdoExistingAppServicePlan.addListener(SWT.Selection, event -> aspRadioGroupLogic());

        // subscription selection
        cpNew.cbSubscription.addListener(SWT.Selection, event -> onSubscriptionSelection());
        // resource group selection
        cpNew.cbExistingResourceGroup.addListener(SWT.Selection, event -> onResourceGroupSelection());
        // app service plan selection
        cpNew.cbExistingAppServicePlan.addListener(SWT.Selection, event -> onAppServicePlanSelection());

        // refresh button
        cpExisting.btnRefresh.addListener(SWT.Selection, event -> onBtnRefreshSelection());

        // ACR composite serverUrl text change listener
        cpAcr.addTxtServerUrlModifyListener(event -> onTxtServerUrlModification());

        // adjust shell size when clicking expandBar
        expandBar.addListener(SWT.MouseUp, event -> adjustShellSize());
        cpNew.expandBar.addListener(SWT.MouseUp, event -> adjustShellSize());
        reset();
        return area;
    }

    /**
     * Create contents of the button bar.
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        getShell().pack();
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(600, 800);
    }

    private void apply() {
        model.setDockerFilePath(cpAcr.getDockerfilePath());
        // set ACR info
        model.setPrivateRegistryImageSetting(new PrivateRegistryImageSetting(cpAcr.getServerUrl(), cpAcr.getUserName(),
                cpAcr.getPassword(), cpAcr.getImageTag(), cpAcr.getStartupFile()));
        // set target
        model.setTargetPath(targetPath);
        model.setTargetName(Paths.get(targetPath).getFileName().toString());
        // set web app info
        if (rdoExistingWebApp.getSelection()) {
            // existing web app
            model.setCreatingNewWebAppOnLinux(false);
            ResourceEx<WebApp> selectedWebApp = getSelectedWebApp();
            if (selectedWebApp != null) {
                model.setWebAppId(selectedWebApp.getResource().id());
                model.setWebAppName(selectedWebApp.getResource().name());
                model.setSubscriptionId(selectedWebApp.getSubscriptionId());
                model.setResourceGroupName(selectedWebApp.getResource().resourceGroupName());
            } else {
                model.setWebAppId(null);
                model.setWebAppName(null);
                model.setSubscriptionId(null);
                model.setResourceGroupName(null);
            }
        } else if (rdoNewWebApp.getSelection()) {
            // create new web app
            model.setCreatingNewWebAppOnLinux(true);
            model.setWebAppId("");
            model.setWebAppName(cpNew.txtAppName.getText());
            Subscription selectedSubscription = getSelectedSubscription();
            if (selectedSubscription != null) {
                model.setSubscriptionId(selectedSubscription.subscriptionId());
            }

            // resource group
            if (cpNew.rdoExistingResourceGroup.getSelection()) {
                // existing RG
                model.setCreatingNewResourceGroup(false);
                ResourceGroup selectedRg = getSelectedResourceGroup();
                if (selectedRg != null) {
                    model.setResourceGroupName(selectedRg.name());
                } else {
                    model.setResourceGroupName(null);
                }
            } else if (cpNew.rdoNewResourceGroup.getSelection()) {
                // new RG
                model.setCreatingNewResourceGroup(true);
                model.setResourceGroupName(cpNew.txtNewResourceGroupName.getText());
            }

            // app service plan
            if (cpNew.rdoNewAppServicePlan.getSelection()) {
                model.setCreatingNewAppServicePlan(true);
                model.setAppServicePlanName(cpNew.txtAppServicePlanName.getText());
                Location selectedLocation = getSelectedLocation();
                if (selectedLocation != null) {
                    model.setLocationName(selectedLocation.region().name());
                } else {
                    model.setLocationName(null);
                }

                PricingTier selectedPricingTier = getSelectedPricingTier();
                if (selectedPricingTier != null) {
                    model.setPricingSkuTier(selectedPricingTier.toSkuDescription().tier());
                    model.setPricingSkuSize(selectedPricingTier.toSkuDescription().size());
                } else {
                    model.setPricingSkuTier(null);
                    model.setPricingSkuSize(null);
                }
            } else if (cpNew.rdoExistingAppServicePlan.getSelection()) {
                model.setCreatingNewAppServicePlan(false);
                AppServicePlan selectedAsp = getSelectedAppServicePlan();
                if (selectedAsp != null) {
                    model.setAppServicePlanId(selectedAsp.id());
                } else {
                    model.setAppServicePlanId(null);
                }
            }
        }
    }

    private void validate() throws InvalidFormDataException {
        if (!AuthMethodManager.getInstance().isSignedIn()) {
            throw new InvalidFormDataException(NEED_SIGN_IN);
        }
        // docker file
        if (Utils.isEmptyString(model.getDockerFilePath())) {
            throw new InvalidFormDataException(INVALID_DOCKER_FILE);
        }
        File dockerFile = Paths.get(model.getDockerFilePath()).toFile();
        if (!dockerFile.exists() || !dockerFile.isFile()) {
            throw new InvalidFormDataException(INVALID_DOCKER_FILE);
        }
        // acr
        PrivateRegistryImageSetting setting = model.getPrivateRegistryImageSetting();
        if (Utils.isEmptyString(setting.getServerUrl()) || !setting.getServerUrl().matches(DOMAIN_NAME_REGEX)) {
            throw new InvalidFormDataException(MISSING_SERVER_URL);
        }
        if (Utils.isEmptyString(setting.getUsername())) {
            throw new InvalidFormDataException(MISSING_USERNAME);
        }
        if (Utils.isEmptyString(setting.getPassword())) {
            throw new InvalidFormDataException(MISSING_PASSWORD);
        }
        String imageTag = setting.getImageTagWithServerUrl();
        if (Utils.isEmptyString(imageTag)) {
            throw new InvalidFormDataException(MISSING_IMAGE_WITH_TAG);
        }
        if (imageTag.endsWith(":")) {
            throw new InvalidFormDataException(CANNOT_END_WITH_COLON);
        }
        final String[] repoAndTag = imageTag.split(":");

        // check repository first
        if (repoAndTag[0].length() < 1 || repoAndTag[0].length() > REPO_LENGTH) {
            throw new InvalidFormDataException(REPO_LENGTH_INVALID);
        }
        if (repoAndTag[0].endsWith("/")) {
            throw new InvalidFormDataException(CANNOT_END_WITH_SLASH);
        }
        final String[] repoComponents = repoAndTag[0].split("/");
        for (String component : repoComponents) {
            if (!component.matches(REPO_COMPONENTS_REGEX)) {
                throw new InvalidFormDataException(
                        String.format(REPO_COMPONENT_INVALID, component, REPO_COMPONENTS_REGEX));
            }
        }
        // check when contains tag
        if (repoAndTag.length == 2) {
            if (repoAndTag[1].length() > TAG_LENGTH) {
                throw new InvalidFormDataException(TAG_LENGTH_INVALID);
            }
            if (!repoAndTag[1].matches(TAG_REGEX)) {
                throw new InvalidFormDataException(String.format(TAG_INVALID, repoAndTag[1], TAG_REGEX));
            }
        }
        if (repoAndTag.length > 2) {
            throw new InvalidFormDataException(INVALID_IMAGE_WITH_TAG);
        }
        // web app
        if (model.isCreatingNewWebAppOnLinux()) {
            if (Utils.isEmptyString(model.getWebAppName())) {
                throw new InvalidFormDataException(MISSING_WEB_APP);
            }
            if (Utils.isEmptyString(model.getSubscriptionId())) {
                throw new InvalidFormDataException(MISSING_SUBSCRIPTION);
            }
            if (Utils.isEmptyString(model.getResourceGroupName())) {
                throw new InvalidFormDataException(MISSING_RESOURCE_GROUP);
            }

            if (model.isCreatingNewAppServicePlan()) {
                if (Utils.isEmptyString(model.getAppServicePlanName())) {
                    throw new InvalidFormDataException(MISSING_APP_SERVICE_PLAN);
                }
            } else {
                if (Utils.isEmptyString(model.getAppServicePlanId())) {
                    throw new InvalidFormDataException(MISSING_APP_SERVICE_PLAN);
                }
            }

        } else {
            if (Utils.isEmptyString(model.getWebAppId())) {
                throw new InvalidFormDataException(MISSING_WEB_APP);
            }
        }

        // target package
        if (Utils.isEmptyString(model.getTargetName())) {
            throw new InvalidFormDataException(MISSING_ARTIFACT);
        }
        if (!model.getTargetName().matches(ARTIFACT_NAME_REGEX)) {
            throw new InvalidFormDataException(String.format(INVALID_WAR_FILE, model.getTargetName()));
        }
    }

    private void execute() {
        Operation operation = TelemetryManager.createOperation(WEBAPP, DEPLOY_WEBAPP_CONTAINER);
        Observable.fromCallable(() -> {
            ConsoleLogger.info("Starting job ...  ");
            operation.start();
            if (basePath == null) {
                ConsoleLogger.error("Project base path is null.");
                throw new FileNotFoundException("Project base path is null.");
            }
            // locate artifact to specified location
            String targetFilePath = model.getTargetPath();
            ConsoleLogger.info(String.format("Locating artifact ... [%s]", targetFilePath));

            // validate dockerfile
            Path targetDockerfile = Paths.get(model.getDockerFilePath());
            ConsoleLogger.info(String.format("Validating dockerfile ... [%s]", targetDockerfile));
            if (!targetDockerfile.toFile().exists()) {
                throw new FileNotFoundException("Dockerfile not found.");
            }
            // replace placeholder if exists
            String content = new String(Files.readAllBytes(targetDockerfile));
            content = content.replaceAll(Constant.DOCKERFILE_ARTIFACT_PLACEHOLDER,
                    Paths.get(basePath).toUri().relativize(Paths.get(targetFilePath).toUri()).getPath());
            Files.write(targetDockerfile, content.getBytes());

            // build image
            PrivateRegistryImageSetting acrInfo = model.getPrivateRegistryImageSetting();
            ConsoleLogger.info(String.format("Building image ...  [%s]", acrInfo.getImageTagWithServerUrl()));
            DockerClient docker = DefaultDockerClient.fromEnv().build();

            DockerUtil.buildImage(docker, acrInfo.getImageTagWithServerUrl(), targetDockerfile.getParent(),
                    targetDockerfile.getFileName().toString(), new DockerProgressHandler());

            // push to ACR
            ConsoleLogger.info(String.format("Pushing to ACR ... [%s] ", acrInfo.getServerUrl()));
            DockerUtil.pushImage(docker, acrInfo.getServerUrl(), acrInfo.getUsername(), acrInfo.getPassword(),
                    acrInfo.getImageTagWithServerUrl(), new DockerProgressHandler());

            // deploy
            if (model.isCreatingNewWebAppOnLinux()) {
                // create new WebApp
                ConsoleLogger.info(String.format("Creating new WebApp ... [%s]", model.getWebAppName()));
                WebApp app = AzureWebAppMvpModel.getInstance().createWebAppWithPrivateRegistryImage(model);

                if (app != null && app.name() != null) {
                    ConsoleLogger.info(String.format("URL:  http://%s.azurewebsites.net/", app.name()));

                    AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
                }
            } else {
                // update WebApp
                ConsoleLogger.info(String.format("Updating WebApp ... [%s]", model.getWebAppName()));
                WebApp app = AzureWebAppMvpModel.getInstance().updateWebAppOnDocker(model.getSubscriptionId(),
                        model.getWebAppId(), acrInfo);
                if (app != null && app.name() != null) {
                    ConsoleLogger.info(String.format("URL:  http://%s.azurewebsites.net/", app.name()));
                }
            }
            return null;
        }).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
            ret -> {
                ConsoleLogger.info("Updating cache ... ");
                AzureWebAppMvpModel.getInstance().listAllWebAppsOnLinux(true);
                ConsoleLogger.info("Job done");
                if (model.isCreatingNewWebAppOnLinux() && AzureUIRefreshCore.listeners != null) {
                    AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
                }
                sendTelemetry(true, null);
                operation.complete();
            },
            err -> {
                err.printStackTrace();
                ConsoleLogger.error(err.getMessage());
                EventUtil.logError(operation, ErrorType.systemError, new Exception(err), null, null);
                operation.complete();
                sendTelemetry(false, err.getMessage());
            });
    }

    private void sendTelemetry(boolean success, @Nullable String errorMsg) {
        Map<String, String> map = new HashMap<>();
        map.put("SubscriptionId", model.getSubscriptionId());
        map.put("CreateNewApp", String.valueOf(model.isCreatingNewWebAppOnLinux()));
        map.put("CreateNewSP", String.valueOf(model.isCreatingNewAppServicePlan()));
        map.put("CreateNewRGP", String.valueOf(model.isCreatingNewResourceGroup()));
        map.put("Success", String.valueOf(success));
        String fileType = "";
        if (null != model.getTargetName()) {
            fileType = FilenameUtils.getExtension(model.getTargetName());
        }
        map.put("FileType", fileType);
        if (!success) {
            map.put("ErrorMsg", errorMsg);
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "Webapp (Linux)", "Deploy", map);
    }

    // get selected items in Combo
    private ResourceEx<WebApp> getSelectedWebApp() {
        ResourceEx<WebApp> selectedWebApp = null;
        int index = cpExisting.tblWebApps.getSelectionIndex();
        if (webAppList != null && index >= 0 && index < webAppList.size()) {
            selectedWebApp = webAppList.get(index);
        }
        return selectedWebApp;
    }

    private AppServicePlan getSelectedAppServicePlan() {
        AppServicePlan asp = null;
        int index = cpNew.cbExistingAppServicePlan.getSelectionIndex();
        if (appServicePlanList != null && index >= 0 && index < appServicePlanList.size()) {
            asp = appServicePlanList.get(index);
        }
        return asp;
    }

    private PricingTier getSelectedPricingTier() {
        PricingTier pt = null;
        int index = cpNew.cbPricingTier.getSelectionIndex();
        if (pricingTierList != null && index >= 0 && index < pricingTierList.size()) {
            pt = pricingTierList.get(index);
        }
        return pt;
    }

    private Location getSelectedLocation() {
        Location loc = null;
        int locIndex = cpNew.cbLocation.getSelectionIndex();
        if (locationList != null && locIndex >= 0 && locIndex < locationList.size()) {
            loc = locationList.get(locIndex);
        }
        return loc;
    }

    private ResourceGroup getSelectedResourceGroup() {
        ResourceGroup rg = null;
        int rgIndex = cpNew.cbExistingResourceGroup.getSelectionIndex();
        if (resourceGroupList != null && rgIndex >= 0 && rgIndex < resourceGroupList.size()) {
            rg = resourceGroupList.get(rgIndex);
        }
        return rg;
    }

    private Subscription getSelectedSubscription() {
        Subscription sub = null;
        int subsIndex = cpNew.cbSubscription.getSelectionIndex();
        if (subscriptionList != null && subsIndex >= 0 && subsIndex < subscriptionList.size()) {
            sub = subscriptionList.get(subsIndex);
        }
        return sub;
    }

    // helpers
    private void reset() {
        // set default value
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());
        cpNew.txtAppName.setText(String.format("%s-%s", APP_NAME_PREFIX, date));
        cpNew.txtNewResourceGroupName.setText(String.format("%s-%s", RESOURCE_GROUP_NAME_PREFIX, date));
        cpNew.txtAppServicePlanName.setText(String.format("%s-%s", APP_SERVICE_PLAN_NAME_PREFIX, date));
        // set default Dockerfile path
        String defaultDockerFilePath = DockerUtil.getDefaultDockerFilePathIfExist(basePath);
        cpAcr.setDockerfilePath(defaultDockerFilePath);
        // load ACRs
        cpAcr.onListRegistries();
        // load webapps
        cpExisting.btnRefresh.setEnabled(false);
        webAppOnLinuxDeployPresenter.onLoadAppList();
        // load subscriptions
        webAppOnLinuxDeployPresenter.onLoadSubscriptionList();
        // load pricing tiers
        webAppOnLinuxDeployPresenter.onLoadPricingTierList();
    }

    private void updateWebAppHolderWidget() {
        cpExisting.setVisible(rdoExistingWebApp.getSelection());
        ((GridData) cpExisting.getLayoutData()).exclude = !rdoExistingWebApp.getSelection();
        cpNew.setVisible(rdoNewWebApp.getSelection());
        ((GridData) cpNew.getLayoutData()).exclude = !rdoNewWebApp.getSelection();
        // resize expandItem
        webappHolder.setHeight(webappHolder.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        ((GridData) expandBar.getLayoutData()).widthHint = acrHolder.getControl().computeSize(SWT.DEFAULT,
                SWT.DEFAULT).x;
        getShell().layout();
    }

    private void adjustShellSize() {
        updateWebAppHolderWidget();
        // resize the whole shell
        getShell().pack();
    }

    // Event listeners
    private void webAppRadioGroupLogic() {
        adjustShellSize();
    }

    private void aspRadioGroupLogic() {
        cpNew.cbExistingAppServicePlan.setEnabled(cpNew.rdoExistingAppServicePlan.getSelection());

        cpNew.txtAppServicePlanName.setEnabled(cpNew.rdoNewAppServicePlan.getSelection());
        cpNew.cbLocation.setEnabled(cpNew.rdoNewAppServicePlan.getSelection());
        cpNew.cbPricingTier.setEnabled(cpNew.rdoNewAppServicePlan.getSelection());
    }

    private void resourceGroupRadioGroupLogic() {
        cpNew.txtNewResourceGroupName.setEnabled(cpNew.rdoNewResourceGroup.getSelection());
        cpNew.cbExistingResourceGroup.setEnabled(cpNew.rdoExistingResourceGroup.getSelection());
    }

    private void onBtnRefreshSelection() {
        cpExisting.btnRefresh.setEnabled(false);
        cpExisting.tblWebApps.removeAll();
        webAppOnLinuxDeployPresenter.onRefreshList();
    }

    private void onAppServicePlanSelection() {
        cpNew.lblLocationValue.setText("N/A");
        cpNew.lblPricingTierValue.setText("N/A");
        AppServicePlan asp = getSelectedAppServicePlan();
        if (asp != null) {
            cpNew.lblLocationValue.setText(asp.regionName());
            cpNew.lblPricingTierValue.setText(asp.pricingTier().toString());
        }
    }

    private void onResourceGroupSelection() {
        cpNew.cbExistingAppServicePlan.removeAll();
        cpNew.lblLocationValue.setText("");
        cpNew.lblPricingTierValue.setText("");
        Subscription sub = getSelectedSubscription();
        ResourceGroup rg = getSelectedResourceGroup();
        if (sub != null && rg != null) {
            // TODO: a minor bug here, if rg is null, related labels should be set to "N/A"
            webAppOnLinuxDeployPresenter.onLoadAppServicePlan(sub.subscriptionId(), rg.name());
        }
    }

    private void onSubscriptionSelection() {
        cpNew.cbExistingResourceGroup.removeAll();
        cpNew.cbLocation.removeAll();
        Subscription sb = getSelectedSubscription();
        if (sb != null) {
            webAppOnLinuxDeployPresenter.onLoadResourceGroup(sb.subscriptionId());
            webAppOnLinuxDeployPresenter.onLoadLocationList(sb.subscriptionId());
        }
    }

    private void onTxtServerUrlModification() {
        // calculate size of expandbar
        ((GridData) expandBar.getLayoutData()).widthHint = acrHolder.getControl().computeSize(SWT.DEFAULT,
                SWT.DEFAULT).x;
        // resize the whole shell
        getShell().pack();
    }

    // Implementation of WebAppOnLinuxDeployView
    @Override
    public void renderAppServicePlanList(List<AppServicePlan> list) {
        appServicePlanList = list;
        cpNew.cbExistingAppServicePlan.removeAll();
        for (AppServicePlan asp : appServicePlanList) {
            cpNew.cbExistingAppServicePlan.add(asp.name());
        }
        if (cpNew.cbExistingAppServicePlan.getItemCount() > 0) {
            cpNew.cbExistingAppServicePlan.select(0);
        }
        onAppServicePlanSelection();
    }

    @Override
    public void renderLocationList(List<Location> list) {
        locationList = list;
        cpNew.cbLocation.removeAll();
        for (Location l : locationList) {
            cpNew.cbLocation.add(l.displayName());
        }
        if (cpNew.cbLocation.getItemCount() > 0) {
            cpNew.cbLocation.select(0);
        }
    }

    @Override
    public void renderPricingTierList(List<PricingTier> list) {
        pricingTierList = list;
        cpNew.cbPricingTier.removeAll();
        for (PricingTier pt : pricingTierList) {
            cpNew.cbPricingTier.add(pt.toString());
        }
        if (cpNew.cbPricingTier.getItemCount() > 0) {
            cpNew.cbPricingTier.select(0);
        }
    }

    @Override
    public void renderResourceGroupList(List<ResourceGroup> list) {
        resourceGroupList = list;
        cpNew.cbExistingResourceGroup.removeAll();
        for (ResourceGroup rg : resourceGroupList) {
            cpNew.cbExistingResourceGroup.add(rg.name());
        }
        if (cpNew.cbExistingResourceGroup.getItemCount() > 0) {
            cpNew.cbExistingResourceGroup.select(0);
        }
        onResourceGroupSelection();
    }

    @Override
    public void renderSubscriptionList(List<Subscription> list) {
        subscriptionList = list;
        cpNew.cbSubscription.removeAll();
        for (Subscription sub : subscriptionList) {
            cpNew.cbSubscription.add(sub.displayName());
        }
        if (cpNew.cbSubscription.getItemCount() > 0) {
            cpNew.cbSubscription.select(0);
        }
        onSubscriptionSelection();
    }

    @Override
    public void renderWebAppOnLinuxList(List<ResourceEx<WebApp>> list) {
        cpExisting.btnRefresh.setEnabled(true);
        webAppList = list.stream().sorted((a, b) -> a.getSubscriptionId().compareToIgnoreCase(b.getSubscriptionId()))
                .collect(Collectors.toList());

        // TODO: where to show loading ...

        if (webAppList.size() > 0) {
            cpExisting.tblWebApps.removeAll();
            for (ResourceEx<WebApp> resource : webAppList) {
                WebApp app = resource.getResource();
                TableItem it = new TableItem(cpExisting.tblWebApps, SWT.NULL);
                it.setText(new String[] {app.name(), app.resourceGroupName()});
            }
        }
    }
}