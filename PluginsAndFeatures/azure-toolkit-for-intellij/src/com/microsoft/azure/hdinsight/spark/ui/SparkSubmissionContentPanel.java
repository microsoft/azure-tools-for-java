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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.DarkThemeManager;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionJobConfigCheckResult;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionJobConfigCheckStatus;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitHelper;
import com.microsoft.azure.hdinsight.spark.common.SubmissionTableModel;
import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveRenderer;
import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveTableModel;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.subjects.BehaviorSubject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.TimeUnit;

public class SparkSubmissionContentPanel extends JPanel{
    public SparkSubmissionContentPanel(@Nullable CallBack updateCallBack){
        this.updateCallBack = updateCallBack;

        initializeComponents();

        addContainerListener(new ContainerAdapter() {
            @Override
            public void componentRemoved(ContainerEvent e) {
                cleanUp();

                super.componentRemoved(e);
            }
        });
    }

    public boolean haveErrorMessage() {
        for (int i = 0; i < errorMessageLabels.length; ++i) {
            if (errorMessageLabels[i].isVisible() && errorMessageLabels[i].getForeground().equals(DarkThemeManager.getInstance().getErrorMessageColor())) {
                return true;
            }
        }

        return false;
    }

    public int displayLayoutCurrentRow = 0;

    private CallBack updateCallBack;

    private final int margin = 10;
    private static final String REFRESH_BUTTON_PATH = "/icons/refresh.png";

    @NotNull
    private ComboboxWithBrowseButton clustersListComboBox;
    @NotNull
    private ComboBox<String> selectedArtifactComboBox;
    @NotNull
    private TextFieldWithBrowseButton selectedArtifactTextField;
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
    private final JLabel[] errorMessageLabels = new JLabel[5];
    @NotNull
    private SparkSubmissionAdvancedConfigDialog advancedConfigDialog;
    @NotNull
    private JButton advancedConfigButton= new JButton("Advanced configuration");
    @NotNull
    FixedSizeButton loadJobConfigurationFixedSizeButton;

    private BehaviorSubject<String> clusterSelectedSubject = BehaviorSubject.create();

    private enum ErrorMessageLabelTag {
        ClusterName,
        SystemArtifact,
        LocalArtifact,
        MainClass,
        JobConfiguration;
    }

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

        addAdvancedConfigLineItem();
    }

    void updateTableColumn() {
        TableColumn hidden = jobConfigurationTable.getColumnModel().getColumn(InteractiveTableModel.HIDDEN_INDEX);
        hidden.setMinWidth(2);
        hidden.setPreferredWidth(2);
        hidden.setMaxWidth(2);
        hidden.setCellRenderer(new InteractiveRenderer(InteractiveTableModel.HIDDEN_INDEX));

        TableColumn keyColumn = jobConfigurationTable.getColumnModel().getColumn(InteractiveTableModel.KEY_INDEX);
        keyColumn.setCellRenderer(new InteractiveRenderer(InteractiveTableModel.KEY_INDEX));
        
        if (updateCallBack != null) {
            updateCallBack.run();
        }
    }

    @NotNull
    ComboBox<String> getSelectedArtifactComboBox() {
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
    TextFieldWithBrowseButton getSelectedArtifactTextField() {
        return selectedArtifactTextField;
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
    SparkSubmissionAdvancedConfigDialog getAdvancedConfigDialog() {
        return advancedConfigDialog;
    }

    @NotNull
    BehaviorSubject<String> getClusterSelectedSubject() {
        return clusterSelectedSubject;
    }

    void setClustersListRefreshEnabled(boolean enabled) {
        clustersListComboBox.getButton().setEnabled(enabled);
    }

    void addClusterListRefreshActionListener(ActionListener actionListener) {
        clustersListComboBox.getButton().addActionListener(actionListener);
    }

    private void addSparkClustersLineItem() {
        JLabel sparkClusterLabel = new JLabel("Spark clusters(Linux only)");
        sparkClusterLabel.setToolTipText("The HDInsight Spark cluster you want to submit your application to. Only Linux cluster is supported.");
        GridBagConstraints c11 = new GridBagConstraints();
        c11.gridx = 0;
        c11.gridy = 0;
        c11.insets = new Insets(margin, margin, 0, margin);
        add(sparkClusterLabel,
                new GridBagConstraints(0, displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, margin), 0, 0));


        clustersListComboBox = new ComboboxWithBrowseButton();
        clustersListComboBox.setButtonIcon(StreamUtil.getImageResourceFile(REFRESH_BUTTON_PATH));
        clustersListComboBox.getButton().setToolTipText("Refresh");
        clustersListComboBox.getComboBox().setToolTipText("The HDInsight Spark cluster you want to submit your application to. Only Linux cluster is supported.");
        clustersListComboBox.getComboBox().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("model") && evt.getNewValue() instanceof DefaultComboBoxModel) {
                    int size = ((DefaultComboBoxModel) evt.getNewValue()).getSize();
                    setVisibleForFixedErrorMessageLabel(ErrorMessageLabelTag.ClusterName.ordinal(), size <= 0);
                }
            }
        });
        clustersListComboBox.getComboBox().addItemListener(ev ->
                clusterSelectedSubject.onNext(ev.getStateChange() == ItemEvent.SELECTED ? ev.getItem().toString() : null));

        add(clustersListComboBox,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin, margin, 0, margin), 0, 0));

        errorMessageLabels[ErrorMessageLabelTag.ClusterName.ordinal()] = new JLabel("Cluster Name Should not be null");
        errorMessageLabels[ErrorMessageLabelTag.ClusterName.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());

        clustersListComboBox.getComboBox().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setVisibleForFixedErrorMessageLabel(0, clustersListComboBox.getComboBox().getItemCount() == 0);
            }
        });

        add(errorMessageLabels[ErrorMessageLabelTag.ClusterName.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, margin, 0, 0), 0, 0));
    }

    private void addSelectedArtifactLineItem() {
        final String tipInfo = "The Artifact you want to use.";
        JLabel artifactSelectLabel = new JLabel("Select an Artifact to submit");
        artifactSelectLabel.setToolTipText(tipInfo);

        selectedArtifactComboBox = new ComboBox<>();
        selectedArtifactComboBox.setToolTipText(tipInfo);

        errorMessageLabels[ErrorMessageLabelTag.SystemArtifact.ordinal()] = new JLabel("Artifact should not be null!");
        errorMessageLabels[ErrorMessageLabelTag.SystemArtifact.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());
        errorMessageLabels[ErrorMessageLabelTag.SystemArtifact.ordinal()].setVisible(false);

        errorMessageLabels[ErrorMessageLabelTag.LocalArtifact.ordinal()] = new JLabel("Could not find the local jar package for Artifact");
        errorMessageLabels[ErrorMessageLabelTag.LocalArtifact.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());
        errorMessageLabels[ErrorMessageLabelTag.LocalArtifact.ordinal()].setVisible(false);

        selectedArtifactTextField = new TextFieldWithBrowseButton();
        selectedArtifactTextField.setToolTipText("Artifact from local jar package.");
        selectedArtifactTextField.setEditable(true);
        selectedArtifactTextField.setEnabled(false);
        selectedArtifactTextField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setVisibleForFixedErrorMessageLabel(2, !SparkSubmitHelper.isLocalArtifactPath(selectedArtifactTextField.getText()));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setVisibleForFixedErrorMessageLabel(2, !SparkSubmitHelper.isLocalArtifactPath(selectedArtifactTextField.getText()));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setVisibleForFixedErrorMessageLabel(2, !SparkSubmitHelper.isLocalArtifactPath(selectedArtifactTextField.getText()));
            }
        });

        selectedArtifactTextField.getButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileChooserDescriptor chooserDescriptor = new FileChooserDescriptor(false, false, true, false, true, false);
                chooserDescriptor.setTitle("Select Local Artifact File");
                VirtualFile chooseFile = FileChooser.chooseFile(chooserDescriptor, null, null);
                if (chooseFile != null) {
                    String path = chooseFile.getPath();
                    if (path.endsWith("!/")) {
                        path = path.substring(0, path.length() - 2);
                    }
                    selectedArtifactTextField.setText(path);
                }
            }
        });


        intelliJArtifactRadioButton = new JRadioButton("Artifact from IntelliJ project:", true);
        localArtifactRadioButton = new JRadioButton("Artifact from local disk:", false);

        intelliJArtifactRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    selectedArtifactComboBox.setEnabled(true);
                    selectedArtifactTextField.setEnabled(false);
                    mainClassTextField.setButtonEnabled(true);

                    setVisibleForFixedErrorMessageLabel(2, false);

                    if (selectedArtifactComboBox.getItemCount() == 0) {
                        setVisibleForFixedErrorMessageLabel(2, true);
                    }
                }
            }
        });

        localArtifactRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    selectedArtifactComboBox.setEnabled(false);
                    selectedArtifactTextField.setEnabled(true);
                    mainClassTextField.setButtonEnabled(false);

                    setVisibleForFixedErrorMessageLabel(1, false);

                    if (StringHelper.isNullOrWhiteSpace(selectedArtifactTextField.getText())) {
                        setVisibleForFixedErrorMessageLabel(2, true);
                    }
                }
            }
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
                        new Insets(margin, margin, 0, margin), 0, 0));

        add(intelliJArtifactRadioButton,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin * 3, 0, margin), 0, 0));

        add(selectedArtifactComboBox,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin / 3, margin, 0, margin), 0, 0));

        add(errorMessageLabels[ErrorMessageLabelTag.SystemArtifact.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, margin, 0, 0), 0, 0));

        add(localArtifactRadioButton,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin * 3, 0, margin), 0, 0));

        add(selectedArtifactTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin / 3, margin, 0, margin), 0, 0));
        add(errorMessageLabels[ErrorMessageLabelTag.LocalArtifact.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, margin, 0, 0), 0, 0));
    }

    private void addMainClassNameLineItem() {
        JLabel sparkMainClassLabel = new JLabel("Main class name");
        sparkMainClassLabel.setToolTipText("Application's java/spark main class");
        GridBagConstraints c31 = new GridBagConstraints();
        c31.gridx = 0;
        c31.gridy = 2;
        c31.insets = new Insets(margin, margin, margin, margin);
        add(sparkMainClassLabel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        mainClassTextField = new TextFieldWithBrowseButton();
        mainClassTextField.setToolTipText("Application's java/spark main class");

        add(mainClassTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin, margin, 0, margin), 0, 0));

        errorMessageLabels[ErrorMessageLabelTag.MainClass.ordinal()] = new JLabel("Main Class Name should not be null");
        errorMessageLabels[ErrorMessageLabelTag.MainClass.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());
        errorMessageLabels[ErrorMessageLabelTag.MainClass.ordinal()].setVisible(true);

        mainClassTextField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setVisibleForFixedErrorMessageLabel(3, e.getDocument().getLength() == 0);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setVisibleForFixedErrorMessageLabel(3, e.getDocument().getLength() == 0);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        add(errorMessageLabels[ErrorMessageLabelTag.MainClass.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(0, margin, 0, margin), 0, 0));
    }

    void addJobConfigurationLoadButtonActionListener(ActionListener actionListener) {
        loadJobConfigurationFixedSizeButton.addActionListener(actionListener);
    }

    void addAdvancedConfigurationButtonActionListener(ActionListener actionListener) {
        advancedConfigButton.addActionListener(actionListener);
    }

    private void addConfigurationLineItem() {
        JLabel jobConfigurationLabel = new JLabel("Job configurations");

        add(jobConfigurationLabel,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        String[] columns = {"Key", "Value", ""};

        jobConfigurationTable = new JBTable();
        Dimension jobConfigurationTableSize = new Dimension(320, 100);
        jobConfigurationTable.setPreferredScrollableViewportSize(jobConfigurationTableSize);

        jobConfigurationTable.setSurrendersFocusOnKeystroke(true);
        jobConfigurationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobConfigurationTable.setColumnSelectionAllowed(true);
        JBScrollPane scrollPane = new JBScrollPane(jobConfigurationTable);
        jobConfigurationTable.setFillsViewportHeight(true);
        scrollPane.setMinimumSize(jobConfigurationTableSize);

        jobConfigurationTable.addPropertyChangeListener((evt)-> {
            if ((evt.getPropertyName().equals("tableCellEditor") || evt.getPropertyName().equals("model")) && jobConfigurationTable.getModel() instanceof SubmissionTableModel) {
                SubmissionTableModel model = (SubmissionTableModel) jobConfigurationTable.getModel();
                setVisibleForFixedErrorMessageLabel(ErrorMessageLabelTag.JobConfiguration.ordinal(), false);

                SparkSubmissionJobConfigCheckResult result = model.getFirstCheckResults();
                if (result != null) {
                    setStatusForMessageLabel(ErrorMessageLabelTag.JobConfiguration.ordinal(), true, result.getMessaqge(), result.getStatus() == SparkSubmissionJobConfigCheckStatus.Warning);
                }
            }
        });

        add(scrollPane,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin, margin, 0, 0), 0, 0));

        JButton loadJobConfigurationButton = new JButton("...");
        loadJobConfigurationButton.setPreferredSize(selectedArtifactTextField.getButton().getPreferredSize());
        loadJobConfigurationFixedSizeButton = new FixedSizeButton(loadJobConfigurationButton);

        add(loadJobConfigurationFixedSizeButton,
                new GridBagConstraints(2, displayLayoutCurrentRow,
                        0, 1,
                        0, 0,
                        GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
                        new Insets(margin, margin / 2, 0, margin), 0, 0));
        loadJobConfigurationFixedSizeButton.setToolTipText("Load Spark config from property file");

        errorMessageLabels[ErrorMessageLabelTag.JobConfiguration.ordinal()] = new JLabel();
        errorMessageLabels[ErrorMessageLabelTag.JobConfiguration.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());
        errorMessageLabels[ErrorMessageLabelTag.JobConfiguration.ordinal()].setVisible(false);

        add(errorMessageLabels[ErrorMessageLabelTag.JobConfiguration.ordinal()],
                new GridBagConstraints(1, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(0, margin, 0, margin), 0, 0));

    }

    private void addCommandlineArgsLineItem() {
        JLabel commandLineArgs = new JLabel("Command line arguments");
        commandLineArgs.setToolTipText("Command line arguments used in your main class; multiple arguments should be split by space.");

        add(commandLineArgs,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        commandLineTextField = new JTextField();
        commandLineTextField.setToolTipText("Command line arguments used in your main class; multiple arguments should be split by space.");

        add(commandLineTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin, margin, 0, margin), 0, 0));
    }

    private void addReferencedJarsLineItem() {
        JLabel commandLineArgs = new JLabel("Referenced Jars");
        commandLineArgs.setToolTipText("Files to be placed on the java classpath; The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;)");

        add(commandLineArgs,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        referencedJarsTextField = new JTextField();
        referencedJarsTextField.setToolTipText("Files to be placed on the java classpath; The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;)");
        add(referencedJarsTextField,
                new GridBagConstraints(1, displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin, margin, 0, margin), 0, 0));
    }

    private void addAdvancedConfigLineItem() {
        advancedConfigButton.setEnabled(false);
        advancedConfigButton.setToolTipText("Specify advanced configuration, for example, enabling Spark remote debug");

        clusterSelectedSubject
                .throttleWithTimeout(200, TimeUnit.MILLISECONDS)
                .subscribe(cluster -> advancedConfigButton.setEnabled(cluster != null));

        add(advancedConfigButton,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        0, 1,
                        1, 0,
                        GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        advancedConfigDialog = new SparkSubmissionAdvancedConfigDialog();
    }

    private void addReferencedFilesLineItem() {
        JLabel commandLineArgs = new JLabel("Referenced Files");
        commandLineArgs.setToolTipText("Files to be placed in executor working directory. The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;) ");
        add(commandLineArgs,
                new GridBagConstraints(0, ++displayLayoutCurrentRow,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        referencedFilesTextField = new JTextField();
        referencedFilesTextField.setToolTipText("Files to be placed in executor working directory. The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;) ");
        add(referencedFilesTextField, new GridBagConstraints(1, displayLayoutCurrentRow,
                0, 1,
                1, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(margin, margin, 0, margin), 0, 0));
    }

    private void setVisibleForFixedErrorMessageLabel(@NotNull int label, @NotNull boolean isVisible) {
        setStatusForMessageLabel(label, isVisible, null);
    }

    private void setStatusForMessageLabel(@NotNull int label, @NotNull boolean isVisible, @Nullable String message, boolean isWarning) {
        if (!StringHelper.isNullOrWhiteSpace(message)) {
            errorMessageLabels[label].setText(message);
        }

        errorMessageLabels[label].setForeground(isWarning ? DarkThemeManager.getInstance().getWarningMessageColor() : DarkThemeManager.getInstance().getErrorMessageColor());
        this.errorMessageLabels[label].setVisible(isVisible);

        if (updateCallBack != null) {
            updateCallBack.run();
        }
    }

    private void setStatusForMessageLabel(@NotNull int label, @NotNull boolean isVisible, @Nullable String message) {
        setStatusForMessageLabel(label, isVisible, message, false);
    }

    public void cleanUp() {
        clusterSelectedSubject.onCompleted();
    }
}
