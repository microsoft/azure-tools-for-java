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

import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azure.toolkit.intellij.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.appservice.component.input.*;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppConfig;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ItemEvent;
import java.nio.file.Path;

public class WebAppConfigFormPanelAdvanced extends JPanel implements AzureFormPanel<WebAppConfig> {
    private static final String NOT_APPLICABLE = "N/A";

    private JPanel contentPanel;

    protected ComboBoxSubscription selectorSubscription;

    protected JTextField textName;
    protected ComboBoxPlatform selectorPlatform;
    protected ComboBoxRegion selectorRegion;

    protected JLabel textSku;
    protected ComboBoxDeployment selectorApplication;
    protected ComboBoxResourceGroup selectorGroup;
    protected ComboBoxServicePlan selectorServicePlan;

    public WebAppConfigFormPanelAdvanced() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public WebAppConfig getData() {
        final Subscription subscription = this.selectorSubscription.getValue();
        final ResourceGroup resourceGroup = this.selectorGroup.getValue();

        final String name = this.textName.getText();
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
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    private void init() {
        this.textSku.setBorder(new EmptyBorder(0, 5, 0, 0));
        this.textSku.setText(NOT_APPLICABLE);
        this.selectorServicePlan.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final AppServicePlan plan = (AppServicePlan) e.getItem();
                this.textSku.setText(plan.pricingTier().toString());
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.textSku.setText(NOT_APPLICABLE);
            }
        });
        this.selectorSubscription.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final Subscription subscription = (Subscription) e.getItem();
                this.selectorGroup.refreshWith(subscription);
                this.selectorServicePlan.refreshWith(subscription);
                this.selectorRegion.refreshWith(subscription);
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.selectorGroup.clear();
                this.selectorServicePlan.clear();
                this.selectorRegion.clear();
            }
        });
    }
}
