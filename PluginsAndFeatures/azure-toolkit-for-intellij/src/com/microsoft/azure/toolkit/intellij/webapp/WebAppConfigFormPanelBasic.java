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

package com.microsoft.azure.toolkit.intellij.webapp;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.intellij.appservice.AppNameInput;
import com.microsoft.azure.toolkit.intellij.appservice.platform.PlatformComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.appservice.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.appservice.DraftServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.intellij.ui.components.AzureArtifact;
import com.microsoft.intellij.ui.components.AzureArtifactManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class WebAppConfigFormPanelBasic extends JPanel implements AzureFormPanel<WebAppConfig> {
    private final Project project;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");
    private static final int RG_NAME_MAX_LENGTH = 90;
    private static final int SP_NAME_MAX_LENGTH = 40;

    private JPanel contentPanel;

    private AppNameInput textName;
    private PlatformComboBox selectorPlatform;
    private AzureArtifactComboBox selectorApplication;
    private TitledSeparator deploymentTitle;
    private JLabel deploymentLabel;
    private Subscription subscription;
    private Region defaultRegion;

    public WebAppConfigFormPanelBasic(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        final String date = DATE_FORMAT.format(new Date());
        final String defaultWebAppName = String.format("app-%s-%s", this.project.getName(), date);
        this.textName.setValue(defaultWebAppName);
        final List<Subscription> items = AzureMvpModel.getInstance().getSelectedSubscriptions();
        this.subscription = items.get(0); // select the first subscription as the default
        DefaultLoader.getIdeHelper().invokeLater(this::getRegion);
        this.textName.setSubscription(this.subscription);
        this.textName.setRequired(true);
        this.selectorPlatform.setRequired(true);
    }

    @SneakyThrows
    private Region getRegion() {
        if (Objects.nonNull(this.defaultRegion)) {
            return this.defaultRegion;
        }
        final String subId = this.subscription.subscriptionId();
        final PricingTier tier = WebAppConfig.DEFAULT_PRICING_TIER;
        final List<Region> regions = AzureWebAppMvpModel.getInstance().getAvailableRegions(subId, tier);
        this.defaultRegion = regions.get(0);
        return this.defaultRegion;
    }

    @SneakyThrows
    @Override
    public WebAppConfig getData() {
        final String date = DATE_FORMAT.format(new Date());
        final String name = this.textName.getValue();
        final Platform platform = this.selectorPlatform.getValue();
        final AzureArtifact artifact = this.selectorApplication.getValue();

        final PricingTier tier = WebAppConfig.DEFAULT_PRICING_TIER;
        final Region region = this.getRegion();

        final WebAppConfig config = WebAppConfig.builder().build();
        config.setSubscription(this.subscription);
        final DraftResourceGroup group = DraftResourceGroup.builder().build();
        group.setName(StringUtils.substring(String.format("rg-%s", name), 0, RG_NAME_MAX_LENGTH));
        group.setSubscription(this.subscription);
        config.setResourceGroup(group);
        config.setName(name);
        config.setPlatform(platform);
        config.setRegion(region);

        final DraftServicePlan plan = DraftServicePlan.builder().build();
        plan.setSubscription(this.subscription);
        plan.setName(StringUtils.substring(String.format("sp-%s", name), 0, SP_NAME_MAX_LENGTH));
        plan.setTier(tier);
        plan.setOs(platform.getOs());
        plan.setRegion(region);
        config.setServicePlan(plan);

        if (Objects.nonNull(artifact)) {
            final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.project);
            final String path = manager.getFileForDeployment(this.selectorApplication.getValue());
            config.setApplication(Paths.get(path));
        }
        return config;
    }

    @Override
    public void setData(final WebAppConfig config) {
        this.textName.setValue(config.getName());
        this.selectorPlatform.setValue(config.getPlatform());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.textName,
            this.selectorPlatform,
            this.selectorApplication
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.selectorApplication = new AzureArtifactComboBox(project);
    }

    public void setDeploymentVisible(boolean visible) {
        this.deploymentTitle.setVisible(visible);
        this.deploymentLabel.setVisible(visible);
        this.selectorApplication.setVisible(visible);
    }
}
