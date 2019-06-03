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

package com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments;

import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentExportResult;
import com.microsoft.azuretools.core.mvp.ui.arm.DeploymentProperty;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import rx.Observable;
import rx.exceptions.Exceptions;

public class DeploymentPropertyViewPresenter<V extends DeploymentPropertyMvpView> extends MvpPresenter<V> {

    public void onLoadProperty(DeploymentNode node) {

        Observable.fromCallable(() -> {
            List<String> parameters = new ArrayList<>();
            List<String> variables = new ArrayList<>();
            List<String> resources = new ArrayList<>();

            Deployment deployment = node.getDeployment();
            DeploymentExportResult template = deployment.exportTemplate();
            Map<String, Object> templateObj = (Map<String, Object>) template.template();

            if (templateObj.get("parameters") != null) {
                Map<String, Map<String, String>> parametersTmp = (Map<String, Map<String, String>>) templateObj.get("parameters");
                for (String parameter : parametersTmp.keySet()) {
                    parameters.add(String.format("%s(%s)", parameter, parametersTmp.get(parameter).get("type")));
                }
            }

            if (templateObj.get("variables") != null) {
                Map<String, Map<String, String>> variablesTmp = (Map<String, Map<String, String>>) templateObj.get("variables");
                for (String variable : variablesTmp.keySet()) {
                    variables.add(variable);
                }
            }

            if (templateObj.get("resources") != null) {
                List<Map<String, String>> resourcesTmp = (List<Map<String, String>>) templateObj.get("resources");
                for (Map<String, String> resource : resourcesTmp) {
                    resources.add(String.format("%s(%s)", resource.get("name"), resource.get("type")));
                }
            }
            return new DeploymentProperty(deployment, parameters, variables, resources, template.templateAsJson());
        }).subscribeOn(this.getSchedulerProvider().io()).subscribe((property) -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (!this.isViewDetached()) {
                    getMvpView().onLoadProperty(property);
                }
            });
        }, (e) -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (!this.isViewDetached()) {
                    DefaultLoader.getUIHelper().logError("An exception occurred when load deployment properties", e);
                    DefaultLoader.getUIHelper().showError(node,
                            "An exception occurred when load deployment properties, " + e.getMessage());
                }
            });
        });
    }
}
