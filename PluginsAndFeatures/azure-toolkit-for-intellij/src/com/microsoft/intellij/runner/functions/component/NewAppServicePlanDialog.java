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

package com.microsoft.intellij.runner.functions.component;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.SkuName;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
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
    private static final String DEFAULT_LOCATION = "eastus";
    private static final String RECOMMEND_SUFFIX = " (Recommend)";

    private JPanel contentPane;
    private JPanel pnlNewAppServicePlan;
    private JComboBox cbPricing;
    private JComboBox cbLocation;
    private JTextField txtAppServicePlanName;

    private String subscriptionId;

    private boolean isNewCreatedAppServicePlan;
    private AppServicePlanPanel.AppServicePlanWrapper appServicePlan;

    public NewAppServicePlanDialog(String subscriptionId) {
        super(false);
        setModal(true);
        setTitle("Create App Service Plan");

        this.subscriptionId = subscriptionId;

        cbLocation.setRenderer(new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof Location) {
                    setText(((Location) object).name());
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

        onLoadPricingTier();
        onLoadLocation(subscriptionId);

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
        return res;
    }

    @Override
    protected void doOKAction() {
        appServicePlan = new AppServicePlanPanel.AppServicePlanWrapper(
                txtAppServicePlanName.getText(),
                (Location) cbLocation.getSelectedItem(),
                (PricingTier) cbPricing.getSelectedItem());
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        appServicePlan = null;
        super.doCancelAction();
    }

    private void onLoadLocation(String sid) {
        cbLocation.removeAllItems();
        Observable.fromCallable(() -> AzureMvpModel.getInstance().listLocationsBySubscriptionId(sid))
                  .subscribeOn(Schedulers.newThread())
                  .subscribe(locations -> DefaultLoader.getIdeHelper().invokeLater(() -> fillLocation(locations)));
    }

    private void fillLocation(List<Location> locationList) {
        if (CollectionUtils.isEmpty(locationList)) {
            return;
        }
        locationList.stream().forEach(location -> cbLocation.addItem(location));
        final Location defaultLocation =
                locationList.stream()
                            .filter(location -> StringUtils.equalsAnyIgnoreCase(location.name(), DEFAULT_LOCATION))
                            .findFirst().orElse(locationList.get(0));
        cbLocation.setSelectedItem(defaultLocation);
    }

    /**
     * Load pricing tier from model.
     */
    private void onLoadPricingTier() {
        try {
            cbPricing.removeAllItems();
            cbPricing.addItem(CONSUMPTION_PRICING_TIER);
            cbPricing.setSelectedItem(CONSUMPTION_PRICING_TIER);
            AzureFunctionMvpModel.getInstance().listFunctionPricingTier().stream()
                    .forEach(pricingTier -> cbPricing.addItem(pricingTier));
        } catch (IllegalAccessException e) {
            DefaultLoader.getUIHelper().logError("Failed to load pricing tier", e);
        }
    }

    // We will mark function only pricing tier as recommend
    private static boolean isRecommendPricingTier(PricingTier pricingTier) {
        final String tier = pricingTier.toSkuDescription().tier();
        return pricingTier == CONSUMPTION_PRICING_TIER || StringUtils.equals(SkuName.ELASTIC_PREMIUM.toString(), tier);
    }
}
