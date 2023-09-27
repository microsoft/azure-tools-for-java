/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.serverexplore.ui;

import com.intellij.CommonBundle;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HideableTitledPanel;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.cluster.SparkClusterType;
import com.microsoft.azure.hdinsight.sdk.common.AuthType;
import com.microsoft.azure.hdinsight.sdk.common.AuthTypeOptions;
import com.microsoft.azure.hdinsight.serverexplore.AddNewClusterCtrlProvider;
import com.microsoft.azure.hdinsight.serverexplore.AddNewClusterModel;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azure.hdinsight.spark.ui.ImmutableComboBoxModel;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import com.microsoft.intellij.ui.AccessibleHideableTitledPanel;
import com.microsoft.intellij.ui.ErrorTextArea;
import com.microsoft.intellij.ui.HintTextField;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.FocusManager;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.URI;
import java.util.Arrays;

import static com.intellij.execution.ui.ConsoleViewContentType.LOG_DEBUG_OUTPUT;
import static com.microsoft.azure.hdinsight.serverexplore.AddNewClusterCtrlProvider.getClusterName;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.*;

public class AddNewClusterForm extends DialogWrapper implements SettableControl<AddNewClusterModel> {
    private JPanel wholePanel;
    private JPanel clusterInfoPanel;
    private JPanel clusterPanel;
    private JPanel authenticationPanel;
    private JPanel comboBoxPanel;
    protected JComboBox clusterComboBox;
    protected JPanel clusterCardsPanel;
    private JPanel hdInsightClusterCard;
    protected JTextField clusterNameOrUrlField;
    private JPanel livyServiceCard;
    protected JTextField livyEndpointField;
    protected ErrorTextArea validationErrorMessageField;
    private JPanel authComboBoxPanel;
    protected JComboBox<AuthType> authComboBox;
    protected JPanel authCardsPanel;
    private JPanel basicAuthCard;
    protected JTextField userNameField;
    protected JTextField passwordField;
    private JPanel noAuthCard;
    protected JTextField livyClusterNameField;
    protected HintTextField yarnEndpointField;
    private JLabel clusterNameLabel;
    protected JLabel userNameLabel;
    protected JLabel passwordLabel;
    protected JLabel livyClusterNameLabel;
    protected JTextField arisPortField;
    protected HintTextField arisHostField;
    protected HintTextField arisClusterNameField;
    protected JPanel arisLivyServiceCard;
    private JPanel authErrorDetailsPanelHolder;
    private JPanel authErrorDetailsPanel;
    protected JLabel linkResourceTypeLabel;
    private JPanel azureAccountCard;
    protected HideableTitledPanel authErrorDetailsDecorator;
    protected ConsoleViewImpl consoleViewPanel;
    @NotNull
    private RefreshableNode hdInsightModule;
    @NotNull
    protected AddNewClusterCtrlProvider ctrlProvider;

    private ImmutableComboBoxModel authOpsForHdiCluster = new ImmutableComboBoxModel(AuthTypeOptions.HDICluster.getOptionTypes());
    private ImmutableComboBoxModel authOpsForLivyCluster = new ImmutableComboBoxModel(AuthTypeOptions.LivyCluster.getOptionTypes());

    private static final String HELP_URL = "https://go.microsoft.com/fwlink/?linkid=866472";

    // ConsoleViewImpl requires project to be NotNull
    public AddNewClusterForm(@NotNull final Project project, @Nullable RefreshableNode hdInsightModule) {
        super(project, true);
        this.ctrlProvider = new AddNewClusterCtrlProvider(this, new IdeaSchedulers(project));

        myHelpAction = new HelpAction();

        init();
        this.hdInsightModule = hdInsightModule;

        validationErrorMessageField.setBackground(this.clusterInfoPanel.getBackground());

        this.setTitle("Link A Cluster");

        // Make error message widget hideable
        authErrorDetailsPanel = new JPanel(new BorderLayout());
        authErrorDetailsPanel.setBorder(BorderFactory.createEmptyBorder());

        // Initialize console view panel
        consoleViewPanel = new ConsoleViewImpl(project, false);
        authErrorDetailsPanel.add(consoleViewPanel.getComponent(), BorderLayout.CENTER);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("linkClusterLog",
                new DefaultActionGroup(consoleViewPanel.createConsoleActions()), false);
        authErrorDetailsPanel.add(toolbar.getComponent(), BorderLayout.WEST);

        authErrorDetailsDecorator = new AccessibleHideableTitledPanel(
                "Authentication Error Details:",
                authErrorDetailsPanel,
                true,
                false
        );

        authErrorDetailsPanelHolder.add(authErrorDetailsDecorator);

        authComboBox.setModel(authOpsForHdiCluster);
        authComboBox.setRenderer(
                new SimpleListCellRenderer<AuthType>() {
                    @Override
                    public void customize(JList<? extends AuthType> elems,
                                          AuthType authType,
                                          int i,
                                          boolean b,
                                          boolean b1) {
                        if (authType != null) {
                            setText(authType.getTypeName());
                        }
                    }
                }
        );

        this.setModal(true);

        clusterComboBox.addItemListener(e -> {
            CardLayout layout = (CardLayout) (clusterCardsPanel.getLayout());
            layout.show(clusterCardsPanel, (String) e.getItem());

            // if "HDInsight Cluster" is chose, "No Authentication" should not exist
            if (isHDInsightClusterSelected()) {
                authComboBox.setModel(authOpsForHdiCluster);
            } else {
                authComboBox.setModel(authOpsForLivyCluster);
            }

            authComboBox.setSelectedItem(authComboBox.getSelectedObjects()[0]);

            // since ops for hdi and livy has overlap auth type, sometimes won't trigger authcombox selected change event
            // also need to render the auth card after cluster change
            layout = (CardLayout) (authCardsPanel.getLayout());
            layout.show(authCardsPanel, ((AuthType) authComboBox.getSelectedItem()).getTypeName());
        });

        authComboBox.addItemListener(e -> {
            CardLayout layout = (CardLayout) (authCardsPanel.getLayout());
            layout.show(authCardsPanel, ((AuthType) e.getItem()).getTypeName());
        });

        // field validation check
        Arrays.asList(clusterComboBox, authComboBox).forEach(comp -> comp.addActionListener(event -> validateBasicInputs()));

        Arrays.asList(clusterNameOrUrlField, userNameField, passwordField, livyEndpointField, livyClusterNameField,
                yarnEndpointField, arisHostField, arisPortField, arisClusterNameField).forEach(
                        comp -> comp.getDocument().addDocumentListener(new DocumentAdapter() {
                            @Override
                            protected void textChanged(DocumentEvent e) {
                                validateBasicInputs();
                            }
                        }));

        // load all cluster details to cache for validation check
        loadClusterDetails();

        getOKAction().setEnabled(false);
    }

    protected void printLogLine(@NotNull ConsoleViewContentType logLevel, @NotNull String log) {
        consoleViewPanel.print(DateTime.now().toString() + " " + logLevel.toString().toUpperCase() + " " + log + "\n", logLevel);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return clusterNameOrUrlField;
    }

    // Data -> Components
    @Override
    public void setData(@NotNull AddNewClusterModel data) {
        // clear console view panel before set new error message
        consoleViewPanel.clear();

        if (data.getErrorMessageList() != null && data.getErrorMessageList().size() > 0) {
            if (!authErrorDetailsDecorator.isExpanded()) {
                authErrorDetailsDecorator.setOn(true);
            }

            // If there are error messages, console view will be focused.
            // Refer to issue https://github.com/microsoft/azure-tools-for-java/issues/3635
            if (data.getErrorMessageList().size() > 0) {
                consoleViewPanel.getPreferredFocusableComponent().grabFocus();
            }
            data.getErrorMessageList().forEach(typeAndLogPair -> {
                if (typeAndLogPair.getLeft().equals(data.ERROR_OUTPUT)) {
                    printLogLine(ConsoleViewContentType.ERROR_OUTPUT, typeAndLogPair.getRight());
                } else if (typeAndLogPair.getLeft().equals(data.NORMAL_OUTPUT)) {
                    printLogLine(ConsoleViewContentType.NORMAL_OUTPUT, typeAndLogPair.getRight());
                }
            });
        } else {
            if (authErrorDetailsDecorator.isExpanded()) {
                authErrorDetailsDecorator.setOn(false);
            }
        }
    }

    // Components -> Data
    @Override
    public void getData(@NotNull AddNewClusterModel data) {
        data.setSparkClusterType(getSparkClusterType());

        switch (getSparkClusterType()) {
            case HDINSIGHT_CLUSTER:
                data.setClusterName(clusterNameOrUrlField.getText().trim())
                        .setUserName(userNameField.getText().trim())
                        .setPassword(passwordField.getText().trim())
                        .setAuthType((AuthType) authComboBox.getSelectedItem())
                        // TODO: these label title setting is no use other than to be compatible with legacy ctrlprovider code
                        .setClusterNameLabelTitle(clusterNameLabel.getText())
                        .setUserNameLabelTitle(userNameLabel.getText())
                        .setPasswordLabelTitle(passwordLabel.getText());
                break;
            case LIVY_LINK_CLUSTER:
                data.setLivyEndpoint(URI.create(livyEndpointField.getText().trim()))
                        .setYarnEndpoint(isBlank(yarnEndpointField.getText()) ? null : URI.create(yarnEndpointField.getText().trim()))
                        .setClusterName(livyClusterNameField.getText().trim())
                        // TODO: these label title setting is no use other than to be compatible with legacy ctrlprovider code
                        .setClusterNameLabelTitle(livyClusterNameLabel.getText())
                        .setUserNameLabelTitle(userNameLabel.getText())
                        .setPasswordLabelTitle(passwordLabel.getText());
                if (isBasicAuthSelected()) {
                    data.setUserName(userNameField.getText().trim())
                            .setPassword(passwordField.getText().trim());
                }
                break;
            default:
                break;
        }
    }

    public void afterOkActionPerformed() {
        if (hdInsightModule != null) {
            // TODO: There is a bug for linking cluster action: If the cluster name already exists,
            // the linked cluster will fail to be added to cache. If we don't force refresh again,
            // the cluster will not be shown as linked state in Azure Explorer.
            hdInsightModule.load(true);
        }
    }

    @Override
    protected void doOKAction() {
        if (!getOKAction().isEnabled()) {
            return;
        }

        getOKAction().setEnabled(false);

        ctrlProvider
                .validateAndAdd()
                .doOnEach(notification -> getOKAction().setEnabled(true))
                .subscribe(toUpdate -> {
                    afterOkActionPerformed();
                    AppInsightsClient.create(HDInsightBundle.message("HDInsightAddNewClusterAction"), null);
                    EventUtil.logEvent(EventType.info, TelemetryConstants.HDINSIGHT,
                        HDInsightBundle.message("HDInsightAddNewClusterAction"), null);
                    super.doOKAction();
                });
    }

    protected void createUIComponents() {
        clusterNameOrUrlField = new HintTextField("Example: spk22 or https://spk22.azurehdinsight.net");
        livyEndpointField = new HintTextField("Example: http://headnodehost:8998");
        yarnEndpointField = new HintTextField("(Optional)Example: http://hn0-spark2:8088");
        arisClusterNameField = new HintTextField("(Optional) Cluster name");
        arisHostField = new HintTextField("Example: 10.123.123.123");

        clusterNameOrUrlField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String clusterNameOrUrl = clusterNameOrUrlField.getText();

                if (isBlank(clusterNameOrUrl)) {
                    return;
                }

                Observable.fromCallable(() -> SparkBatchSubmission.getInstance().probeAuthType(
                                                    getClusterConnectionUrl(clusterNameOrUrl.trim())))
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                authType -> {
                                    printLogLine(LOG_DEBUG_OUTPUT,
                                                 format("The cluster %s authentication type is %s",
                                                        clusterNameOrUrl, authType));

                                    authComboBox.getModel().setSelectedItem(authType);
                                },
                                err -> printLogLine(LOG_DEBUG_OUTPUT,
                                                    format("Can't probe cluster %s authentication type with error %s",
                                                           clusterNameOrUrl, err.getMessage())));
            }
        });
    }

    private String getClusterConnectionUrl(String clusterNameOrUrl) {
        return StringUtils.startsWithIgnoreCase(clusterNameOrUrl, "https://") ?
                clusterNameOrUrl :
                ClusterManagerEx.getInstance().getClusterConnectionString(clusterNameOrUrl);
    }

    public SparkClusterType getSparkClusterType() {
        return isHDInsightClusterSelected() ? SparkClusterType.HDINSIGHT_CLUSTER : SparkClusterType.LIVY_LINK_CLUSTER;
    }

    private boolean isHDInsightClusterSelected() {
        return ((String) clusterComboBox.getSelectedItem()).equalsIgnoreCase("HDInsight Cluster");
    }

    protected boolean isBasicAuthSelected() {
        return authComboBox.getSelectedItem() == AuthType.BasicAuth;
    }

    private void validateBasicInputs() {
        final String errorMessage = doInputValidate();
        validationErrorMessageField.setText(errorMessage);

        Component focused = FocusManager.getCurrentManager().getFocusOwner();
        ofNullable(focused)
            .map(Component::getAccessibleContext)
            .ifPresent(ac -> {
                // Use accessible description to inform validation error
                ac.setAccessibleDescription(errorMessage);

                // Register a focus lost event listener to remove validation error
                focused.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        e.getComponent().getAccessibleContext().setAccessibleDescription(null);

                        super.focusLost(e);
                    }
                });
            });

        getOKAction().setEnabled(isEmpty(errorMessage));
    }

    /**
     * Validate Form inputs
     * TODO: there is a doValidate() method can be override
     *
     * @return error messages if validation failed, EMPTY for success.
     */
    protected String doInputValidate() {
        switch (getSparkClusterType()) {
            case HDINSIGHT_CLUSTER:
                if (isBlank(clusterNameOrUrlField.getText())) {
                    return "Cluster name can't be empty";
                } else {
                    String clusterName = getClusterName(clusterNameOrUrlField.getText());
                    if (clusterName == null) {
                        return "Cluster URL is not a valid URL";
                    } else if (ctrlProvider.doesClusterNameExistInLinkedHDInsightClusters(
                            clusterName)) {
                        return "Cluster already exists in linked clusters";
                    }
                }
                break;
            case LIVY_LINK_CLUSTER:
                if (isBlank(livyEndpointField.getText()) ||
                        isBlank(livyClusterNameField.getText())) {
                    return "Livy Endpoint and cluster name can't be empty";
                } else if (!ctrlProvider.isURLValid(livyEndpointField.getText())) {
                    return "Livy Endpoint is not a valid URL";
                } else if (ctrlProvider.doesClusterLivyEndpointExistInAllHDInsightClusters(
                        livyEndpointField.getText())) {
                    return "The same name Livy Endpoint already exists in clusters";
                } else if (ctrlProvider.doesClusterNameExistInAllHDInsightClusters(
                        livyClusterNameField.getText())) {
                    return "Cluster Name already exists in clusters";
                } else if (!isEmpty(yarnEndpointField.getText()) &&
                        !ctrlProvider.isURLValid(yarnEndpointField.getText())) {
                    return "Yarn Endpoint is not a valid URL";
                }
                break;
            default:
                break;
        }

        if (isBasicAuthSelected()) {
            if (isBlank(userNameField.getText()) || isBlank(passwordField.getText())) {
                return "Username and password can't be empty in Basic Authentication";
            }
        }

        return EMPTY;
    }

    private void loadClusterDetails() {
        if (ClusterManagerEx.getInstance().getCachedClusters() == null) {
            ClusterManagerEx.getInstance().getClusterDetails();
        }
    }

    @Override
    protected void dispose() {
        Disposer.dispose(consoleViewPanel);

        super.dispose();
    }

    private class HelpAction extends AbstractAction {
        private HelpAction() {
            this.putValue("Name", CommonBundle.getHelpButtonText());
        }

        public void actionPerformed(ActionEvent e) {
            AddNewClusterForm.this.doHelpAction();
        }
    }

    @Override
    protected void doHelpAction() {
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(HELP_URL);
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[] { getOKAction(), getCancelAction(), getHelpAction() };
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[0];
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return wholePanel;
    }
}
