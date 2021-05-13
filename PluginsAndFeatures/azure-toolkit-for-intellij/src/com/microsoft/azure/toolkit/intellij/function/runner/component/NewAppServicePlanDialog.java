/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.SkuName;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.ValidationUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class NewAppServicePlanDialog extends AzureDialogWrapper {

    public static final String CONSUMPTION = "Consumption";
    public static final PricingTier CONSUMPTION_PRICING_TIER = new PricingTier("Consumption", "");
    private static final String DEFAULT_REGION = "eastus";
    private static final String RECOMMEND_SUFFIX = " (Recommended)";
    private static final String MISSING_PRICING_TIER_PROMPT = "Please set pricing tier for app service plan";
    private static final String MISSING_REGION_PROMPT = "Please set region for app service plan";

    private JPanel contentPane;
    private JPanel pnlNewAppServicePlan;
    private JComboBox cbPricing;
    private JComboBox cbRegion;
    private JTextField txtAppServicePlanName;

    private String subscriptionId;

    private boolean isNewCreatedAppServicePlan;
    private AppServicePlanPanel.AppServicePlanWrapper appServicePlan;

    public NewAppServicePlanDialog(String subscriptionId) {
        super(false);
        setModal(true);
        setTitle("Create App Service Plan");

        this.subscriptionId = subscriptionId;

        cbRegion.setRenderer(new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof Region) {
                    setText(((Region) object).getLabel());
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        cbPricing.setRenderer(new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof PricingTier) {
                    final PricingTier pricingTier = (PricingTier) object;
                    final String text = pricingTier == CONSUMPTION_PRICING_TIER ? CONSUMPTION : pricingTier.toString();
                    setText(isRecommendPricingTier(pricingTier) ? text + RECOMMEND_SUFFIX : text);
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        cbPricing.addItemListener(e -> onLoadRegion());

        onLoadPricingTier();
        init();
    }

    public AppServicePlanPanel.AppServicePlanWrapper getAppServicePlan() {
        return appServicePlan;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> res = new ArrayList<>();
        try {
            ValidationUtils.validateAppServicePlanName(txtAppServicePlanName.getText());
        } catch (IllegalArgumentException iae) {
            res.add(new ValidationInfo(iae.getMessage(), txtAppServicePlanName));
        }
        if (cbPricing.getSelectedItem() == null) {
            res.add(new ValidationInfo(MISSING_PRICING_TIER_PROMPT, cbPricing));
        }
        if (cbRegion.getSelectedItem() == null) {
            res.add(new ValidationInfo(MISSING_REGION_PROMPT, cbRegion));
        }
        return res;
    }

    @Override
    protected void doOKAction() {
        appServicePlan = new AppServicePlanPanel.AppServicePlanWrapper(
                txtAppServicePlanName.getText(),
                (Region) cbRegion.getSelectedItem(),
                (PricingTier) cbPricing.getSelectedItem());
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        appServicePlan = null;
        super.doCancelAction();
    }

    private void onLoadRegion() {
        cbRegion.removeAllItems();
        Observable.fromCallable(() -> AzureWebAppMvpModel
                .getInstance().getAvailableRegions(subscriptionId, (PricingTier) cbPricing.getSelectedItem()))
                  .subscribeOn(Schedulers.newThread())
                  .subscribe(locations -> DefaultLoader.getIdeHelper().invokeLater(() -> fillRegion(locations)));
    }

    private void fillRegion(List<Region> regionList) {
        if (CollectionUtils.isEmpty(regionList)) {
            return;
        }
        regionList.stream().forEach(region -> cbRegion.addItem(region));
        final Region defaultLocation =
                regionList.stream()
                          .filter(region -> StringUtils.equalsAnyIgnoreCase(region.getName(), DEFAULT_REGION))
                          .findFirst().orElse(regionList.get(0));
        cbRegion.setSelectedItem(defaultLocation);
    }

    /**
     * Load pricing tier from model.
     */
    private void onLoadPricingTier() {
        cbPricing.removeAllItems();
        cbPricing.addItem(CONSUMPTION_PRICING_TIER);
        cbPricing.setSelectedItem(CONSUMPTION_PRICING_TIER);
        AzureFunctionMvpModel.getInstance().listFunctionPricingTier().stream()
                .forEach(pricingTier -> cbPricing.addItem(pricingTier));
    }

    // We will mark function only pricing tier as recommend
    private static boolean isRecommendPricingTier(PricingTier pricingTier) {
        final String tier = pricingTier.toSkuDescription().tier();
        return pricingTier == CONSUMPTION_PRICING_TIER || StringUtils.equals(SkuName.ELASTIC_PREMIUM.toString(), tier);
    }
}
