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

import com.intellij.ui.HideableDecorator;
import com.intellij.ui.treeStructure.Tree;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentOperation;
import com.microsoft.azuretools.core.mvp.ui.arm.DeploymentProperty;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.helpers.base.BaseEditor;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentPropertyViewPresenter;

import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.codehaus.jackson.schema.JsonSchema;
import org.jdesktop.swingx.JXLabel;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DeploymentPropertyView extends BaseEditor implements DeploymentPropertyMvpView {

    public static final String ID = "com.microsoft.intellij.helpers.arm.DeploymentPropertyView";
    public static final String DATETIME_FORMAT = "M/d/yyyy, h:mm:ss a";
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern(DATETIME_FORMAT);
    private final DeploymentPropertyViewPresenter<DeploymentPropertyView> deploymentPropertyViewPresenter;
    private JPanel contentPane;
    private JPanel pnlOverviewHolder;
    private JPanel pnlOverview;
    private JLabel deploymenNameLabel;
    private JLabel lastModifiedLabel;
    private JLabel statusLabel;
    private JLabel deploymentModeLabel;
    private Tree templateTree;
    private JXLabel statusReasonLabel;
    private JButton viewResourceTemplateButton;
    private JButton exportTemplateFileButton;
    private DeploymentNode deploymentNode;
    private static final String PNL_OVERVIEW = "Overview";

    public DeploymentPropertyView() {
        deploymentPropertyViewPresenter = new DeploymentPropertyViewPresenter<>();
        deploymentPropertyViewPresenter.onAttachView(this);

        HideableDecorator overviewDecorator = new HideableDecorator(pnlOverviewHolder, PNL_OVERVIEW, false);
        overviewDecorator.setContentComponent(pnlOverview);
        overviewDecorator.setOn(true);
        pnlOverview.setName(PNL_OVERVIEW);
        pnlOverview.setBorder(BorderFactory.createCompoundBorder());

        exportTemplateFileButton.addActionListener((e) -> {
            ExportTemplate exportTemplate = new ExportTemplate(deploymentNode);
            exportTemplate.doExport();
            exportTemplate.doExportParameters();
        });
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return contentPane;
    }

    @NotNull
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public void dispose() {
        deploymentPropertyViewPresenter.onDetachView();
    }

    public void onLoadProperty(DeploymentNode deploymentNode) {
        this.deploymentNode = deploymentNode;
        deploymentPropertyViewPresenter.onLoadProperty(deploymentNode);
    }

    @Override
    public void onLoadProperty(DeploymentProperty deploymentProperty) {
        final Deployment deployment = deploymentProperty.getDeployment();
        deploymenNameLabel.setText(deployment.name());
        lastModifiedLabel.setText(DATETIME_FORMATTER.print(deployment.timestamp().withZone(DateTimeZone.getDefault())));
        statusLabel.setText(deployment.provisioningState());
        deploymentModeLabel.setText(deployment.mode().name());
        StringBuffer sb = new StringBuffer();

        PagedList<DeploymentOperation> deploymentOperations = deployment.deploymentOperations().list();
        for (DeploymentOperation deploymentOperation : deploymentOperations) {
            if (deploymentOperation.statusMessage() != null && deploymentOperation.statusMessage() instanceof Map) {
                String errMsg = ((Map<String, String>)deploymentOperation.statusMessage()).get("Message");
                if (errMsg != null) {
                    sb.append(errMsg);
                } else {
                    sb.append(deploymentOperation.statusMessage().toString());
                }
            }
        }
        statusReasonLabel.setLineWrap(true);
        statusReasonLabel.setText(sb.toString());

        viewResourceTemplateButton.addActionListener((e) -> {
            EventUtil.logEvent(EventType.info, TelemetryConstants.ARM, TelemetryConstants.VIEW_TEMPALTE_FILE, null);
            DefaultLoader.getUIHelper().openResourceTemplateView(deploymentNode, deploymentProperty.getTemplateJson());
        });


        DefaultMutableTreeNode nodeRoot = new DefaultMutableTreeNode("Template");
        TreeModel model = new DefaultTreeModel(nodeRoot);
        templateTree.setModel(model);
        templateTree.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        DefaultMutableTreeNode nodeParameters = new DefaultMutableTreeNode("parameters");
        DefaultMutableTreeNode nodeVariables = new DefaultMutableTreeNode("variables");
        DefaultMutableTreeNode nodeResources = new DefaultMutableTreeNode("resources");
        nodeRoot.add(nodeParameters);
        nodeRoot.add(nodeVariables);
        nodeRoot.add(nodeResources);

        deploymentProperty.getParameters().stream().forEach((parameter) -> {
            nodeParameters.add(new DefaultMutableTreeNode(parameter));
        });

        deploymentProperty.getVariables().stream().forEach((variable) -> {
            nodeVariables.add(new DefaultMutableTreeNode(variable));
        });

        deploymentProperty.getResources().stream().forEach((resource) -> {
            nodeResources.add(new DefaultMutableTreeNode(resource));
        });
    }
}
