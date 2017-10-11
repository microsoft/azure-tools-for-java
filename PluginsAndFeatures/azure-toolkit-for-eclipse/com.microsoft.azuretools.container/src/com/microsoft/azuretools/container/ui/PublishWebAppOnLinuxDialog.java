package com.microsoft.azuretools.container.ui;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.FillLayout;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.WebAppOnLinuxDeployPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.WebAppOnLinuxDeployView;

public class PublishWebAppOnLinuxDialog extends TitleAreaDialog implements WebAppOnLinuxDeployView {

    private final WebAppOnLinuxDeployPresenter<PublishWebAppOnLinuxDialog> webAppOnLinuxDeployPresenter;
    private Button rdoExistingWebApp;
    private Button rdoNewWebApp;
    private WebAppTableComposite cpExisting;
    private NewWebAppComposite cpNew;
    private Composite cpWebApp;
    private ExpandItem webappHolder;
    private List<AppServicePlan> appServicePlanList;
    private List<Location> locationList;
    private List<Subscription> subscriptionList;
    private List<PricingTier> pricingTierList;
    private List<ResourceGroup> resourceGroupList;
    private List<ResourceEx<WebApp>> webAppList;

    @Override
    protected void okPressed() {
        // TODO: validation & execution
        boolean validated = true;
        if (!validated) {
            return;
        }
        execute();
        super.okPressed();
    }

    private void execute() {
        WebAppOnLinuxDeployModel model = new WebAppOnLinuxDeployModel();
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

        // TODO
    }

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

    @Override
    public boolean close() {
        boolean ret = super.close();
        if (ret) {
            webAppOnLinuxDeployPresenter.onDetachView();
        }
        return ret;
    }

    /**
     * Create the dialog.
     * 
     * @param parentShell
     */
    public PublishWebAppOnLinuxDialog(Shell parentShell) {
        super(parentShell);
        webAppOnLinuxDeployPresenter = new WebAppOnLinuxDeployPresenter<>();
        webAppOnLinuxDeployPresenter.onAttachView(this);
        setHelpAvailable(false);
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        ExpandBar expandBar = new ExpandBar(container, SWT.NONE);
        expandBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        ExpandItem acrHolder = new ExpandItem(expandBar, SWT.NONE);
        acrHolder.setExpanded(true);
        acrHolder.setText("Azure Container Registry");

        Composite cpAcr = new Composite(expandBar, SWT.NONE);
        acrHolder.setControl(cpAcr);
        acrHolder.setHeight(acrHolder.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

        webappHolder = new ExpandItem(expandBar, SWT.NONE);
        webappHolder.setExpanded(true);
        webappHolder.setText("Web App On Linux");

        cpWebApp = new Composite(expandBar, SWT.NONE);
        webappHolder.setControl(cpWebApp);
        cpWebApp.setLayout(new GridLayout(1, false));

        Composite cpRadioGroup = new Composite(cpWebApp, SWT.NONE);
        cpRadioGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        cpRadioGroup.setLayout(new GridLayout(2, false));

        rdoExistingWebApp = new Button(cpRadioGroup, SWT.RADIO);
        rdoExistingWebApp.setSelection(true);
        rdoExistingWebApp.setText("Use Exisiting");

        rdoNewWebApp = new Button(cpRadioGroup, SWT.RADIO);
        rdoNewWebApp.setText("Create New");

        cpExisting = new WebAppTableComposite(cpWebApp, SWT.NONE);
        cpExisting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        cpExisting.setLayout(new FillLayout(SWT.HORIZONTAL));

        cpNew = new NewWebAppComposite(cpWebApp, SWT.NONE);
        cpNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        webAppRadioGroupLogic();
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
        initialize();
        return area;
    }

    private void onBtnRefreshSelection() {
        cpExisting.btnRefresh.setEnabled(false);
        cpExisting.tblWebApps.removeAll();
        webAppOnLinuxDeployPresenter.onRefreshList();
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

    private void initialize() {
        // load webapps
        cpExisting.btnRefresh.setEnabled(false);
        webAppOnLinuxDeployPresenter.onLoadAppList();
        // load subscriptions
        webAppOnLinuxDeployPresenter.onLoadSubscriptionList();
        // load pricing tiers
        webAppOnLinuxDeployPresenter.onLoadPricingTierList();
    }

    /**
     * Create contents of the button bar.
     * 
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(689, 742);
    }

    private void webAppRadioGroupLogic() {
        cpExisting.setVisible(rdoExistingWebApp.getSelection());
        ((GridData) cpExisting.getLayoutData()).exclude = !rdoExistingWebApp.getSelection();
        cpNew.setVisible(rdoNewWebApp.getSelection());
        ((GridData) cpNew.getLayoutData()).exclude = !rdoNewWebApp.getSelection();
        cpWebApp.layout();
        webappHolder.setHeight(webappHolder.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
    }

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
                it.setText(new String[] { app.name(), app.resourceGroupName() });
            }
        }
    }
}