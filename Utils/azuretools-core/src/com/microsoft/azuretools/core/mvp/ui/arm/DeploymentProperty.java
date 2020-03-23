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
