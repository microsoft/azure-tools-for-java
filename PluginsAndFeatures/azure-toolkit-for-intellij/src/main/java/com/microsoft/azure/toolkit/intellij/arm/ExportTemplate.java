/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm;

import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;

import java.io.File;

public class ExportTemplate {

    private final DeploymentNode deploymentNode;

    private static final String FILE_SELECTOR_TITLE = "Choose Where to Save the ARM Template File.";
    private static final String PARAMETERS_SELECTOR_TITLE = "Choose Where to Save the ARM Parameter File.";

    private static final String TEMPLATE_FILE_NAME = "%s.json";
    private static final String PARAMETERS_FILE_NAME = "%s.parameters.json";

    public ExportTemplate(DeploymentNode deploymentNode) {
        this.deploymentNode = deploymentNode;
    }

    public void doExportTemplate() {
        final String template = deploymentNode.getDeployment().exportTemplate().templateAsJson();
        doExportTemplate(template);
    }

    public void doExportParameters() {
        final String parameters = DeploymentUtils.serializeParameters(deploymentNode.getDeployment());
        doExportParameters(parameters);
    }

    public void doExportTemplate(String template) {
        File file = DefaultLoader.getUIHelper().showFileSaver(FILE_SELECTOR_TITLE,
                String.format(TEMPLATE_FILE_NAME, deploymentNode.getName()));
        if (file != null) {
            deploymentNode.getDeploymentNodePresenter().onGetExportTemplateRes(Utils.getPrettyJson(template), file);
        }
    }

    public void doExportParameters(String parameters) {
        File file = DefaultLoader.getUIHelper().showFileSaver(PARAMETERS_SELECTOR_TITLE,
                String.format(PARAMETERS_FILE_NAME, deploymentNode.getName()));
        if (file != null) {
            deploymentNode.getDeploymentNodePresenter().onGetExportTemplateRes(Utils.getPrettyJson(parameters), file);
        }
    }
}
