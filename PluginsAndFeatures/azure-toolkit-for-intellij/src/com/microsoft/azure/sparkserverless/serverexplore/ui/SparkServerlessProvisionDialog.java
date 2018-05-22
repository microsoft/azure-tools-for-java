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
package com.microsoft.azure.sparkserverless.serverexplore.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterProvisionCtrlProvider;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterProvisionSettingsModel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import com.microsoft.tooling.msservices.serviceexplorer.azure.sparkserverless.SparkServerlessADLAccountNode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class SparkServerlessProvisionDialog extends DialogWrapper
        implements SettableControl<SparkServerlessClusterProvisionSettingsModel> {
    @NotNull
    private SparkServerlessClusterProvisionCtrlProvider ctrlProvider;

    @NotNull
    private SparkServerlessADLAccountNode adlAccountNode;

    private JTextField clusterNameField;
    private JTextField adlAccountField;
    private JTextField previousSparkEventsField;
    private JTextField masterCoresField;
    private JTextField masterMemoryField;
    private JTextField workerCoresField;
    private JTextField workerMemoryField;
    private JTextField workerNumberOfContainersField;
    private JTextField availableAUField;
    private JTextField totalAUField;
    private JTextField calculatedAUField;
    private JLabel masterMemoryLabel;
    private JLabel masterCoresLabel;
    private JLabel clusterNameLabel;
    private JLabel adlAccountLabel;
    private JLabel previousSparkEventsLabel;
    private JLabel availableAULabel;
    private JLabel calculatedAULabel;
    private JLabel workerCoresLabel;
    private JLabel workerMemoryLabel;
    private JLabel workerNumberOfContainersLabel;
    private JTextField errorMessageField;
    private JPanel provisionDialogPanel;
    private JButton refreshButton;

    public SparkServerlessProvisionDialog(@NotNull SparkServerlessADLAccountNode adlAccountNode) {
        // TODO: canBeParent
        super((Project)adlAccountNode.getProject(), true);

        this.ctrlProvider = new SparkServerlessClusterProvisionCtrlProvider(
                this, new IdeaSchedulers((Project)adlAccountNode.getProject()));
        this.adlAccountNode = adlAccountNode;

        init();
        this.setTitle("Provision Spark Cluster");
        errorMessageField.setBackground(this.provisionDialogPanel.getBackground());
        errorMessageField.setBorder(BorderFactory.createEmptyBorder());
        this.setModal(true);

        adlAccountField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // TODO: check validity of ADL account
                availableAUField.setText(String.valueOf(ctrlProvider.getAvailableAU(adlAccountField.getText())));
                totalAUField.setText(String.valueOf(ctrlProvider.getTotalAU(adlAccountField.getText())));
            }
        });
        refreshButton.addActionListener(e -> {
                availableAUField.setText(String.valueOf(ctrlProvider.getAvailableAU(adlAccountField.getText())));
        });
        masterCoresField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateCalculatedAUField();
            }
        });
        masterMemoryField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateCalculatedAUField();
            }
        });
        workerCoresField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateCalculatedAUField();
            }
        });
        workerMemoryField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateCalculatedAUField();
            }
        });
        workerNumberOfContainersField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateCalculatedAUField();
            }
        });
    }

    private void updateCalculatedAUField() {
        int calculatedAU = ctrlProvider.getCalculatedAU(masterCoresField.getText(), workerCoresField.getText());
        calculatedAUField.setText(String.valueOf(calculatedAU));
    }


    @Override
    public void setData(@NotNull SparkServerlessClusterProvisionSettingsModel data) {
        clusterNameField.setText(data.getClusterName());
        adlAccountField.setText(data.getAdlAccount());
        previousSparkEventsField.setText(data.getPreviousSparkEvents());
        availableAUField.setText(String.valueOf(data.getAvailableAU()));
        totalAUField.setText(String.valueOf(data.getTotalAU()));
        calculatedAUField.setText(String.valueOf(data.getCalculatedAU()));
        masterCoresField.setText(String.valueOf(data.getMasterCores()));
        masterMemoryField.setText(String.valueOf(data.getMasterMemory()));
        workerCoresField.setText(String.valueOf(data.getWorkerCores()));
        workerMemoryField.setText(String.valueOf(data.getWorkerMemory()));
        workerNumberOfContainersField.setText(String.valueOf(data.getWorkerNumberOfContainers()));

        clusterNameLabel.setText(data.getClusterNameLabelTitle());
        adlAccountLabel.setText(data.getAdlAccountLabelTitle());
        previousSparkEventsLabel.setText(data.getPreviousSparkEventsLabelTitle());
        workerNumberOfContainersLabel.setText(data.getWorkerNumberOfContainersLabelTitle());

        errorMessageField.setText(data.getErrorMessage());
        // TODO: finish all other fields
    }

    @Override
    public void getData(@NotNull SparkServerlessClusterProvisionSettingsModel data) {
        // TODO: remove applying jTextFieldToInt to master/worker fields in the final version
        data.setClusterName(clusterNameField.getText())
                .setAdlAccount(adlAccountField.getText())
                .setPreviousSparkEvents(previousSparkEventsField.getText())
                .setAvailableAU(ctrlProvider.editorStringToInt(availableAUField.getText()))
                .setTotalAU(ctrlProvider.editorStringToInt(totalAUField.getText()))
                .setCalculatedAU(ctrlProvider.editorStringToInt(calculatedAUField.getText()))
                .setMasterCores(ctrlProvider.editorStringToInt(masterCoresField.getText()))
                .setMasterMemory(ctrlProvider.editorStringToInt(masterMemoryField.getText()))
                .setWorkerCores(ctrlProvider.editorStringToInt(workerCoresField.getText()))
                .setWorkerMemory(ctrlProvider.editorStringToInt(workerMemoryField.getText()))
                .setWorkerNumberOfContainers(ctrlProvider.editorStringToInt(workerNumberOfContainersField.getText()))
                .setClusterNameLabelTitle(clusterNameLabel.getText())
                .setAdlAccountLabelTitle(adlAccountLabel.getText())
                .setPreviousSparkEventsLabelTitle(previousSparkEventsLabel.getText())
                .setWorkerNumberOfContainersLabelTitle(workerNumberOfContainersLabel.getText())
                .setErrorMessage(errorMessageField.getText());

        // TODO: finish all other fields
    }

    @Override
    protected void doOKAction() {
        ctrlProvider
                .validateAndProvision()
                .subscribe(toUpdate -> {
                    // TODO: replace load with refreshWithoutAsync
                    adlAccountNode.load(false);
                    super.doOKAction();
                });
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[] {getOKAction(), getCancelAction()};
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[0];
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return provisionDialogPanel;
    }
}
