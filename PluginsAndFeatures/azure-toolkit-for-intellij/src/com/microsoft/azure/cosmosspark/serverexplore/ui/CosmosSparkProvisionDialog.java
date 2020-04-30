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

package com.microsoft.azure.cosmosspark.serverexplore.ui;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HideableDecorator;
import com.microsoft.azure.cosmosspark.common.IntegerWithErrorHintedField;
import com.microsoft.azure.cosmosspark.common.JXHyperLinkWithUri;
import com.microsoft.azure.cosmosspark.common.TextWithErrorHintedField;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterProvisionCtrlProvider;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterProvisionSettingsModel;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkADLAccountNode;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import com.microsoft.intellij.ui.components.JsonEnvPropertiesField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

public class CosmosSparkProvisionDialog extends DialogWrapper
        implements SettableControl<CosmosSparkClusterProvisionSettingsModel>, ILogger {
    @NotNull
    private final CosmosSparkClusterProvisionCtrlProvider ctrlProvider;
    @NotNull
    private final CosmosSparkADLAccountNode adlAccountNode;

    protected TextWithErrorHintedField clusterNameField;
    protected JTextField adlAccountField;
    protected TextWithErrorHintedField sparkEventsField;
    protected IntegerWithErrorHintedField masterCoresField;
    protected IntegerWithErrorHintedField masterMemoryField;
    protected IntegerWithErrorHintedField workerCoresField;
    protected IntegerWithErrorHintedField workerMemoryField;
    protected IntegerWithErrorHintedField workerNumberOfContainersField;
    protected JLabel availableAUNumberLabel;
    protected JLabel totalAUNumberLabel;
    protected JLabel calculatedAUNumberLabel;
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
    protected JPanel provisionDialogPanel;
    protected JButton refreshButton;
    protected JLabel storageRootPathLabel;
    protected JComboBox<String> sparkVersionComboBox;
    protected JLabel sparkVersionLabel;
    private JXHyperLinkWithUri jobQueueHyperLink;
    protected JPanel errorMessagePanel;
    protected JPanel errorMessagePanelHolder;
    protected JPanel configPanel;
    protected JPanel availalableAUPanel;
    private JPanel auRequiredPanel;
    protected JLabel auWarningLabel;
    protected JsonEnvPropertiesField extendedPropertiesField;
    protected JLabel extendedPropertiesLabel;
    protected ConsoleViewImpl consoleViewPanel;
    protected HideableDecorator errorMessageDecorator;
    @NotNull
    private final List<TextWithErrorHintedField> allTextFields = Arrays.asList(clusterNameField, sparkEventsField);
    @NotNull
    private final List<IntegerWithErrorHintedField> allAURelatedFields = Arrays.asList(masterCoresField,
                                                                                       workerCoresField,
                                                                                       masterMemoryField,
                                                                                       workerMemoryField,
                                                                                       workerNumberOfContainersField);

    public CosmosSparkProvisionDialog(@NotNull final CosmosSparkADLAccountNode adlAccountNode,
                                      @NotNull final AzureSparkServerlessAccount account) {
        // TODO: refactor the design of getProject Method for Node Class
        // TODO: get project through ProjectUtils.theProject()
        super((Project) adlAccountNode.getProject(), true);
        this.ctrlProvider = new CosmosSparkClusterProvisionCtrlProvider(
                this, new IdeaSchedulers((Project) adlAccountNode.getProject()), account);
        this.adlAccountNode = adlAccountNode;

        init();
        this.setTitle("Provision Spark Cluster");
        this.setModal(true);

        // make error message widget hideable
        errorMessagePanel.setBorder(BorderFactory.createEmptyBorder());
        errorMessageDecorator = new HideableDecorator(errorMessagePanelHolder, "Log", true);
        errorMessageDecorator.setContentComponent(errorMessagePanel);
        errorMessageDecorator.setOn(false);

        // add console view panel to error message panel
        consoleViewPanel = new ConsoleViewImpl((Project) adlAccountNode.getProject(), false);
        errorMessagePanel.add(consoleViewPanel.getComponent(), BorderLayout.CENTER);
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                "provisionLog",
                new DefaultActionGroup(consoleViewPanel.createConsoleActions()),
                false);
        errorMessagePanel.add(toolbar.getComponent(), BorderLayout.WEST);

        this.jobQueueHyperLink.setURI(account.getJobManagementURI());

        this.enableClusterNameUniquenessCheck();
        // We can determine the ADL account since we provision on a specific ADL account Node
        this.adlAccountField.setText(adlAccountNode.getAdlAccount().getName());
        this.storageRootPathLabel.setText(Optional.ofNullable(account.getStorageRootPath()).orElse(""));

        refreshButton.addActionListener(event -> ctrlProvider.updateAvailableAU().subscribe(
                data -> {
                },
                err -> log().warn("Update available AU in provision cluster dialog get exceptions. Error: "
                                          + ExceptionUtils.getStackTrace(err))));

        allAURelatedFields.forEach(comp -> comp.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                if (isAllAURelatedFieldsLegal()) {
                    ctrlProvider.updateCalculatedAU().subscribe(
                            data -> {
                            },
                            err -> log().warn(
                                    "Update AU Required in provision cluster dialog get exceptions. Error: "
                                            + ExceptionUtils.getStackTrace(err)));
                }
                super.focusLost(e);
            }
        }));
        // These action listeners promise that Ok button can only be clicked until all the fields are legal
        Stream.concat(allTextFields.stream(), allAURelatedFields.stream()).forEach(
                comp -> comp.getDocument().addDocumentListener(new DocumentAdapter() {
                    @Override
                    protected void textChanged(@NotNull final DocumentEvent e) {
                        getOKAction().setEnabled(isAllFieldsLegal());
                    }
                }));
        getOKAction().setEnabled(false);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(final WindowEvent e) {
                ctrlProvider.updateAvailableAUAndTotalAU().subscribe(
                        data -> {
                        },
                        err -> log().warn("Update available AU, total AU and calculated AU in provision cluster dialog"
                                                  + " get exceptions. Error: " + ExceptionUtils.getStackTrace(err)));
                super.windowOpened(e);
            }
        });
    }

    @Override
    protected void dispose() {
        Disposer.dispose(consoleViewPanel);

        super.dispose();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        // Focus on cluster name field so that `Esc` can be applied to dismiss the dialog
        return clusterNameField;
    }

    protected void enableClusterNameUniquenessCheck() {
        try {
            clusterNameField.setNotAllowedValues(
                    new HashSet<>(ctrlProvider.getClusterNames().toBlocking().singleOrDefault(new ArrayList<>())));

            sparkEventsField.setPatternAndErrorMessage(null);
            // The text setting is necessary. By default, '/' is not allowed for TextWithErrorHintedField, leading to
            // error tooltip. We have to set the text to trigger the validator of the new pattern.
            sparkEventsField.setText("spark-events/");
        } catch (final Exception ex) {
            log().warn("Got exceptions when getting cluster names: " + ex);
        }
    }

    protected void printLogLine(@NotNull final ConsoleViewContentType logLevel, @NotNull final String log) {
        consoleViewPanel.print(DateTime.now().toString() + " " + logLevel.toString().toUpperCase() + " " + log + "\n",
                               logLevel);
    }

    // Data -> Components
    @Override
    public void setData(@NotNull final CosmosSparkClusterProvisionSettingsModel data) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            clusterNameField.setText(data.getClusterName());
            adlAccountField.setText(data.getAdlAccount());
            // set sparkEventsField to "-" rather than empty string to avoid "string expected" tooltip
            sparkEventsField.setText(StringUtils.isEmpty(data.getSparkEvents()) ? "-" : data.getSparkEvents());
            availableAUNumberLabel.setText(String.valueOf(data.getAvailableAU()));
            totalAUNumberLabel.setText(String.valueOf(data.getTotalAU()));
            calculatedAUNumberLabel.setText(String.valueOf(data.getCalculatedAU()));
            auWarningLabel.setVisible(data.getCalculatedAU() > data.getAvailableAU());
            refreshButton.setEnabled(data.getRefreshEnabled());
            masterCoresField.setText(String.valueOf(data.getMasterCores()));
            masterMemoryField.setText(String.valueOf(data.getMasterMemory()));
            workerCoresField.setText(String.valueOf(data.getWorkerCores()));
            workerMemoryField.setText(String.valueOf(data.getWorkerMemory()));
            workerNumberOfContainersField.setText(String.valueOf(data.getWorkerNumberOfContainers()));

            if (!StringUtils.isEmpty(data.getErrorMessage())) {
                if (!errorMessageDecorator.isExpanded()) {
                    errorMessageDecorator.setOn(true);
                }
                printLogLine(ConsoleViewContentType.ERROR_OUTPUT, data.getErrorMessage());

                printLogLine(ConsoleViewContentType.NORMAL_OUTPUT, "x-ms-request-id: " + data.getRequestId());
                printLogLine(ConsoleViewContentType.NORMAL_OUTPUT, "cluster guid: " + data.getClusterGuid());
            }
        }, ModalityState.any());
    }

    // Components -> Data
    @Override
    public void getData(@NotNull final CosmosSparkClusterProvisionSettingsModel data) {
        data.setClusterName(clusterNameField.getText())
            .setAdlAccount(adlAccountField.getText())
            .setSparkEvents(sparkEventsField.getText())
            .setExtendedProperties(extendedPropertiesField.getEnvs())
            .setAvailableAU(NumberUtils.toInt(availableAUNumberLabel.getText()))
            .setTotalAU(NumberUtils.toInt(totalAUNumberLabel.getText()))
            .setCalculatedAU(NumberUtils.toInt(calculatedAUNumberLabel.getText()))
            .setRefreshEnabled(refreshButton.isEnabled())
            .setMasterCores(masterCoresField.getValue())
            .setMasterMemory(masterMemoryField.getValue())
            .setWorkerCores(workerCoresField.getValue())
            .setWorkerMemory(workerMemoryField.getValue())
            .setWorkerNumberOfContainers(workerNumberOfContainersField.getValue())
            .setStorageRootPathLabelTitle(storageRootPathLabel.getText());
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
                    adlAccountNode.load(false);
                    super.doOKAction();
                }, err -> log().warn("Error provision a cluster. " + err.toString()));
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
