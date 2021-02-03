/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
