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
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;

import java.io.File;

public class ExportTemplate {

    private final DeploymentNode deploymentNode;
    private static final String FOLDER_SELECTOR_TITLE = "Choose Where You Want to Save the ARM Templates";
    private static final String FILE_SELECTOR_TITLE = "Choose Where You Want to Save the Template File";
    private static final String PARAMETERS_SELECTOR_TITLE = "Choose Where You Want to Save the Parameter File";

    private static final String TEMPLATE_FILE_NAME = "%s.json";

    private static final String PARAMETERS_FILE_NAME = "%s_parameters.json";

    public ExportTemplate(DeploymentNode deploymentNode) {
        this.deploymentNode = deploymentNode;
    }

    public void doExport() {
        String template = deploymentNode.getDeployment().exportTemplate().templateAsJson();
        String parameters = DeploymentUtils.serializeParameters(deploymentNode.getDeployment());
        doExport(template, parameters);
    }

    public void doExport(String template, String parameters) {
        UIUtils.showSingleFolderChooser(FOLDER_SELECTOR_TITLE, (File folder) -> {
            String templateFile = String.format(TEMPLATE_FILE_NAME, deploymentNode.getName());
            String parameterFile = String.format(PARAMETERS_FILE_NAME, deploymentNode.getName());
            deploymentNode.getDeploymentNodePresenter().onGetExportTemplateRes(Utils.getPrettyJson(template),
                    new File(folder, templateFile), Utils.getPrettyJson(parameters), new File(folder, parameterFile));
        });
    }

    public void doExportTemplate(String template) {
        File file = DefaultLoader.getUIHelper().showFileSaver(FILE_SELECTOR_TITLE,
                String.format(TEMPLATE_FILE_NAME, deploymentNode.getName()));
        if (file != null) {
            deploymentNode.getDeploymentNodePresenter().onGetExportTemplateFile(Utils.getPrettyJson(template), file);
        }
    }

    public void doExportParameters(String parameters) {
        File file = DefaultLoader.getUIHelper().showFileSaver(PARAMETERS_SELECTOR_TITLE,
                String.format(PARAMETERS_FILE_NAME, deploymentNode.getName()));
        if (file != null) {
            deploymentNode.getDeploymentNodePresenter().onGetExportParameterFile(Utils.getPrettyJson(parameters), file);
        }
    }
}
