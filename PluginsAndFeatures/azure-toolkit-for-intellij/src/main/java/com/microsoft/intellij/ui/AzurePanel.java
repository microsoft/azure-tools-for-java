/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;


import com.azure.core.management.AzureEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.database.component.PasswordSaveComboBox;
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionCliResolver;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.azure.toolkit.intellij.common.settings.AzureConfigurations;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
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
    private TextFieldWithBrowseButton funcCoreToolsPath;
    private JLabel azureEnvDesc;

    public AzurePanel() {
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
        funcCoreToolsPath.addBrowseFolderListener(null, "Path to Azure Functions Core Tools", null, FileChooserDescriptorFactory.createSingleFileDescriptor(),
            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        final AzureConfigurations.AzureConfigurationData state = AzureConfigurations.getInstance().getState();
        if (StringUtils.isBlank(state.functionCoreToolsPath())) {
            try {
                funcCoreToolsPath.setText(FunctionCliResolver.resolveFunc());
            } catch (final IOException | InterruptedException ex) {
                //ignore
            }
        } else {
            funcCoreToolsPath.setText(state.functionCoreToolsPath());
        }

        allowTelemetryCheckBox.setSelected(state.allowTelemetry());

        azureEnvironmentComboBox.setSelectedItem(ObjectUtils.firstNonNull(AzureEnvironmentUtils.stringToAzureEnvironment(state.environment())
            , AzureEnvironment.AZURE
        ));

        savePasswordComboBox.setValue(Arrays.stream(Password.SaveType.values())
            .filter(e -> StringUtils.equals(e.name(), AzureConfigurations.getInstance().passwordSaveType())).findAny()
            .orElse(Password.SaveType.UNTIL_RESTART));
    }

    private void displayDescriptionForAzureEnv() {
        final String azureEnv = AuthMethodManager.getInstance().getAuthMethodDetails().getAzureEnv();
        if (AuthMethodManager.getInstance().isSignedIn()) {

            final AzureEnvironment currentEnv =
                AzureEnvironmentUtils.stringToAzureEnvironment(azureEnv);
            String currentEnvStr = azureEnvironmentToString(currentEnv);
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
        final AzureConfigurations.AzureConfigurationData config = AzureConfigurations.getInstance().getState();
        config.allowTelemetry(allowTelemetryCheckBox.isSelected());
        config.functionCoreToolsPath(this.funcCoreToolsPath.getText());
        config.environment(AzureEnvironmentUtils.azureEnvironmentToString((AzureEnvironment) azureEnvironmentComboBox.getSelectedItem()));
        config.passwordSaveType(savePasswordComboBox.getValue().name());
        AzureConfigurations.getInstance().loadState(config);

        final String userAgent = String.format(AzurePlugin.USER_AGENT, AzurePlugin.PLUGIN_VERSION,
            config.allowTelemetry() ? config.installationId() : StringUtils.EMPTY);
        Azure.az().config().setUserAgent(userAgent);
        CommonSettings.setUserAgent(userAgent);

        if (AuthMethodManager.getInstance().isSignedIn()) {
            AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
            final String azureEnv = AuthMethodManager.getInstance().getAuthMethodDetails().getAzureEnv();
            final AzureEnvironment currentEnv = AzureEnvironmentUtils.stringToAzureEnvironment(azureEnv);
            if (!Objects.equals(currentEnv, azureEnvironmentComboBox.getSelectedItem())) {
                EventUtil.executeWithLog(ACCOUNT, SIGNOUT, (operation) -> {
                    authMethodManager.signOut();
                });
            }
        }

        Azure.az(AzureCloud.class).set(AzureEnvironmentUtils.stringToAzureEnvironment(config.environment()));
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
        return true;
    }

    @Override
    public void reset() {
    }
}
