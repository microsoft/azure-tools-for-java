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
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.appservice.platform.PlatformComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.appservice.artifact.ArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.appservice.AppNameInput;
import com.microsoft.azure.toolkit.lib.appservice.ResourceGroupMock;
import com.microsoft.azure.toolkit.lib.appservice.ServicePlanMock;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;

import javax.swing.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class WebAppConfigFormPanelBasic extends JPanel implements AzureFormPanel<WebAppConfig> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");

    private JPanel contentPanel;

    private AppNameInput textName;
    private PlatformComboBox selectorPlatform;
    private ArtifactComboBox selectorApplication;
    private TitledSeparator deploymentTitle;
    private JLabel deploymentLabel;


    public WebAppConfigFormPanelBasic() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        final String date = DATE_FORMAT.format(new Date());
        final Project project = ProjectManager.getInstance().getOpenProjects()[0];
        final String defaultWebAppName = String.format("app-%s-%s", project.getName(), date);
        this.textName.setValue(defaultWebAppName);
        final List<Subscription> items = AzureMvpModel.getInstance().getSelectedSubscriptions();
        this.textName.setSubscription(items.get(0)); // select the first subscription as the default
    }

    @Override
    public WebAppConfig getData() {
        final String date = DATE_FORMAT.format(new Date());
        final String name = this.textName.getValue();
        final Platform platform = this.selectorPlatform.getValue();
        final Path path = this.selectorApplication.getValue();

        final WebAppConfig config = WebAppConfig.builder().build();
        final ResourceGroupMock group = ResourceGroupMock.builder().build();
        group.setName(String.format("rg-%s-%s", name, date));
        config.setResourceGroup(group);
        config.setName(name);
        config.setPlatform(platform);
        config.setRegion(WebAppConfig.DEFAULT_REGION);

        final ServicePlanMock plan = ServicePlanMock.builder().build();
        plan.setName(String.format("sp-%s-%s", name, date));
        plan.setTier(WebAppConfig.DEFAULT_PRICING_TIER);
        plan.setOs(platform.getOs());
        plan.setRegion(WebAppConfig.DEFAULT_REGION);
        config.setServicePlan(plan);

        config.setApplication(path);
        return config;
    }

    @Override
    public void setData(final WebAppConfig config) {
        this.textName.setValue(config.getName());
        this.selectorPlatform.setValue(config.getPlatform());
        this.selectorApplication.setValue(config.getApplication());
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

    @Override
    public void setDeploymentVisible(boolean visible){
        this.deploymentTitle.setVisible(visible);
        this.deploymentLabel.setVisible(visible);
        this.selectorApplication.setVisible(visible);
    }
}
