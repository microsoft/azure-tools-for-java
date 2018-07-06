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

package com.microsoft.azure.sparkserverless.serverexplore.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.DocumentAdapter;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.sparkserverless.common.IntegerWithErrorHintedField;
import com.microsoft.azure.sparkserverless.common.TextWithErrorHintedField;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterProvisionCtrlProvider;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterProvisionSettingsModel;
import com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode.SparkServerlessADLAccountNode;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.stream.Stream;

public class SparkServerlessProvisionDialog extends DialogWrapper
        implements SettableControl<SparkServerlessClusterProvisionSettingsModel> {
    @NotNull
    private SparkServerlessClusterProvisionCtrlProvider ctrlProvider;
    @NotNull
    private SparkServerlessADLAccountNode adlAccountNode;

    protected TextWithErrorHintedField clusterNameField;
    protected JTextField adlAccountField;
    protected TextWithErrorHintedField sparkEventsField;
    protected IntegerWithErrorHintedField masterCoresField;
    protected IntegerWithErrorHintedField masterMemoryField;
    protected IntegerWithErrorHintedField workerCoresField;
    protected IntegerWithErrorHintedField workerMemoryField;
    protected IntegerWithErrorHintedField workerNumberOfContainersField;
    protected JTextField availableAUField;
    protected JTextField totalAUField;
    protected JTextField calculatedAUField;
    protected JLabel masterMemoryLabel;
    protected JLabel masterCoresLabel;
    protected JLabel clusterNameLabel;
    protected JLabel adlAccountLabel;
    protected JLabel SparkEventsLabel;
    protected JLabel availableAULabel;
    protected JLabel calculatedAULabel;
    protected JLabel workerCoresLabel;
    protected JLabel workerMemoryLabel;
    protected JLabel workerNumberOfContainersLabel;
    protected JTextField errorMessageField;
    protected JPanel provisionDialogPanel;
    protected JButton refreshButton = new JButton(new ImageIcon(CommonConst.RefreshIConPath));
    protected JLabel storageRootPathLabel;
    protected JComboBox sparkVersionComboBox;
    protected JLabel sparkVersionLabel;

    @NotNull
    private final List<TextWithErrorHintedField> allTextFields = Arrays.asList(clusterNameField, sparkEventsField);
    @NotNull
    private final List<IntegerWithErrorHintedField> allAURelatedFields = Arrays.asList(masterCoresField, workerCoresField,
            masterMemoryField, workerMemoryField, workerNumberOfContainersField);

    protected void setClusterNameSets() {
        clusterNameField.setNotAllowedValues(
                new HashSet<>(ctrlProvider.getClusterNames().toBlocking().singleOrDefault(new ArrayList<>())));
    }

    public SparkServerlessProvisionDialog(@NotNull SparkServerlessADLAccountNode adlAccountNode,
                                          @NotNull AzureSparkServerlessAccount account) {
        // TODO: refactor the design of getProject Method for Node Class
        // TODO: get project through ProjectUtils.theProject()
        super((Project) adlAccountNode.getProject(), true);

        this.ctrlProvider = new SparkServerlessClusterProvisionCtrlProvider(
                this, new IdeaSchedulers((Project) adlAccountNode.getProject()), account);
        this.adlAccountNode = adlAccountNode;

        init();
        this.setTitle("Provision Spark Cluster");
        errorMessageField.setBackground(this.provisionDialogPanel.getBackground());
        errorMessageField.setBorder(BorderFactory.createEmptyBorder());
        availableAUField.setBorder(BorderFactory.createEmptyBorder());
        totalAUField.setBorder(BorderFactory.createEmptyBorder());
        calculatedAUField.setBorder(BorderFactory.createEmptyBorder());
        this.setModal(true);

        // setClusterNameSets to enable cluster name uniqueness check
        this.setClusterNameSets();
        // We can determine the ADL account since we provision on a specific ADL account Node
        this.adlAccountField.setText(adlAccountNode.getAdlAccount().getName());
        this.storageRootPathLabel.setText(Optional.ofNullable(account.getStorageRootPath()).orElse(""));

        refreshButton.addActionListener(e -> updateAvailableAU());
        allAURelatedFields.forEach(comp ->
                comp.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        if (isAllAURelatedFieldsLegal()) {
                            updateCalculatedAU();
                        }
                        super.focusLost(e);
                    }
                }));
        // These action listeners promise that Ok button can only be clicked until all the fields are legal
        Stream.concat(allTextFields.stream(), allAURelatedFields.stream()).forEach(comp ->
                comp.getDocument().addDocumentListener(new DocumentAdapter() {
                    @Override
                    protected void textChanged(DocumentEvent e) {
                        getOKAction().setEnabled(isAllFieldsLegal());
                    }
                }));
        getOKAction().setEnabled(false);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                updateAvailableAUAndTotalAU();
                updateCalculatedAU();
                super.windowOpened(e);
            }
        });
    }

    private void updateAvailableAUAndTotalAU() {
        ctrlProvider.getAvailableAUAndTotalAU().subscribe(pair -> {
            availableAUField.setText(String.valueOf(pair.getLeft()));
            totalAUField.setText(String.valueOf(pair.getRight()));
        });
    }

    private void updateAvailableAU() {
        ctrlProvider.getAvailableAU().subscribe(au -> availableAUField.setText(String.valueOf(au)));
    }

    private void updateCalculatedAU() {
        calculatedAUField.setText(String.valueOf(ctrlProvider.getCalculatedAU(
                Integer.valueOf(masterCoresField.getText()),
                Integer.valueOf(workerCoresField.getText()),
                Integer.valueOf(masterMemoryField.getText()),
                Integer.valueOf(workerMemoryField.getText()),
                Integer.valueOf(workerNumberOfContainersField.getText()))));
    }

    // Data -> Components
    @Override
    public void setData(@NotNull SparkServerlessClusterProvisionSettingsModel data) {
        errorMessageField.setText(data.getErrorMessage());
    }

    // Components -> Data
    @Override
    public void getData(@NotNull SparkServerlessClusterProvisionSettingsModel data) {
        data.setClusterName(clusterNameField.getText())
                .setAdlAccount(adlAccountField.getText())
                .setSparkEvents(sparkEventsField.getText())
                .setAvailableAU(NumberUtils.toInt(availableAUField.getText()))
                .setTotalAU(NumberUtils.toInt(totalAUField.getText()))
                .setCalculatedAU(NumberUtils.toInt(calculatedAUField.getText()))
                .setMasterCores(masterCoresField.getValue())
                .setMasterMemory(masterMemoryField.getValue())
                .setWorkerCores(workerCoresField.getValue())
                .setWorkerMemory(workerMemoryField.getValue())
                .setWorkerNumberOfContainers(workerNumberOfContainersField.getValue())
                .setStorageRootPathLabelTitle(storageRootPathLabel.getText())
                .setErrorMessage(errorMessageField.getText());
    }

    @Override
    protected void doOKAction() {
        if (!getOKAction().isEnabled()) {
            return;
        }

        getOKAction().setEnabled(false);
        ctrlProvider
                .validateAndProvision()
                .doOnEach(notification -> getOKAction().setEnabled(true))
                .subscribe(toUpdate -> {
                    // TODO: replace load with refreshWithoutAsync
                    adlAccountNode.load(false);
                    super.doOKAction();
                });
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

    private boolean isAllFieldsLegal() {
        return Stream.concat(allTextFields.stream(), allAURelatedFields.stream()).allMatch(comp -> comp.isLegal());
    }

    private boolean isAllAURelatedFieldsLegal() {
        return allAURelatedFields.stream().allMatch(comp -> comp.isLegal());
    }
}
