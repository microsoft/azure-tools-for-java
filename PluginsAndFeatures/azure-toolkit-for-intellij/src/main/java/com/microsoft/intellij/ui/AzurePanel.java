/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;


import com.azure.core.management.AzureEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.database.component.PasswordSaveComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.function.FunctionCoreToolsCombobox;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.AzurePlugin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Objects;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SIGNOUT;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;


@Slf4j
public class AzurePanel implements AzureAbstractConfigurablePanel {
    private static final String DISPLAY_NAME = "Azure";
    private JPanel contentPane;
    private JCheckBox allowTelemetryCheckBox;
    private JTextPane allowTelemetryComment;
    private JComboBox<AzureEnvironment> azureEnvironmentComboBox;
    private PasswordSaveComboBox savePasswordComboBox;
    private FunctionCoreToolsCombobox funcCoreToolsPath;
    private JLabel azureEnvDesc;

    private AzureConfiguration originalData;
    private Project project;

    public AzurePanel(Project project) {
        this.project = project;
    }

    @Override
    public void init() {
        if (AzurePlugin.IS_ANDROID_STUDIO) {
            return;
        }
        Messages.configureMessagePaneUi(allowTelemetryComment, message("settings.root.telemetry.notice"));
        allowTelemetryComment.setForeground(UIUtil.getContextHelpForeground());
        final ComboBoxModel<AzureEnvironment> model = new DefaultComboBoxModel<>(Azure.az(AzureCloud.class).list().toArray(new AzureEnvironment[0]));
        azureEnvironmentComboBox.setModel(model);
        azureEnvironmentComboBox.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@NotNull JList list, AzureEnvironment value, int index, boolean selected, boolean hasFocus) {
                    setText(azureEnvironmentDisplayString(value));
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

        setData(Azure.az().config());
    }

    public void setData(AzureConfiguration config) {
        if (StringUtils.isNotBlank(config.getFunctionCoreToolsPath())) {
            //ignore
            funcCoreToolsPath.setValue(config.getFunctionCoreToolsPath());
        }

        savePasswordComboBox.setValue(Arrays.stream(Password.SaveType.values())
            .filter(e -> StringUtils.equals(e.name(), config.getDatabasePasswordSaveType())).findAny()
            .orElse(Password.SaveType.UNTIL_RESTART));
        allowTelemetryCheckBox.setSelected(config.getTelemetryEnabled());

        azureEnvironmentComboBox.setSelectedItem(ObjectUtils.firstNonNull(AzureEnvironmentUtils.stringToAzureEnvironment(config.getCloud()),
            AzureEnvironment.AZURE
        ));
        this.originalData = getData();
    }

    public AzureConfiguration getData() {
        final AzureConfiguration data = new AzureConfiguration();
        data.setCloud(AzureEnvironmentUtils.azureEnvironmentToString((AzureEnvironment) azureEnvironmentComboBox.getSelectedItem()));
        if (savePasswordComboBox.getValue() != null) {
            data.setDatabasePasswordSaveType(savePasswordComboBox.getValue().name());
        }
        data.setTelemetryEnabled(allowTelemetryCheckBox.isSelected());
        data.setFunctionCoreToolsPath(funcCoreToolsPath.getItem());
        return data;
    }

    private void displayDescriptionForAzureEnv() {
        if (AuthMethodManager.getInstance().isSignedIn()) {
            final String azureEnv = AuthMethodManager.getInstance().getAuthMethodDetails().getAzureEnv();
            final AzureEnvironment currentEnv =
                AzureEnvironmentUtils.stringToAzureEnvironment(azureEnv);
            final String currentEnvStr = azureEnvironmentToString(currentEnv);
            if (Objects.equals(currentEnv, azureEnvironmentComboBox.getSelectedItem())) {
                setTextToLabel(azureEnvDesc, "You are currently signed in with environment: " + currentEnvStr);
                azureEnvDesc.setIcon(AllIcons.General.Information);
            } else {
                setTextToLabel(azureEnvDesc,
                    String.format("You are currently signed in with environment: %s, your change will sign out your account.", currentEnvStr));
                azureEnvDesc.setIcon(AllIcons.General.Warning);
            }
        } else {
            setTextToLabel(azureEnvDesc, "You are currently not signed, the environment will be applied when you sign in next time.");
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
        final String name = AzureEnvironmentUtils.getCloudNameForAzureCli(env);
        return StringUtils.removeEnd(name, "Cloud");
    }

    @Override
    public JComponent getPanel() {
        return contentPane;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean doOKAction() {
        final AzureConfiguration data = getData();
        // set partial config to global config
        final AzureConfiguration config = Azure.az().config();
        config.setCloud(data.getCloud());
        config.setTelemetryEnabled(data.getTelemetryEnabled());
        config.setDatabasePasswordSaveType(data.getDatabasePasswordSaveType());
        config.setFunctionCoreToolsPath(data.getFunctionCoreToolsPath());
        final String userAgent = String.format(AzurePlugin.USER_AGENT, AzurePlugin.PLUGIN_VERSION,
            config.getTelemetryEnabled() ? config.getMachineId() : StringUtils.EMPTY);
        config.setUserAgent(userAgent);
        CommonSettings.setUserAgent(config.getUserAgent());
        // apply changes

        // we need to get rid of AuthMethodManager, using az.azure_account
        if (AuthMethodManager.getInstance().isSignedIn()) {
            final AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
            final String azureEnv = authMethodManager.getAuthMethodDetails().getAzureEnv();
            final AzureEnvironment currentEnv = AzureEnvironmentUtils.stringToAzureEnvironment(azureEnv);
            if (!Objects.equals(currentEnv, azureEnvironmentComboBox.getSelectedItem())) {
                EventUtil.executeWithLog(ACCOUNT, SIGNOUT, (operation) -> {
                    authMethodManager.signOut();
                });
            }
        }

        if (StringUtils.isNotBlank(config.getCloud())) {
            Azure.az(AzureCloud.class).setByName(config.getCloud());
        }
        AzureConfigInitializer.saveAzConfig();
        return true;
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public boolean isModified() {
        if (originalData == null) {
            return false;
        }

        final AzureConfiguration data = getData();

        if (!StringUtils.equalsIgnoreCase(data.getCloud(), originalData.getCloud())) {
            return true;
        }

        if (!StringUtils.equalsIgnoreCase(data.getDatabasePasswordSaveType(), originalData.getDatabasePasswordSaveType())) {
            return true;
        }

        if (!StringUtils.equalsIgnoreCase(data.getFunctionCoreToolsPath(), originalData.getFunctionCoreToolsPath())) {
            return true;
        }

        return !Objects.equals(data.getTelemetryEnabled(), data.getTelemetryEnabled());
    }

    @Override
    public void reset() {
        setData(originalData);
    }

    private void createUIComponents() {
        this.funcCoreToolsPath = new FunctionCoreToolsCombobox(project, false);
    }
}
