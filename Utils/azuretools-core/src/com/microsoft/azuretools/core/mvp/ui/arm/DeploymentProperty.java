/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.arm;

import com.microsoft.azure.management.resources.Deployment;
import java.util.List;

public class DeploymentProperty {

    private Deployment deployment;
    private String templateJson;
    private List<String> parameters;
    private List<String> variables;
    private List<String> resources;

    public DeploymentProperty(Deployment deployment, List<String> parameters,
        List<String> variables, List<String> resources, String templateJson) {
        this.deployment = deployment;
        this.parameters = parameters;
        this.variables = variables;
        this.resources = resources;
        this.templateJson = templateJson;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<String> getVariables() {
        return variables;
    }

    public String getTemplateJson() {
        return templateJson;
    }

    public List<String> getResources() {
        return resources;
    }


}
