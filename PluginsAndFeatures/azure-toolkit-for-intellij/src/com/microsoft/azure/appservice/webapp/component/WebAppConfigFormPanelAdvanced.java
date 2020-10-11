package com.microsoft.azure.appservice.webapp.component;

import com.intellij.ui.components.JBRadioButton;
import com.microsoft.azure.appservice.component.form.AzureFormPanel;
import com.microsoft.azure.appservice.component.form.input.*;
import com.microsoft.azure.appservice.webapp.WebAppConfig;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;

import javax.swing.*;

public class WebAppConfigFormPanelAdvanced extends JPanel implements AzureFormPanel<WebAppConfig> {
    private JPanel contentPanel;

    private JPanel sectionInstanceDetails;

    protected ComboBoxSubscription selectorSubscription;

    protected JTextField textName;
    protected ComboBoxPlatform selectorRuntime;
    protected JBRadioButton radioOsLinux;
    protected JBRadioButton radioOsWindows;
    protected ComboBoxRegion selectorRegion;

    protected JLabel textSku;
    protected ComboBoxDeployment selectorApplication;
    protected ComboBoxResourceGroup selectorGroup;
    protected ComboBoxServicePlan selectorServicePlan;

    public WebAppConfigFormPanelAdvanced() {
        super();
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    @Override
    public WebAppConfig getData() {
        return null;
    }

    private void createUIComponents() {
        this.selectorGroup = new ComboBoxResourceGroup(() -> {
            final Subscription subscription = this.selectorSubscription.getValue();
            final String sid = subscription.subscriptionId();
            return AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(sid);
        });

        //FIXME: move to webapp, seems only webapp has service plan/regions?
        this.selectorServicePlan = new ComboBoxServicePlan(() -> {
            final Subscription subscription = this.selectorSubscription.getValue();
            final String sid = subscription.subscriptionId();
            return AzureWebAppMvpModel.getInstance().listAppServicePlanBySubscriptionId(sid);
        });
        this.selectorRegion = new ComboBoxRegion(() -> {
            final Subscription subscription = this.selectorSubscription.getValue();
            final String sid = subscription.subscriptionId();
            final PricingTier tier = PricingTier.BASIC_B2; // FIXME
            return AzureWebAppMvpModel.getInstance().getAvailableRegions(sid, tier);
        });
    }
}
