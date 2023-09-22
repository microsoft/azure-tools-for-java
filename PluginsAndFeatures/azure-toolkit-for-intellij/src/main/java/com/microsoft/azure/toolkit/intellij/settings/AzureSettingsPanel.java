/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.settings;

import com.azure.core.management.AzureEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.common.auth.IdeAzureAccount;
import com.microsoft.azure.toolkit.ide.common.dotnet.DotnetRuntimeHandler;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.intellij.common.settings.IntelliJAzureConfiguration;
import com.microsoft.azure.toolkit.intellij.storage.component.AzuriteWorkspaceComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionCoreToolsCombobox;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.FocusManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor.DOWNLOAD_CORE_TOOLS;
import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.INSTALL_DOTNET_RUNTIME;
import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

@Slf4j
public class AzureSettingsPanel {
    private JPanel contentPane;
    private JCheckBox allowTelemetryCheckBox;
    private JTextPane allowTelemetryComment;
    private JComboBox<AzureEnvironment> azureEnvironmentComboBox;
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
    private JLabel lblDocumentsLabelFields;
    private JLabel lblPageSize;
    private JLabel lblRows;
    private AzureTextInput consumerGroupName;
    private JCheckBox chkLooseMode;
    private AzureFileInput txtAzurite;
    private AzuriteWorkspaceComboBox txtAzuriteWorkspace;

    public void init() {
        Messages.configureMessagePaneUi(allowTelemetryComment, message("settings.root.telemetry.notice"));
        allowTelemetryComment.setForeground(UIUtil.getContextHelpForeground());

        final ComboBoxModel<AzureEnvironment> envModel = new DefaultComboBoxModel<>(Azure.az(AzureCloud.class).list().toArray(new AzureEnvironment[0]));
        azureEnvironmentComboBox.setModel(envModel);
        azureEnvironmentComboBox.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@Nonnull JList list, AzureEnvironment value, int index, boolean selected, boolean hasFocus) {
                setText(String.format("%s - %s", azureEnvironmentToString(value), value.getActiveDirectoryEndpoint()));
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

        this.lblRows.setIcon(AllIcons.General.ContextHelp);
        this.lblPageSize.setIcon(AllIcons.General.ContextHelp);
        this.lblDocumentsLabelFields.setIcon(AllIcons.General.ContextHelp);
        this.bindSettings();
        this.reset();
    }

    private void setSettings(Map<String, String> settings) {
        final List<JComponent> inputs = this.getInputs();
        for (final JComponent input : inputs) {
            final String key = (String) input.getClientProperty("setting.key");
            //noinspection unchecked
            final Consumer<String> setter = (Consumer<String>) input.getClientProperty("setting.set");
            setter.accept(settings.get(key));
        }
    }

    private Map<String, String> getSettings() {
        final IntelliJAzureConfiguration config = ((IntelliJAzureConfiguration) Azure.az().config());
        final HashMap<String, String> settings = new HashMap<>(config.getSettings());
        final List<JComponent> inputs = this.getInputs();
        for (final JComponent input : inputs) {
            final String key = (String) input.getClientProperty("setting.key");
            //noinspection unchecked
            final Supplier<String> getter = (Supplier<String>) input.getClientProperty("setting.get");
            settings.put(key, getter.get());
        }
        return settings;
    }

    public boolean doOKAction() {
        if (IdeAzureAccount.getInstance().isLoggedIn()) {
            final AzureEnvironment currentEnv = Azure.az(AzureCloud.class).getOrDefault();
            if (!Objects.equals(currentEnv, azureEnvironmentComboBox.getSelectedItem())) {
                Azure.az(AzureAccount.class).logout();
            }
        }
        final Map<String, String> settings = this.getSettings();
        final AzureConfiguration config = Azure.az().config();
        config.setSettings(settings);
        if (!StringUtils.equalsIgnoreCase((CharSequence) settings.get("bicep.dotnet_runtime_path"), config.getDotnetRuntimePath())) {
            AzureEventBus.emit("dotnet_runtime.updated");
        }
        if (StringUtils.isNotBlank(config.getCloud())) {
            Azure.az(AzureCloud.class).setByName(config.getCloud());
        }
        return true;
    }

    public boolean isModified() {
        final IntelliJAzureConfiguration config = ((IntelliJAzureConfiguration) Azure.az().config());
        return !config.getSettings().equals(this.getSettings());
    }

    public void reset() {
        final IntelliJAzureConfiguration config = ((IntelliJAzureConfiguration) Azure.az().config());
        setSettings(config.getSettings());
    }

    private void bindSettings() {
        azureEnvironmentComboBox.putClientProperty("setting.key", "account.azure_environment");
        azureEnvironmentComboBox.putClientProperty("setting.set", (Consumer<String>) (String v) -> azureEnvironmentComboBox.setSelectedItem(ObjectUtils.firstNonNull(AzureEnvironmentUtils.stringToAzureEnvironment(v), AzureEnvironment.AZURE)));
        azureEnvironmentComboBox.putClientProperty("setting.get", (Supplier<String>) () -> AzureEnvironmentUtils.azureEnvironmentToString((AzureEnvironment) azureEnvironmentComboBox.getSelectedItem()));
        funcCoreToolsPath.putClientProperty("setting.key", "function.function_core_tools_path");
        funcCoreToolsPath.putClientProperty("setting.set", (Consumer<String>) (String v) -> funcCoreToolsPath.setSelectedItem(v));
        funcCoreToolsPath.putClientProperty("setting.get", (Supplier<String>) () -> Objects.nonNull(funcCoreToolsPath.getSelectedItem()) ? (String) funcCoreToolsPath.getSelectedItem() : (String) funcCoreToolsPath.getRawValue());
        txtStorageExplorer.putClientProperty("setting.key", "storage.storage_explorer_path");
        txtStorageExplorer.putClientProperty("setting.set", (Consumer<String>) (String v) -> txtStorageExplorer.setValue(v));
        txtStorageExplorer.putClientProperty("setting.get", (Supplier<String>) () -> txtStorageExplorer.getValue());
        dotnetRuntimePath.putClientProperty("setting.key", "bicep.dotnet_runtime_path");
        dotnetRuntimePath.putClientProperty("setting.set", (Consumer<String>) (String v) -> dotnetRuntimePath.setValue(v));
        dotnetRuntimePath.putClientProperty("setting.get", (Supplier<String>) () -> dotnetRuntimePath.getValue());
        txtAzuriteWorkspace.putClientProperty("setting.key", "azurite.azurite_workspace");
        txtAzuriteWorkspace.putClientProperty("setting.set", (Consumer<String>) (String v) -> txtAzuriteWorkspace.setValue(v));
        txtAzuriteWorkspace.putClientProperty("setting.get", (Supplier<String>) () -> AzureEnvironmentUtils.azureEnvironmentToString((AzureEnvironment) txtAzuriteWorkspace.getSelectedItem()));
        txtAzurite.putClientProperty("setting.key", "azurite.azurite_path");
        txtAzurite.putClientProperty("setting.set", (Consumer<String>) (String v) -> txtAzurite.setValue(v));
        txtAzurite.putClientProperty("setting.get", (Supplier<String>) () -> txtAzurite.getValue());
        txtLabelFields.putClientProperty("setting.key", "cosmos.documents_label_fields");
        txtLabelFields.putClientProperty("setting.set", (Consumer<String>) (String v) -> txtLabelFields.setValue(v));
        txtLabelFields.putClientProperty("setting.get", (Supplier<String>) () -> txtLabelFields.getValue());
        consumerGroupName.putClientProperty("setting.key", "event_hubs.consumer_group_name");
        consumerGroupName.putClientProperty("setting.set", (Consumer<String>) (String v) -> consumerGroupName.setValue(v));
        consumerGroupName.putClientProperty("setting.get", (Supplier<String>) () -> consumerGroupName.getValue());
        allowTelemetryCheckBox.putClientProperty("setting.key", "telemetry.telemetry_allow_telemetry");
        allowTelemetryCheckBox.putClientProperty("setting.set", (Consumer<String>) (String v) -> allowTelemetryCheckBox.setSelected(toBoolean(v, true)));
        allowTelemetryCheckBox.putClientProperty("setting.get", (Supplier<String>) () -> String.valueOf(allowTelemetryCheckBox.isSelected()));
        enableAuthPersistence.putClientProperty("setting.key", "other.enable_auth_persistence");
        enableAuthPersistence.putClientProperty("setting.set", (Consumer<String>) (String v) -> enableAuthPersistence.setSelected(toBoolean(v, true)));
        enableAuthPersistence.putClientProperty("setting.get", (Supplier<String>) () -> String.valueOf(enableAuthPersistence.isSelected()));
        chkLooseMode.putClientProperty("setting.key", "azurite.enable_lease_mode");
        chkLooseMode.putClientProperty("setting.set", (Consumer<String>) (String v) -> chkLooseMode.setSelected(toBoolean(v, false)));
        chkLooseMode.putClientProperty("setting.get", (Supplier<String>) () -> String.valueOf(chkLooseMode.isSelected()));
        txtPageSize.putClientProperty("setting.key", "common.page_size");
        txtPageSize.putClientProperty("setting.set", (Consumer<String>) (String v) -> txtPageSize.setNumber(toInteger(v, 99)));
        txtPageSize.putClientProperty("setting.get", (Supplier<String>) () -> String.valueOf(txtPageSize.getNumber()));
        queryRowNumber.putClientProperty("setting.key", "monitor.monitor_table_rows");
        queryRowNumber.putClientProperty("setting.set", (Consumer<String>) (String v) -> queryRowNumber.setValue(toInteger(v, 200)));
        queryRowNumber.putClientProperty("setting.get", (Supplier<String>) () -> String.valueOf(queryRowNumber.getNumber()));
    }

    public List<JComponent> getInputs() {
        return Arrays.asList(
            azureEnvironmentComboBox,
            funcCoreToolsPath,
            txtStorageExplorer,
            dotnetRuntimePath,
            txtAzuriteWorkspace,
            txtAzurite,
            txtLabelFields,
            consumerGroupName,
            allowTelemetryCheckBox,
            enableAuthPersistence,
            chkLooseMode,
            txtPageSize,
            queryRowNumber
        );
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
        this.txtAzurite = new AzureFileInput();
        this.txtAzurite.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>("Select Path of Azurite", null, txtAzurite,
            null, FileChooserDescriptorFactory.createSingleLocalFileDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
        this.txtAzuriteWorkspace = new AzuriteWorkspaceComboBox();
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

    private void displayDescriptionForAzureEnv() {
        if (IdeAzureAccount.getInstance().isLoggedIn()) {
            final AzureEnvironment currentEnv = Optional.ofNullable(AzureEnvironmentUtils.stringToAzureEnvironment(Azure.az().config().getCloud())).orElse(AzureEnvironment.AZURE);
            final String currentEnvStr = azureEnvironmentToString(currentEnv);
            if (Objects.equals(currentEnv, azureEnvironmentComboBox.getSelectedItem())) {
                azureEnvDesc.setText("<html>You are currently signed in with environment: " + currentEnvStr + "</html>");
                azureEnvDesc.setIcon(AllIcons.General.Information);
            } else {
                azureEnvDesc.setText(String.format("<html>You are currently signed in to environment: %s, your change will sign out your account.</html>", currentEnvStr));
                azureEnvDesc.setIcon(AllIcons.General.Warning);
            }
        } else {
            azureEnvDesc.setText("<html>You are currently not signed in, the environment will be applied when you sign in next time.</html>");
            azureEnvDesc.setIcon(AllIcons.General.Warning);
        }
    }

    private static String azureEnvironmentToString(@Nonnull AzureEnvironment env) {
        return StringUtils.removeEnd(AzureEnvironmentUtils.getCloudName(env), "Cloud");
    }

    public JComponent getPanel() {
        final JBScrollPane pane = new JBScrollPane(contentPane);
        pane.setBorder(JBUI.Borders.empty());
        return pane;
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

    private static boolean toBoolean(Object v, boolean defaultVal) {
        if (v instanceof Boolean) {
            return (Boolean) v;
        } else if (v instanceof String) {
            return Boolean.parseBoolean((String) v);
        } else {
            return defaultVal;
        }
    }

    private static int toInteger(Object v, int defaultVal) {
        if (v instanceof Integer) {
            return (Integer) v;
        } else if (v instanceof String) {
            return Integer.parseInt((String) v);
        } else {
            return defaultVal;
        }
    }
}
