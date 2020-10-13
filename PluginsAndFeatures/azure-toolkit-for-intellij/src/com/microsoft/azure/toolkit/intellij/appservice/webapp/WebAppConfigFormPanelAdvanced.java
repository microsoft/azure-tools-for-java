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

package com.microsoft.azure.toolkit.intellij.appservice.webapp;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.intellij.appservice.component.AppServiceConfigPanel;
import com.microsoft.azure.toolkit.intellij.appservice.component.input.*;
import com.microsoft.azure.toolkit.lib.AzureFormInput;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class WebAppConfigFormPanelAdvanced extends JPanel implements AppServiceConfigPanel<WebAppConfig> {
    private static final String NOT_APPLICABLE = "N/A";

    private JPanel contentPanel;

    private ComboBoxSubscription selectorSubscription;
    private ComboBoxResourceGroup selectorGroup;

    private TextInputAppName textName;
    private ComboBoxPlatform selectorPlatform;
    private ComboBoxRegion selectorRegion;

    private JLabel textSku;
    private ComboBoxDeployment selectorApplication;
    private ComboBoxServicePlan selectorServicePlan;

    public WebAppConfigFormPanelAdvanced() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public WebAppConfig getData() {
        final Subscription subscription = this.selectorSubscription.getValue();
        final ResourceGroup resourceGroup = this.selectorGroup.getValue();

        final String name = this.textName.getValue();
        final Platform platform = this.selectorPlatform.getValue();
        final Region region = this.selectorRegion.getValue();

        final AppServicePlan servicePlan = this.selectorServicePlan.getValue();

        final Path path = this.selectorApplication.getValue();

        return WebAppConfig.builder()
                           .subscription(subscription)
                           .resourceGroup(resourceGroup)
                           .name(name)
                           .platform(platform)
                           .region(region)
                           .servicePlan(servicePlan)
                           .application(path)
                           .build();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
                this.selectorSubscription,
                this.selectorGroup,
                this.textName,
                this.selectorPlatform,
                this.selectorRegion,
                this.selectorApplication,
                this.selectorServicePlan
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    private void init() {
        this.textSku.setBorder(new EmptyBorder(0, 5, 0, 0));
        this.textSku.setText(NOT_APPLICABLE);
        this.selectorServicePlan.addItemListener(this::onServicePlanChanged);
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        //TODO: @wangmi try subscription mechanism? e.g. this.selectorGroup.subscribe(this.selectSubscription)
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = e.getStateChange() == ItemEvent.SELECTED ? (Subscription) e.getItem() : null;
            this.selectorGroup.setSubscription(subscription);
            this.textName.setSubscription(subscription);
            this.selectorRegion.setSubscription(subscription);
            this.selectorServicePlan.setSubscription(subscription);
        }
    }

    private void onServicePlanChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final AppServicePlan plan = (AppServicePlan) e.getItem();
            this.textSku.setText(plan.pricingTier().toString());
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.textSku.setText(NOT_APPLICABLE);
        }
    }
}
