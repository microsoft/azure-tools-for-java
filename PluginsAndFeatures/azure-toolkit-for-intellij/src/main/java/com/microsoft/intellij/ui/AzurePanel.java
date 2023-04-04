/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.azure.core.management.AzureEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.common.dotnet.DotnetRuntimeHandler;
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionCoreToolsCombobox;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.AzurePlugin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.FocusManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor.DOWNLOAD_CORE_TOOLS;
import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.INSTALL_DOTNET_RUNTIME;
import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SIGNOUT;

public class AzurePanel implements AzureAbstractConfigurablePanel {
    private static final String DISPLAY_NAME = "Azure";
    private JPanel contentPane;
    private JCheckBox allowTelemetryCheckBox;
    private JTextPane allowTelemetryComment;
    private JComboBox<AzureEnvironment> azureEnvironmentComboBox;
    private JComboBox<Password.SaveType> savePasswordComboBox;
    private FunctionCoreToolsCombobox funcCoreToolsPath;
    private JLabel azureEnvDesc;
    private AzureFileInput txtStorageExplorer;
    private JBIntSpinner txtPageSize;
    private AzureTextInput txtLabelFields;
    private ActionLink installFuncCoreToolsAction;
    private AzureFileInput dotnetRuntimePath;
    private ActionLink installDotnetRuntime;
    private JBIntSpinner queryRowNumber;
    private JCheckBox enableAuthPersistence;
    private AzureTextInput consumerGroupName;

    private AzureConfiguration originalConfig;

    @Override
    public void init() {
        if (AzurePlugin.IS_ANDROID_STUDIO) {
            return;
        }
        Messages.configureMessagePaneUi(allowTelemetryComment, message("settings.root.telemetry.notice"));
        allowTelemetryComment.setForeground(UIUtil.getContextHelpForeground());
        final ComboBoxModel<AzureEnvironment> envModel = new DefaultComboBoxModel<>(Azure.az(AzureCloud.class).list().toArray(new AzureEnvironment[0]));
        azureEnvironmentComboBox.setModel(envModel);
        azureEnvironmentComboBox.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@Nonnull JList list, AzureEnvironment value, int index, boolean selected, boolean hasFocus) {
                setText(azureEnvironmentDisplayString(value));
            }
        });
        final ComboBoxModel<Password.SaveType> saveTypeModel = new DefaultComboBoxModel<>(Password.SaveType.values());
        savePasswordComboBox.setModel(saveTypeModel);
        savePasswordComboBox.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@Nonnull JList<? extends Password.SaveType> list, Password.SaveType value, int index, boolean selected, boolean hasFocus) {
                setText(value.title());
            }
        });

        azureEnvDesc.setForeground(UIUtil.getContextHelpForeground());
        azureEnvDesc.setMaximumSize(new Dimension());
        azureEnvironmentComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                displayDescriptionForAzureEnv();
            }
        });

        displayDescriptionForAzureEnv();

        final AzureConfiguration config = Azure.az().config();
        if (Objects.isNull(config.getDatabasePasswordSaveType())) {
            config.setDatabasePasswordSaveType(Password.SaveType.UNTIL_RESTART.name());
        }
        setData(config);
    }

    public void setData(AzureConfiguration config) {
        this.originalConfig = config;
        final AzureEnvironment oldEnv = ObjectUtils.firstNonNull(AzureEnvironmentUtils.stringToAzureEnvironment(config.getCloud()), AzureEnvironment.AZURE);
        final String oldPasswordSaveType = config.getDatabasePasswordSaveType();
        final boolean oldTelemetryEnabled = BooleanUtils.isNotFalse(config.getTelemetryEnabled());
        final boolean oldEnableAuthPersistence = config.isAuthPersistenceEnabled();
        final String oldFuncCoreToolsPath = config.getFunctionCoreToolsPath();
        azureEnvironmentComboBox.setSelectedItem(oldEnv);
        savePasswordComboBox.setSelectedItem(Optional.ofNullable(oldPasswordSaveType).map(Password.SaveType::valueOf).orElse(Password.SaveType.UNTIL_RESTART));
        if (StringUtils.isNotBlank(oldFuncCoreToolsPath)) {
            funcCoreToolsPath.setValue(oldFuncCoreToolsPath);
        }
        if (StringUtils.isNotBlank(config.getStorageExplorerPath())) {
            txtStorageExplorer.setValue(config.getStorageExplorerPath());
        }
        if (StringUtils.isNotBlank(config.getDotnetRuntimePath())) {
            dotnetRuntimePath.setValue(config.getDotnetRuntimePath());
        }
        allowTelemetryCheckBox.setSelected(oldTelemetryEnabled);
        enableAuthPersistence.setSelected(oldEnableAuthPersistence);
        txtPageSize.setNumber(config.getPageSize());
        queryRowNumber.setValue(config.getMonitorQueryRowNumber());
        txtLabelFields.setValue(String.join(";", config.getDocumentsLabelFields()));
        consumerGroupName.setValue(config.getEventHubsConsumerGroup());
    }

    public AzureConfiguration getData() {
        final AzureConfiguration data = new AzureConfiguration();
        data.setCloud(AzureEnvironmentUtils.azureEnvironmentToString((AzureEnvironment) azureEnvironmentComboBox.getSelectedItem()));
        data.setDatabasePasswordSaveType(Optional.ofNullable(savePasswordComboBox.getSelectedItem())
            .map(i -> ((Password.SaveType) i).name())
            .orElse(Password.SaveType.UNTIL_RESTART.name()));
        data.setTelemetryEnabled(allowTelemetryCheckBox.isSelected());
        data.setAuthPersistenceEnabled(enableAuthPersistence.isSelected());
        if (Objects.nonNull(funcCoreToolsPath.getSelectedItem())) {
            data.setFunctionCoreToolsPath((String) funcCoreToolsPath.getSelectedItem());
        } else if (funcCoreToolsPath.getRawValue() instanceof String) {
            data.setFunctionCoreToolsPath((String) funcCoreToolsPath.getRawValue());
        }
        if (StringUtils.isNotBlank(txtStorageExplorer.getValue())) {
            data.setStorageExplorerPath(txtStorageExplorer.getValue());
        }
        if (Objects.nonNull(txtPageSize.getValue())) {
            data.setPageSize(txtPageSize.getNumber());
        }
        if (Objects.nonNull(queryRowNumber.getValue())) {
            data.setMonitorQueryRowNumber(queryRowNumber.getNumber());
        }
        if (StringUtils.isNotEmpty(txtLabelFields.getValue())) {
            final List<String> fields = Arrays.stream(txtLabelFields.getValue().split(";")).collect(Collectors.toList());
            data.setDocumentsLabelFields(fields);
        }
        if (StringUtils.isNoneBlank(dotnetRuntimePath.getValue())) {
            data.setDotnetRuntimePath(dotnetRuntimePath.getValue());
        }
        if (StringUtils.isNotBlank(consumerGroupName.getValue())) {
            data.setEventHubsConsumerGroup(consumerGroupName.getValue());
        }
        return data;
    }

    private void displayDescriptionForAzureEnv() {
        if (IdeAzureAccount.getInstance().isLoggedIn()) {
            final AzureEnvironment currentEnv = Azure.az(AzureCloud.class).getOrDefault();
            final String currentEnvStr = azureEnvironmentToString(currentEnv);
            if (Objects.equals(currentEnv, azureEnvironmentComboBox.getSelectedItem())) {
                setTextToLabel(azureEnvDesc, "You are currently signed in with environment: " + currentEnvStr);
                azureEnvDesc.setIcon(AllIcons.General.Information);
            } else {
                setTextToLabel(azureEnvDesc,
                    String.format("You are currently signed in to environment: %s, your change will sign out your account.", currentEnvStr));
                azureEnvDesc.setIcon(AllIcons.General.Warning);
            }
        } else {
            setTextToLabel(azureEnvDesc, "You are currently not signed in, the environment will be applied when you sign in next time.");
            azureEnvDesc.setIcon(AllIcons.General.Warning);
        }
    }

    private static void setTextToLabel(@Nonnull JLabel label, @Nonnull String text) {
        label.setText("<html>" + text + "</html>");
    }

    private static String azureEnvironmentDisplayString(@Nonnull AzureEnvironment env) {
        return String.format("%s - %s", azureEnvironmentToString(env), env.getActiveDirectoryEndpoint());
    }

    private static String azureEnvironmentToString(@Nonnull AzureEnvironment env) {
        final String name = AzureEnvironmentUtils.getCloudName(env);
        return StringUtils.removeEnd(name, "Cloud");
    }

    @Override
    public JComponent getPanel() {
        final JBScrollPane pane = new JBScrollPane(contentPane);
        pane.setBorder(JBUI.Borders.empty());
        return pane;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean doOKAction() {
        if (IdeAzureAccount.getInstance().isLoggedIn()) {
            final AzureEnvironment currentEnv = Azure.az(AzureCloud.class).getOrDefault();
            if (!Objects.equals(currentEnv, azureEnvironmentComboBox.getSelectedItem())) {
                EventUtil.executeWithLog(ACCOUNT, SIGNOUT, (operation) -> {
                    Azure.az(AzureAccount.class).logout();
                });
            }
        }
        final AzureConfiguration newConfig = getData();
        if (!StringUtils.equalsIgnoreCase(this.originalConfig.getDotnetRuntimePath(), newConfig.getDotnetRuntimePath())) {
            AzureEventBus.emit("dotnet_runtime.updated");
        }
        // set partial config to global config
        this.originalConfig.setCloud(newConfig.getCloud());
        this.originalConfig.setTelemetryEnabled(newConfig.getTelemetryEnabled());
        this.originalConfig.setAuthPersistenceEnabled(newConfig.isAuthPersistenceEnabled());
        this.originalConfig.setDatabasePasswordSaveType(newConfig.getDatabasePasswordSaveType());
        this.originalConfig.setFunctionCoreToolsPath(newConfig.getFunctionCoreToolsPath());
        final String userAgent = String.format(AzurePlugin.USER_AGENT, AzurePlugin.PLUGIN_VERSION,
            this.originalConfig.getTelemetryEnabled() ? this.originalConfig.getMachineId() : StringUtils.EMPTY);
        this.originalConfig.setUserAgent(userAgent);
        this.originalConfig.setStorageExplorerPath(newConfig.getStorageExplorerPath());
        this.originalConfig.setPageSize(newConfig.getPageSize());
        this.originalConfig.setDocumentsLabelFields(newConfig.getDocumentsLabelFields());
        this.originalConfig.setDotnetRuntimePath(newConfig.getDotnetRuntimePath());
        this.originalConfig.setMonitorQueryRowNumber(newConfig.getMonitorQueryRowNumber());
        this.originalConfig.setEventHubsConsumerGroup(newConfig.getEventHubsConsumerGroup());
        CommonSettings.setUserAgent(newConfig.getUserAgent());

        if (StringUtils.isNotBlank(newConfig.getCloud())) {
            Azure.az(AzureCloud.class).setByName(newConfig.getCloud());
        }
        AzureConfigInitializer.saveAzConfig();
        return true;
    }

    @Nullable
    @Override
    public String getSelectedValue() {
        return null;
    }

    @Nullable
    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public boolean isModified() {
        if (originalConfig == null) {
            return false;
        }
        final AzureConfiguration newConfig = getData();
        final AzureEnvironment newEnv = AzureEnvironmentUtils.stringToAzureEnvironment(newConfig.getCloud());
        final AzureEnvironment oldEnv = AzureEnvironmentUtils.stringToAzureEnvironment(originalConfig.getCloud());
        return !Objects.equals(newEnv, oldEnv) ||
            !StringUtils.equalsIgnoreCase(newConfig.getDatabasePasswordSaveType(), originalConfig.getDatabasePasswordSaveType()) ||
            !StringUtils.equalsIgnoreCase(newConfig.getFunctionCoreToolsPath(), originalConfig.getFunctionCoreToolsPath()) ||
            !StringUtils.equalsIgnoreCase(newConfig.getStorageExplorerPath(), originalConfig.getStorageExplorerPath()) ||
            !StringUtils.equalsIgnoreCase(newConfig.getDotnetRuntimePath(), originalConfig.getDotnetRuntimePath()) ||
            !Objects.equals(newConfig.getTelemetryEnabled(), originalConfig.getTelemetryEnabled()) ||
            !Objects.equals(newConfig.isAuthPersistenceEnabled(), originalConfig.isAuthPersistenceEnabled()) ||
            !Objects.equals(newConfig.getPageSize(), originalConfig.getPageSize()) ||
            !Objects.equals(newConfig.getMonitorQueryRowNumber(), originalConfig.getMonitorQueryRowNumber()) ||
            !Objects.equals(newConfig.getEventHubsConsumerGroup(), originalConfig.getEventHubsConsumerGroup()) ||
            !Objects.equals(newConfig.getDocumentsLabelFields(), originalConfig.getDocumentsLabelFields());
    }

    @Override
    public void reset() {
        setData(originalConfig);
    }

    private void createUIComponents() {
        this.txtPageSize = new JBIntSpinner(99, 1, 999);
        this.queryRowNumber = new JBIntSpinner(200, 1, 5000);
        this.funcCoreToolsPath = new FunctionCoreToolsCombobox(null, false);
        this.funcCoreToolsPath.setPrototypeDisplayValue(StringUtils.EMPTY);
        this.txtStorageExplorer = new AzureFileInput();
        this.txtStorageExplorer.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>("Select Path of Azure Storage Explorer", null, txtStorageExplorer,
            null, FileChooserDescriptorFactory.createSingleLocalFileDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
        this.txtStorageExplorer.addValidator(this::validateStorageExplorerPath);
        this.dotnetRuntimePath = new AzureFileInput();
        // noinspection DialogTitleCapitalization
        this.dotnetRuntimePath.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>("Select Path of .NET Runtime", null, dotnetRuntimePath,
            null, FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
        this.dotnetRuntimePath.addValidator(this::validateDotnetRuntime);
        this.installFuncCoreToolsAction = new ActionLink("Install the latest version", e -> {
            FocusManager.getCurrentManager().getActiveWindow().dispose();
            AzureActionManager.getInstance().getAction(DOWNLOAD_CORE_TOOLS).handle(null);
        });

        this.installDotnetRuntime = new ActionLink("Install .NET runtime", e -> {
            FocusManager.getCurrentManager().getActiveWindow().dispose();
            AzureActionManager.getInstance().getAction(INSTALL_DOTNET_RUNTIME).handle(null);
        });
    }

    public AzureValidationInfo validateStorageExplorerPath() {
        final String path = txtStorageExplorer.getValue();
        if (StringUtils.isEmpty(path)) {
            return AzureValidationInfo.ok(txtStorageExplorer);
        }
        if (!FileUtil.exists(path)) {
            return AzureValidationInfo.error("Target file does not exist", txtStorageExplorer);
        }
        final String fileName = FilenameUtils.getName(path);
        if (!(StringUtils.containsIgnoreCase(fileName, "storage") && StringUtils.containsIgnoreCase(fileName, "explorer"))) {
            return AzureValidationInfo.error("Please select correct path for storage explorer", txtStorageExplorer);
        }
        return AzureValidationInfo.ok(txtStorageExplorer);
    }

    private AzureValidationInfo validateDotnetRuntime() {
        final String path = dotnetRuntimePath.getValue();
        if (StringUtils.isEmpty(path)) {
            return AzureValidationInfo.ok(dotnetRuntimePath);
        }
        if (!FileUtil.exists(path)) {
            return AzureValidationInfo.error("Target directory does not exist", dotnetRuntimePath);
        }
        if (!FileUtils.isDirectory(new File(path))) {
            return AzureValidationInfo.error(".NET runtime path should be a directory", dotnetRuntimePath);
        }
        if (!DotnetRuntimeHandler.isDotnetRuntimeInstalled(path)) {
            return AzureValidationInfo.error("invalid .NET runtime path", dotnetRuntimePath);
        }
        // todo: make sure dotnet exists in current folder
        return AzureValidationInfo.ok(dotnetRuntimePath);
    }
}
