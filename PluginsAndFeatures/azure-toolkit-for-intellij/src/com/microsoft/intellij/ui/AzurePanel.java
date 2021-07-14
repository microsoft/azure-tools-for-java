/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;


import com.azure.core.management.AzureEnvironment;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionCliResolver;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.configuration.AzureConfigurations;
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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.io.IOException;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


@Slf4j
public class AzurePanel implements AzureAbstractConfigurablePanel {
    private static final String DISPLAY_NAME = "Azure";
    private JPanel contentPane;
    private JCheckBox allowTelemetryCheckBox;
    private JTextPane allowTelemetryComment;
    private JComboBox<AzureEnvironment> azureEnvironmentComboBox;
    private JComboBox<String> savePasswordComboBox;
    private TextFieldWithBrowseButton funcCoreToolsPath;

    public AzurePanel() {
    }

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
                    setText(envToString(value));
            }
        });

        savePasswordComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Until restart", "Forever", "Never"}));

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

        if (StringUtils.isNotBlank(state.passwordSaveType())) {
            savePasswordComboBox.setSelectedItem(state.passwordSaveType());
        }
    }

    private static String envToString(@Nonnull AzureEnvironment env) {
        final String name = AzureEnvironmentUtils.getCloudNameForAzureCli(env);
        return String.format("%s - %s", StringUtils.removeEnd(name, "Cloud"), env.getActiveDirectoryEndpoint());
    }

    public JComponent getPanel() {
        return contentPane;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean doOKAction() {
        final AzureConfigurations.AzureConfigurationData config = AzureConfigurations.getInstance().getState();
        config.allowTelemetry(allowTelemetryCheckBox.isSelected());
        config.functionCoreToolsPath(this.funcCoreToolsPath.getText());
        config.environment(AzureEnvironmentUtils.azureEnvironmentToString((AzureEnvironment) azureEnvironmentComboBox.getSelectedItem()));
        config.passwordSaveType((String) savePasswordComboBox.getSelectedItem());
        AzureConfigurations.getInstance().loadState(config);

        final String userAgent = String.format(AzurePlugin.USER_AGENT, AzurePlugin.PLUGIN_VERSION,
            config.allowTelemetry() ? config.installationId() : StringUtils.EMPTY);
        Azure.az().config().setUserAgent(userAgent);
        CommonSettings.setUserAgent(userAgent);
        Azure.az(AzureCloud.class).set(AzureEnvironmentUtils.stringToAzureEnvironment(config.environment()));
        return true;
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

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
