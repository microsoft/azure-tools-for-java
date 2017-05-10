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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.hdinsight.common.*;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveRenderer;
import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveTableModel;
import com.microsoft.azure.hdinsight.spark.common.*;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SparkSubmissionExDialog extends JDialog {
    private JPanel contentPane;

    private final int margin = 10;
    private final String DialogTitle = "Spark Submission";

    private int row = 0;
    private ComboboxWithBrowseButton clustersListComboBox;
    private ComboBox selectedArtifactComboBox;
    private TextFieldWithBrowseButton selectedArtifactTextField;
    private TextFieldWithBrowseButton mainClassTextField;
    private JBTable jobConfigurationTable;
    private JTextField commandLineTextField;
    private JTextField referencedJarsTextField;
    private JTextField referencedFilesTextField;
    private JRadioButton intelliJArtifactRadioButton;
    private JRadioButton localArtifactRadioButton;
    private JButton buttonSubmit;

    private static final String REFRESH_BUTTON_PATH = "/icons/refresh.png";

    private final JLabel[] errorMessageLabels = new JLabel[5];
    private enum ErrorMessageLabelTag {
        ClusterName,
        SystemArtifact,
        LocalArtifact,
        MainClass,
        JobConfiguration;
    }

    private Project project;
    private SparkSubmitModel submitModel;
    private CallBack callBack;

    public SparkSubmissionExDialog(@NotNull Project project, @NotNull List<IClusterDetail> cachedClusterDetails, @Nullable CallBack callBack) {
        this.project = project;
        this.callBack = callBack;
        submitModel = new SparkSubmitModel(project, cachedClusterDetails);

        initializeComponents();
        initializeModel();
        updateTableColumn();
        loadParameter();
        setSubmitButtonStatus();
        this.pack();
    }

    public void loadParameter() {
        SparkSubmissionParameter parameter = submitModel.getSubmissionParameter();
        if (parameter != null) {
            if (parameter.isLocalArtifact()) {
                localArtifactRadioButton.setSelected(true);
            }

            selectedArtifactTextField.setText(parameter.getLocalArtifactPath());
            mainClassTextField.setText(parameter.getMainClassName());
            commandLineTextField.setText(StringHelper.join(" ", parameter.getArgs()));
            referencedJarsTextField.setText(StringHelper.join(";", parameter.getReferencedJars()));
            referencedFilesTextField.setText(StringHelper.join(";", parameter.getReferencedFiles()));
        }
    }

    //region UI Constructor
    private void initializeComponents() {
        Image image = StreamUtil.getImageResourceFile(CommonConst.ProductIConPath).getImage();
        setIconImage(image);

        contentPane = new JPanel();
        setContentPane(contentPane);

        contentPane.setLayout(new GridBagLayout());
        setModal(true);
        setTitle(DialogTitle);

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
        addOperationJPanel();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initializeModel() {
        clustersListComboBox.getComboBox().setModel(submitModel.getClusterComboBoxModel());
        selectedArtifactComboBox.setModel(submitModel.getArtifactComboBoxModel());
        jobConfigurationTable.setModel(submitModel.getTableModel());
    }

    private void updateTableColumn() {
        TableColumn hidden = jobConfigurationTable.getColumnModel().getColumn(InteractiveTableModel.HIDDEN_INDEX);
        hidden.setMinWidth(2);
        hidden.setPreferredWidth(2);
        hidden.setMaxWidth(2);
        hidden.setCellRenderer(new InteractiveRenderer(InteractiveTableModel.HIDDEN_INDEX));
        this.pack();
    }

    private void addSparkClustersLineItem() {
        JLabel sparkClusterLabel = new JLabel("Spark clusters(Linux only)");
        sparkClusterLabel.setToolTipText("The HDInsight Spark cluster you want to submit your application to. Only Linux cluster is supported.");
        GridBagConstraints c11 = new GridBagConstraints();
        c11.gridx = 0;
        c11.gridy = 0;
        c11.insets = new Insets(margin, margin, 0, margin);
        contentPane.add(sparkClusterLabel,
                new GridBagConstraints(0, row,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, margin), 0, 0));


        clustersListComboBox = new ComboboxWithBrowseButton();
        clustersListComboBox.setButtonIcon(StreamUtil.getImageResourceFile(REFRESH_BUTTON_PATH));
        clustersListComboBox.getButton().setToolTipText("Refresh");
        clustersListComboBox.getButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Cursor cursor = contentPane.getCursor();
                contentPane.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                ClusterManagerEx.getInstance().getClusterDetails(project);
                contentPane.setCursor(cursor);
            }
        });
        clustersListComboBox.getComboBox().setToolTipText("The HDInsight Spark cluster you want to submit your application to. Only Linux cluster is supported.");
        clustersListComboBox.getComboBox().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() == "model" && evt.getNewValue() instanceof DefaultComboBoxModel) {
                    int size = ((DefaultComboBoxModel) evt.getNewValue()).getSize();
                    setVisibleForFixedErrorMessageLabel(ErrorMessageLabelTag.ClusterName.ordinal(), size <= 0);
                }
            }
        });

        contentPane.add(clustersListComboBox,
                new GridBagConstraints(1, row,
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

        contentPane.add(errorMessageLabels[ErrorMessageLabelTag.ClusterName.ordinal()],
                new GridBagConstraints(1, ++row,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, margin, 0, 0), 0, 0));
    }

    private void addSelectedArtifactLineItem() {
        final String tipInfo = "The Artifact you want to use.";
        JLabel artifactSelectLabel = new JLabel("Select an Artifact to submit");
        artifactSelectLabel.setToolTipText(tipInfo);

        selectedArtifactComboBox = new ComboBox();
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

        contentPane.add(artifactSelectLabel,
                new GridBagConstraints(0, ++row,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, margin), 0, 0));

        contentPane.add(intelliJArtifactRadioButton,
                new GridBagConstraints(0, ++row,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin * 3, 0, margin), 0, 0));

        contentPane.add(selectedArtifactComboBox,
                new GridBagConstraints(1, row,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin / 3, margin, 0, margin), 0, 0));

        contentPane.add(errorMessageLabels[ErrorMessageLabelTag.SystemArtifact.ordinal()],
                new GridBagConstraints(1, ++row,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, margin, 0, 0), 0, 0));

        contentPane.add(localArtifactRadioButton,
                new GridBagConstraints(0, ++row,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin * 3, 0, margin), 0, 0));

        contentPane.add(selectedArtifactTextField,
                new GridBagConstraints(1, row,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin / 3, margin, 0, margin), 0, 0));
        contentPane.add(errorMessageLabels[ErrorMessageLabelTag.LocalArtifact.ordinal()],
                new GridBagConstraints(1, ++row,
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
        contentPane.add(sparkMainClassLabel,
                new GridBagConstraints(0, ++row,
                        1, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        mainClassTextField = new TextFieldWithBrowseButton();
        mainClassTextField.setToolTipText("Application's java/spark main class");
        ManifestFileUtil.setupMainClassField(project, mainClassTextField);

        contentPane.add(mainClassTextField,
                new GridBagConstraints(1, row,
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

        contentPane.add(errorMessageLabels[ErrorMessageLabelTag.MainClass.ordinal()],
                new GridBagConstraints(1, ++row,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(0, margin, 0, margin), 0, 0));
    }

    private TextFieldWithBrowseButton localConfigFileTextButton;

    private JRadioButton tableConfigRadioButton;
    private JRadioButton localConfigRadioButton;

    private void addConfigurationLineItem() {
        JLabel jobConfigurationLabel = new JLabel("Job configurations");

        contentPane.add(jobConfigurationLabel,
                new GridBagConstraints(0, ++row,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        localConfigFileTextButton = new TextFieldWithBrowseButton();
        localConfigFileTextButton = new TextFieldWithBrowseButton();
        localConfigFileTextButton.setToolTipText("Job Configuration from local file.");
        localConfigFileTextButton.setEditable(true);
        localConfigFileTextButton.setEnabled(false);
        localConfigFileTextButton.getButton().addActionListener((e)-> {
            FileChooserDescriptor chooserDescriptor = new FileChooserDescriptor(true,
                    false,
                    false,
                    false,
                    true,
                    false);
            chooserDescriptor.setTitle("Select Local Configuration File");
            VirtualFile chooseFile = FileChooser.chooseFile(chooserDescriptor, null, null);
            if (chooseFile != null) {
                String path = chooseFile.getPath();
                localConfigFileTextButton.setText(path);
            }
        });


        localConfigFileTextButton.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                localConfigurationValidate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                localConfigurationValidate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                localConfigurationValidate();
            }
        });

        tableConfigRadioButton = new JRadioButton("Dynamic Configuration", true);
        localConfigRadioButton = new JRadioButton("Configuration from local disk:", false);

        tableConfigRadioButton.addItemListener((event)->{
            if (event.getStateChange() == ItemEvent.SELECTED) {
                jobConfigurationTable.setEnabled(true);
                localConfigFileTextButton.setEnabled(false);
                setVisibleForFixedErrorMessageLabel(ErrorMessageLabelTag.JobConfiguration.ordinal(), false);
            }
        });

        localConfigRadioButton.addItemListener((event)->{
            if(event.getStateChange() == ItemEvent.SELECTED) {
                jobConfigurationTable.setEnabled(false);
                localConfigFileTextButton.setEnabled(true);

                // refresh to trigger data validate
                String text = localConfigFileTextButton.getText();
                localConfigFileTextButton.setText(text);
            }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(tableConfigRadioButton);
        group.add(localConfigRadioButton);

        contentPane.add(tableConfigRadioButton,
                new GridBagConstraints(0, ++row,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin * 3, 0, margin), 0, 0));


        String[] columns = {"Key", "Value", ""};

        jobConfigurationTable = new JBTable();
        jobConfigurationTable.setPreferredScrollableViewportSize(new Dimension(300, 100));

        jobConfigurationTable.setSurrendersFocusOnKeystroke(true);
        jobConfigurationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobConfigurationTable.setColumnSelectionAllowed(true);
        JBScrollPane scrollPane = new JBScrollPane(jobConfigurationTable);
        jobConfigurationTable.setFillsViewportHeight(true);

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

        contentPane.add(scrollPane,
                new GridBagConstraints(1, row,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin, 0, margin), 0, 0));

        errorMessageLabels[ErrorMessageLabelTag.JobConfiguration.ordinal()] = new JLabel();
        errorMessageLabels[ErrorMessageLabelTag.JobConfiguration.ordinal()].setForeground(DarkThemeManager.getInstance().getErrorMessageColor());
        errorMessageLabels[ErrorMessageLabelTag.JobConfiguration.ordinal()].setVisible(false);

        contentPane.add(localConfigRadioButton,
                new GridBagConstraints(0, ++row,
                        1, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(margin / 3, margin * 3, 0, margin), 0, 0));

        contentPane.add(localConfigFileTextButton,
                new GridBagConstraints(1, row,
                        0, 1,
                        0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin / 3, margin, 0, margin), 0, 0));

        contentPane.add(errorMessageLabels[ErrorMessageLabelTag.JobConfiguration.ordinal()],
                new GridBagConstraints(1, ++row,
                        0, 1,
                        1, 0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(0, margin, 0, margin), 0, 0));
    }

    private void localConfigurationValidate () {
        final String fileName = localConfigFileTextButton.getText();
        final File file = new File(fileName);
        if(!file.exists() || file.isDirectory()) {
            setStatusForMessageLabel(ErrorMessageLabelTag.JobConfiguration.ordinal(), true, "Configuration file not found!", false);
        }
        else {
            setStatusForMessageLabel(ErrorMessageLabelTag.JobConfiguration.ordinal(), false,"");
        }
    }

    private void addCommandlineArgsLineItem() {
        JLabel commandLineArgs = new JLabel("Command line arguments");
        commandLineArgs.setToolTipText("Command line arguments used in your main class; multiple arguments should be split by space.");

        contentPane.add(commandLineArgs,
                new GridBagConstraints(0, ++row,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        commandLineTextField = new JTextField();
        commandLineTextField.setToolTipText("Command line arguments used in your main class; multiple arguments should be split by space.");

        contentPane.add(commandLineTextField,
                new GridBagConstraints(1, row,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin, margin, 0, margin), 0, 0));
    }

    private void addReferencedJarsLineItem() {
        JLabel commandLineArgs = new JLabel("Referenced Jars");
        commandLineArgs.setToolTipText("Files to be placed on the java classpath; The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;)");

        contentPane.add(commandLineArgs,
                new GridBagConstraints(0, ++row,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        referencedJarsTextField = new JTextField();
        referencedJarsTextField.setToolTipText("Files to be placed on the java classpath; The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;)");
        contentPane.add(referencedJarsTextField,
                new GridBagConstraints(1, row,
                        0, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(margin, margin, 0, margin), 0, 0));
    }

    private void addReferencedFilesLineItem() {
        JLabel commandLineArgs = new JLabel("Referenced Files");
        commandLineArgs.setToolTipText("Files to be placed in executor working directory. The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;) ");
        contentPane.add(commandLineArgs,
                new GridBagConstraints(0, ++row,
                        1, 1,
                        1, 0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(margin, margin, 0, 0), 0, 0));

        referencedFilesTextField = new JTextField();
        referencedFilesTextField.setToolTipText("Files to be placed in executor working directory. The path needs to be a Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;) ");
        contentPane.add(referencedFilesTextField, new GridBagConstraints(1, row,
                0, 1,
                1, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(margin, margin, 0, margin), 0, 0));
    }

    private void addOperationJPanel() {
        JPanel operationPanel = new JPanel();
        operationPanel.setLayout(new FlowLayout());
        buttonSubmit = new JButton("Submit");
        JButton buttonCancel = new JButton("Cancel");
        JButton buttonHelper = new JButton("Help >>");

        operationPanel.add(buttonSubmit);
        operationPanel.add(buttonCancel);
        operationPanel.add(buttonHelper);

        contentPane.add(operationPanel,
                new GridBagConstraints(1, ++row,
                        0, 1,
                        1, 0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE,
                        new Insets(0, margin, 0, 0), 0, 0));

        buttonSubmit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        buttonHelper.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OnHelper();
            }
        });
    }

    private SparkSubmissionParameter constructSubmissionParameter() {
        IClusterDetail selectedClusterDetail = submitModel.getSelectedClusterDetail();
        Object selectedItem = selectedArtifactComboBox.getSelectedItem();
        String selectedArtifactName = selectedItem == null ? "" : selectedItem.toString();
        
        String className = mainClassTextField.getText().trim();
        String commandLine = commandLineTextField.getText().trim();
        String localArtifactPath = selectedArtifactTextField.getText();
        String selectedClusterName = selectedClusterDetail.getName();

        List<String> referencedFileList = new ArrayList<>();
        for (String singleReferencedFile : referencedFilesTextField.getText().split(";")) {
            singleReferencedFile = singleReferencedFile.trim();
            if (!StringHelper.isNullOrWhiteSpace(singleReferencedFile)) {
                referencedFileList.add(singleReferencedFile);
            }
        }

        List<String> uploadedFilePathList = new ArrayList<>();
        for (String singleReferencedJars : referencedJarsTextField.getText().split(";")) {
            singleReferencedJars = singleReferencedJars.trim();
            if (!StringHelper.isNullOrWhiteSpace(singleReferencedJars)) {
                uploadedFilePathList.add(singleReferencedJars);
            }
        }

        List<String> argsList = new ArrayList<>();
        for (String singleArs : commandLine.split(" ")) {
            if (!StringHelper.isNullOrWhiteSpace(singleArs)) {
                argsList.add(singleArs.trim());
            }
        }

        Map<String, Object> jobConfigMap = submitModel.getJobConfigMap();

        return new SparkSubmissionParameter(selectedClusterName, localArtifactRadioButton.isSelected(),
                selectedArtifactName, localArtifactPath, null, className, referencedFileList, uploadedFilePathList, argsList, jobConfigMap);
    }

    private void onOK() {
        submitModel.action(constructSubmissionParameter());
        dispose();
    }
    //endregion

    private void setSubmitButtonStatus() {
        for (int i = 0; i < errorMessageLabels.length; ++i) {
            if (errorMessageLabels[i].isVisible() && errorMessageLabels[i].getForeground().equals(DarkThemeManager.getInstance().getErrorMessageColor())) {
                this.buttonSubmit.setEnabled(false);
                return;
            }
        }

        this.buttonSubmit.setEnabled(true);
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

        setSubmitButtonStatus();

        this.pack();
    }

    private void setStatusForMessageLabel(@NotNull int label, @NotNull boolean isVisible, @Nullable String message) {
        setStatusForMessageLabel(label, isVisible, message, false);
    }


    private void onCancel() {
        dispose();
    }

    private void OnHelper() {
        try {
            AppInsightsClient.create(HDInsightBundle.message("SparkSubmissionHelpClickEvent"), null);
            Desktop.getDesktop().browse(new URI(SparkSubmitHelper.HELP_LINK));
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showException("An error occurred while attempting to browse link.", e,
                    "HDInsight Spark Submit - Error Browsing Link", false, true);
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        if (this.callBack != null) {
            callBack.run();
        }
    }
}
