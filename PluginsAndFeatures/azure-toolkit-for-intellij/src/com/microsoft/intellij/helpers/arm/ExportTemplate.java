/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers.arm;

import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import rx.schedulers.Schedulers;

import java.io.File;

public class ExportTemplate {

    private final DeploymentNode deploymentNode;
    private static final String FILE_SELECTOR_TITLE = "Choose Where You Want to Save the Azure Resource Manager "
            + "Template.";
    private static final String PARAMETERS_SELECTOR_TITLE = "Choose Where You Want to Save the Azure Resource Manager "
            + "Parameters.";
    private static final String EXPORT_TEMPLATE_FAIL = "MS Services - Error Export resource manager template";

    public ExportTemplate(DeploymentNode deploymentNode) {
        this.deploymentNode = deploymentNode;
    }

    public void doExport() {
        File file = DefaultLoader.getUIHelper().showFileSaver(FILE_SELECTOR_TITLE, deploymentNode.getName() + ".json");
        if (file != null) {
            deploymentNode.getDeployment().exportTemplateAsync().subscribeOn(Schedulers.io()).subscribe(
                    res -> DefaultLoader.getIdeHelper()
                            .invokeLater(() -> deploymentNode.getDeploymentNodePresenter()
                                    .onGetExportTemplateRes(Utils.getPrettyJson(res.templateAsJson()), file)),
                    ex -> DefaultLoader.getUIHelper().showError(ex.getMessage(), EXPORT_TEMPLATE_FAIL));
        }
    }

    public void doExportParameters(){
        File file = DefaultLoader.getUIHelper().showFileSaver(PARAMETERS_SELECTOR_TITLE, deploymentNode.getName() + ".json");
        if (file != null) {
            final String parameters = DeploymentUtils.serializeParameters(deploymentNode.getDeployment());
            deploymentNode.getDeployment().exportTemplateAsync().subscribeOn(Schedulers.io()).subscribe(
                    res -> DefaultLoader.getIdeHelper()
                            .invokeLater(() -> deploymentNode.getDeploymentNodePresenter()
                                    .onGetExportTemplateRes(parameters, file)),
                    ex -> DefaultLoader.getUIHelper().showError(ex.getMessage(), EXPORT_TEMPLATE_FAIL));
        }
    }

    public void doExport(String template) {
        File file = DefaultLoader.getUIHelper().showFileSaver(FILE_SELECTOR_TITLE, deploymentNode.getName() + ".json");
        if (file != null) {
            deploymentNode.getDeploymentNodePresenter().onGetExportTemplateRes(Utils.getPrettyJson(template), file);
        }
    }

    public void doExportParameters(String parameters){
        File file = DefaultLoader.getUIHelper().showFileSaver(PARAMETERS_SELECTOR_TITLE, deploymentNode.getName() + ".json");
        if (file != null) {
            deploymentNode.getDeploymentNodePresenter().onGetExportTemplateRes(Utils.getPrettyJson(parameters), file);
        }
    }

}
