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
package com.microsoft.azure.hdinsight.spark.ui;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.hdinsight.common.DarkThemeManager;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionJobConfigCheckResult;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionJobConfigCheckStatus;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitHelper;
import com.microsoft.azure.hdinsight.spark.common.SubmissionTableModel;
import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveRenderer;
import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveTableModel;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.utils.Pair;
import org.apache.commons.lang3.StringUtils;
import rx.subjects.BehaviorSubject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SparkSubmissionContentPanel extends JPanel{
    public SparkSubmissionContentPanel(){
        initializeComponents();

        addContainerListener(new ContainerAdapter() {
            @Override
            public void componentRemoved(ContainerEvent e) {
                cleanUp();

                super.componentRemoved(e);
            }
        });
    }

    @NotNull
    private java.util.List<String> getErrorMessages() {
        final Color currentErrorColor = DarkThemeManager.getInstance().getErrorMessageColor();

        return Arrays.stream(errorMessageLabels)
                .filter(errorLabel -> errorLabel.isVisible() && errorLabel.getForeground().equals(currentErrorColor))
                .map(JLabel::getText)
                .collect(Collectors.toList());
    }

    public int displayLayoutCurrentRow = 0;

    protected final int margin = 10;
    private static final String REFRESH_BUTTON_PATH = "/icons/refresh.png";

    @NotNull
    private ComboboxWithBrowseButton clustersListComboBox;
    @NotNull
    private ComboBox<Artifact> selectedArtifactComboBox;
    @NotNull
    private TextFieldWithBrowseButton localArtifactTextField;
    @NotNull
    private TextFieldWithBrowseButton mainClassTextField;
    @NotNull
    private JBTable jobConfigurationTable;
    @NotNull
    private JTextField commandLineTextField;
    @NotNull
    private JTextField referencedJarsTextField;
    @NotNull
    private JTextField referencedFilesTextField;
    @NotNull
    private JRadioButton intelliJArtifactRadioButton;
    @NotNull
    private JRadioButton localArtifactRadioButton;
    @NotNull
    public SparkSubmissionJobUploadStorageWithUploadPathPanel storageWithUploadPathPanel;

    private BehaviorSubject<String> clusterSelectedSubject = BehaviorSubject.create();

    private enum ErrorMessage {
        ClusterName,
        SystemArtifact,
        LocalArtifact,
        MainClass,
        JobConfiguration
        // Don't add more Error Message please, throw Configuration Exception in checkInputs()
    }

    @NotNull
    private final JLabel[] errorMessageLabels = new JLabel[5]; // Fix the size rather than ErrorMessage.values().length
                                                               // since we won't like to add more message labels

    private void initializeComponents(){
        setLayout(new GridBagLayout());

        addSparkClustersLineItem();
        addSelectedArtifactLineItem();
        addMainClassNameLineItem();

        addConfigurationLineItem();

        //TODO : CommandlineArgs Parameter Valid Check
        addCommandlineArgsLineItem();
        // TODO: ReferencedJarsPath Parameter Valid Check
        addReferencedJarsLineItem();
        //TODO: ReferencedFiles Parameter Valid Check
        addReferencedFilesLineItem();

        addJobUploadStorageItem();
    }

    void updateTableColumn() {
        TableColumn keyColumn = jobConfigurationTable.getColumnModel().getColumn(InteractiveTableModel.KEY_INDEX);
        keyColumn.setCellRenderer(new InteractiveRenderer(InteractiveTableModel.KEY_INDEX));
    }

    @NotNull
    ComboBox<Artifact> getSelectedArtifactComboBox() {
        return selectedArtifactComboBox;
    }

    @NotNull
    JBTable getJobConfigurationTable() {
        return jobConfigurationTable;
    }

    @NotNull
    ComboboxWithBrowseButton getClustersListComboBox() {
        return clustersListComboBox;
    }

    @NotNull
    JRadioButton getLocalArtifactRadioButton() {
        return localArtifactRadioButton;
    }

    @NotNull
    JRadioButton getIntelliJArtifactRadioButton() {
        return intelliJArtifactRadioButton;
    }

    @NotNull
    TextFieldWithBrowseButton getLocalArtifactTextField() {
        return localArtifactTextField;
    }

    @NotNull
    TextFieldWithBrowseButton getMainClassTextField() {
        return mainClassTextField;
    }

    @NotNull
    JTextField getCommandLineTextField() {
        return commandLineTextField;
    }

    @NotNull
    JTextField getReferencedJarsTextField() {
        return referencedJarsTextField;
    }

    @NotNull
    JTextField getReferencedFilesTextField() {
        return referencedFilesTextField;
    }

    @NotNull
    BehaviorSubject<String> getClusterSelectedSubject() {
        return clusterSelectedSubject;
    }

    void setClustersListRefreshEnabled(boolean enabled) {
        clustersListComboBox.setButtonEnabled(enabled);
    }

    void addClusterListRefreshActionListener(ActionListener actionListener) {
        clustersListComboBox.getButton().addActionListener(actionListener);
    }

    private void addSparkClustersLineItem() {
        JLabel sparkClusterLabel = new JLabel("Spark clusters(Linux only)");
        sparkClusterLabel.setToolTipText("The HDInsight Spark cluster you want to submit your application to. Only Linux cluster is supported.");
        add(sparkClusterLabel,
                new GridBagConstraints(0, displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insets(margin, margin, 0, margin), 0, 0));


        clustersListComboBox = new ComboboxWithBrowseButton();
        clustersListComboBox.setButtonIcon(StreamUtil.getImageResourceFile(REFRESH_BUTTON_PATH));
        clustersListComboBox.getButton().setToolTipText("Refresh");
        clustersListComboBox.getComboBox().setToolTipText("The HDInsight Spark cluster you want to submit your application to. Only Linux cluster is supported.");
        clustersListComboBox.getComboBox().addPropertyChangeListener(evt -> checkInputsWithErrorLabels());
        clustersListComboBox.getComboBox().addItemListener(ev ->
                clusterSelectedSubject.onNext(ev.getStateChange() == ItemEvent.SELECTED ? ev.getItem().toString() : null));

        add(clustersListComboBox,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin, margin, 0, margin), 0, 0));

        boolean isSignedIn = false;

        try {
            isSignedIn = AuthMethodManager.getInstance().isSignedIn();
        } catch (IOException ignored) { }

        errorMessageLabels[ErrorMessage.ClusterName.ordinal()] = new JLabel( isSignedIn ?
                        "Cluster Name Should not be null, please choose one for submission" :
                        "Can't list cluster, please login within Azure Explorer (View -> Tool Windows -> Azure Explorer) and refresh");
        errorMessageLabels[ErrorMessage.ClusterName.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());

        clustersListComboBox.getComboBox().addItemListener(e -> setVisibleForFixedErrorMessageLabel(ErrorMessage.ClusterName, clustersListComboBox.getComboBox().getItemCount() == 0));

        add(errorMessageLabels[ErrorMessage.ClusterName.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insetsLeft(margin), 0, 0));
    }

    private DocumentListener documentValidationListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            checkInputsWithErrorLabels();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            checkInputsWithErrorLabels();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            checkInputsWithErrorLabels();
        }
    };

    private void addSelectedArtifactLineItem() {
        final String tipInfo = "The Artifact you want to use.";
        JLabel artifactSelectLabel = new JLabel("Select an Artifact to submit");
        artifactSelectLabel.setToolTipText(tipInfo);

        selectedArtifactComboBox = new ComboBox<>();
        selectedArtifactComboBox.setToolTipText(tipInfo);

        errorMessageLabels[ErrorMessage.SystemArtifact.ordinal()] = new JLabel("Artifact should not be null!");
        errorMessageLabels[ErrorMessage.SystemArtifact.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());

        errorMessageLabels[ErrorMessage.LocalArtifact.ordinal()] = new JLabel("Could not find the local jar package for Artifact");
        errorMessageLabels[ErrorMessage.LocalArtifact.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());

        localArtifactTextField = new TextFieldWithBrowseButton();
        localArtifactTextField.setToolTipText("Artifact from local jar package.");
        localArtifactTextField.setEnabled(false);
        localArtifactTextField.getTextField().getDocument().addDocumentListener(documentValidationListener);

        localArtifactTextField.getButton().addActionListener(e -> {
            FileChooserDescriptor chooserDescriptor = new FileChooserDescriptor(false, false, true, false, true, false);
            chooserDescriptor.setTitle("Select Local Artifact File");
            VirtualFile chooseFile = FileChooser.chooseFile(chooserDescriptor, null, null);
            if (chooseFile != null) {
                String path = chooseFile.getPath();
                if (path.endsWith("!/")) {
                    path = path.substring(0, path.length() - 2);
                }
                localArtifactTextField.setText(path);
            }
        });


        intelliJArtifactRadioButton = new JRadioButton("Artifact from IntelliJ project:", true);
        intelliJArtifactRadioButton.addItemListener(e -> {
            selectedArtifactComboBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            checkInputsWithErrorLabels();
        });

        localArtifactRadioButton = new JRadioButton("Artifact from local disk:", false);
        localArtifactRadioButton.addItemListener(e -> {
            localArtifactTextField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            checkInputsWithErrorLabels();
        });

        ButtonGroup group = new ButtonGroup();
        group.add(intelliJArtifactRadioButton);
        group.add(localArtifactRadioButton);

        intelliJArtifactRadioButton.setSelected(true);

        add(artifactSelectLabel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insets(margin, margin, 0, margin), 0, 0));

        add(intelliJArtifactRadioButton,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insets(margin / 3, margin * 3, 0, margin), 0, 0));

        add(selectedArtifactComboBox,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin / 3, margin, 0, margin), 0, 0));

        add(errorMessageLabels[ErrorMessage.SystemArtifact.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insetsLeft(margin), 0, 0));

        add(localArtifactRadioButton,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insets(margin / 3, margin * 3, 0, margin), 0, 0));

        add(localArtifactTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin / 3, margin, 0, margin), 0, 0));
        add(errorMessageLabels[ErrorMessage.LocalArtifact.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insetsLeft(margin), 0, 0));
    }

    private void addMainClassNameLineItem() {
        JLabel sparkMainClassLabel = new JLabel("Main class name");
        sparkMainClassLabel.setToolTipText("Application's java/spark main class");
        add(sparkMainClassLabel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        JBUI.insets(margin, margin, 0, 0), 0, 0));

        mainClassTextField = new TextFieldWithBrowseButton();
        mainClassTextField.setToolTipText("Application's java/spark main class");

        add(mainClassTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin, margin, 0, margin), 0, 0));

        errorMessageLabels[ErrorMessage.MainClass.ordinal()] = new JLabel("Main Class Name should not be null");
        errorMessageLabels[ErrorMessage.MainClass.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());
        errorMessageLabels[ErrorMessage.MainClass.ordinal()].setVisible(true);

        mainClassTextField.getTextField().getDocument().addDocumentListener(documentValidationListener);

        add(errorMessageLabels[ErrorMessage.MainClass.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(0, margin), 0, 0));
    }

    private void addConfigurationLineItem() {
        JLabel jobConfigurationLabel = new JLabel("Job configurations");

        add(jobConfigurationLabel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        JBUI.insets(margin, margin, 0, 0), 0, 0));

        jobConfigurationTable = new JBTable(new SubmissionTableModel());
        Dimension jobConfigurationTableSize = new Dimension(320, 100);
        jobConfigurationTable.setPreferredScrollableViewportSize(jobConfigurationTableSize);

        jobConfigurationTable.setSurrendersFocusOnKeystroke(true);
        jobConfigurationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobConfigurationTable.setColumnSelectionAllowed(true);
        JBScrollPane scrollPane = new JBScrollPane(jobConfigurationTable);
        jobConfigurationTable.setFillsViewportHeight(true);
        scrollPane.setMinimumSize(jobConfigurationTableSize);

        jobConfigurationTable.addPropertyChangeListener((evt)-> checkInputsWithErrorLabels());

        add(scrollPane,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin, margin, 0, 0), 0, 0));

        JButton loadJobConfigurationButton = new JButton("...");
        loadJobConfigurationButton.setPreferredSize(localArtifactTextField.getButton().getPreferredSize());

        errorMessageLabels[ErrorMessage.JobConfiguration.ordinal()] = new JLabel();
        errorMessageLabels[ErrorMessage.JobConfiguration.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());
        errorMessageLabels[ErrorMessage.JobConfiguration.ordinal()].setVisible(false);

        add(errorMessageLabels[ErrorMessage.JobConfiguration.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(0, margin), 0, 0));

    }

    private void addCommandlineArgsLineItem() {
        JLabel commandLineArgs = new JLabel("Command line arguments");
        commandLineArgs.setToolTipText("Command line arguments used in your main class; multiple arguments should be split by space.");

        add(commandLineArgs,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        JBUI.insets(margin, margin, 0, 0), 0, 0));

        commandLineTextField = new JTextField();
        commandLineTextField.setToolTipText("Command line arguments used in your main class; multiple arguments should be split by space.");

        add(commandLineTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin, margin, 0, margin), 0, 0));
    }

    private void addReferencedJarsLineItem() {
        JLabel commandLineArgs = new JLabel("Referenced Jars(spark.jars)");
        commandLineArgs.setToolTipText("Files to be placed on the java classpath; The path needs to be an Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;)");

        add(commandLineArgs,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        JBUI.insets(margin, margin, 0, 0), 0, 0));

        referencedJarsTextField = new JTextField();
        referencedJarsTextField.setToolTipText("Files to be placed on the java classpath; The path needs to be an Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;)");
        add(referencedJarsTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        JBUI.insets(margin, margin, 0, margin), 0, 0));
    }

    private void addReferencedFilesLineItem() {
        JLabel commandLineArgs = new JLabel("Referenced Files(spark.files)");
        commandLineArgs.setToolTipText("Files to be placed in executor working directory. The path needs to be an Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;) ");
        add(commandLineArgs,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        JBUI.insets(margin, margin, 0, 0), 0, 0));

        referencedFilesTextField = new JTextField();
        referencedFilesTextField.setToolTipText("Files to be placed in executor working directory. The path needs to be an Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;) ");
        add(referencedFilesTextField, new GridBagConstraints(1, displayLayoutCurrentRow,
                0, 1,
                1, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.insets(margin, margin, 0, margin), 0, 0));
    }

    private void addJobUploadStorageItem() {
        storageWithUploadPathPanel = new SparkSubmissionJobUploadStorageWithUploadPathPanel();
        add(storageWithUploadPathPanel, new GridBagConstraints(0, ++displayLayoutCurrentRow,
                0, 1,
                0, 0,
                GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.insets(margin, margin, 0, margin), 0, 0));
    }

    private void setVisibleForFixedErrorMessageLabel(@NotNull ErrorMessage label, boolean isVisible) {
        setStatusForMessageLabel(label, isVisible, null);
    }

    private void setStatusForMessageLabel(@NotNull ErrorMessage label, boolean isVisible, @Nullable String message, boolean isWarning) {
        if (!StringHelper.isNullOrWhiteSpace(message)) {
            errorMessageLabels[label.ordinal()].setText(message);
        }

        errorMessageLabels[label.ordinal()].setForeground(isWarning ? DarkThemeManager.getInstance().getWarningMessageColor() : DarkThemeManager.getInstance().getErrorMessageColor());
        this.errorMessageLabels[label.ordinal()].setVisible(isVisible);
    }

    private void setStatusForMessageLabel(@NotNull ErrorMessage label, boolean isVisible, @Nullable String message) {
        setStatusForMessageLabel(label, isVisible, message, false);
    }

    private void hideAllErrors() {
        for (ErrorMessage errorMessageLabel : ErrorMessage.values()) {
            setVisibleForFixedErrorMessageLabel(errorMessageLabel, false);
        }
    }

    private void cleanUp() {
        clusterSelectedSubject.onCompleted();
    }

    private synchronized void checkInputsWithErrorLabels() {
        // Clean all error messages firstly
        hideAllErrors();

        // Check Cluster selection
        if (clustersListComboBox.getComboBox().getSelectedItem() == null) {
            setVisibleForFixedErrorMessageLabel(ErrorMessage.ClusterName, true);
        }

        if (intelliJArtifactRadioButton.isSelected()) {
            // Check Intellij artifact
            if (selectedArtifactComboBox.getSelectedItem() == null) {
                setVisibleForFixedErrorMessageLabel(ErrorMessage.SystemArtifact, true);
            }
        }

        if (localArtifactRadioButton.isSelected()) {
            // Check local jar artifact
            if (StringHelper.isNullOrWhiteSpace(localArtifactTextField.getText())) {
                setVisibleForFixedErrorMessageLabel(ErrorMessage.LocalArtifact, true);
            }

            if (!SparkSubmitHelper.isLocalArtifactPath(localArtifactTextField.getText())) {
                setVisibleForFixedErrorMessageLabel(ErrorMessage.LocalArtifact, true);
            }
        }

        // Check main class input
        if (StringUtils.isBlank(mainClassTextField.getText())) {
            setVisibleForFixedErrorMessageLabel(ErrorMessage.MainClass, true);
        }

        // Check job config table
        SubmissionTableModel confTableModel = ((SubmissionTableModel) getJobConfigurationTable().getModel());
        SparkSubmissionJobConfigCheckResult result = confTableModel.getFirstCheckResults();
        if (result != null) {
            setStatusForMessageLabel(ErrorMessage.JobConfiguration, true, result.getMessaqge(), result.getStatus() == SparkSubmissionJobConfigCheckStatus.Warning);
        }
    }

    public void checkInputs() throws ConfigurationException {
        ApplicationManager.getApplication().invokeAndWait(this::checkInputsWithErrorLabels, ModalityState.any());

        // Convert Error Labels into Configuration Exception
        java.util.List<String> errors = getErrorMessages();
        if (!errors.isEmpty()) {
            throw new ConfigurationException(String.join("; \n", errors));
        }

        SubmissionTableModel confTableModel = ((SubmissionTableModel) getJobConfigurationTable().getModel());
        for (Pair<String, String> confEntry : confTableModel.getJobConfigMap()) {
            String entryKey = confEntry.first();

            if (!StringUtils.isAlpha(entryKey.substring(0, 1)) && !StringUtils.startsWith(entryKey, "_")) {
                throw new RuntimeConfigurationError("The Spark config key should start with a letter or underscore");
            }

            if (!StringUtils.containsOnly(entryKey.toLowerCase(), "abcdefghijklmnopqrstuvwxyz1234567890_-.")) {
                throw new RuntimeConfigurationError("The Spark config key should only contains letters, digits, hyphens, underscores, and periods: (" + entryKey + ")");
            }

        }

    }
}
