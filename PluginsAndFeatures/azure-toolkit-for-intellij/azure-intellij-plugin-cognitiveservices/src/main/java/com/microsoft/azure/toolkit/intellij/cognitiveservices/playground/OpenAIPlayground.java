/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.playground;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.GPTDeploymentComboBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.Configuration;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.SystemMessage;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.service.SystemMessageTemplateService;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class OpenAIPlayground extends BaseEditor {

    @Nonnull
    private final CognitiveAccount account;
    @Nullable
    private final CognitiveDeployment deployment;

    private JPanel pnlRoot;
    private JPanel pnlTitleBar;
    private JPanel pnlBody;
    private JPanel pnlConfigurationContainer;
    private JPanel pnlSystem;
    private JPanel pnlChat;
    private JTabbedPane pnlAssistantTabPane;
    private SystemMessagePanel pnlSystemMessage;
    private AzureActionButton<Void> btnExport;
    private AzureActionButton<Void> btnImport;
    private JPanel pnlSystemMessageContainer;
    private ConfigurationPanel pnlConfiguration;
    private ChatBox chatBox;
    private GPTDeploymentComboBox cbDeployment;
    private JPanel pnlExample;

    public OpenAIPlayground(@Nonnull final Project project, @Nonnull final CognitiveAccount account, @Nonnull final VirtualFile virtualFile) {
        this(project, account, null, virtualFile);
    }

    public OpenAIPlayground(@Nonnull final Project project, @Nonnull final CognitiveDeployment deployment, @Nonnull final VirtualFile virtualFile) {
        this(project, deployment.getParent(), deployment, virtualFile);
    }

    public OpenAIPlayground(@Nonnull final Project project, @Nonnull final CognitiveAccount account,
                            @Nullable final CognitiveDeployment deployment, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile);
        this.account = account;
        this.deployment = deployment;
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        final Configuration defaultConfiguration = Configuration.DEFAULT;
        this.pnlConfiguration.setValue(defaultConfiguration);
        this.pnlConfiguration.addValueChangedListener(this::updateConfiguration);

        final SystemMessage defaultSystemMessage = SystemMessageTemplateService.getDefaultSystemMessage();
        this.pnlSystemMessage.setValue(defaultSystemMessage);
        this.pnlSystemMessage.setValueChangedListener(this::updateSystemMessage);

        cbDeployment.setAccount(this.account);
        cbDeployment.addValueChangedListener(this::onDeploymentChanged);
        Optional.ofNullable(this.deployment).ifPresent(deployment -> {
            cbDeployment.setValue(deployment);
            this.onDeploymentChanged(deployment);
        });
    }

    @AzureOperation(value = "user/openai.update_system_message", source = "this.account")
    private void updateSystemMessage(final SystemMessage m) {
        this.chatBox.clearSession();
        Optional.ofNullable(this.chatBox.getChatBot()).ifPresent(b -> b.setSystemMessage(m));
    }

    @AzureOperation(value = "user/openai.update_configuration", source = "this.account")
    private void updateConfiguration(final Configuration v) {
        Optional.ofNullable(this.chatBox.getChatBot()).ifPresent(b -> b.setConfiguration(v));
    }

    private void onDeploymentChanged(@Nonnull final CognitiveDeployment deployment) {
        if (Objects.isNull(chatBox.getChatBot())
            || Objects.isNull(chatBox.getChatBot().getDeployment())
            || !Objects.equals(chatBox.getChatBot().getDeployment(), deployment)) {
            AzureTaskManager.getInstance().runOnPooledThread(() -> {
                final ChatBot chatBot = new ChatBot(deployment);
                AzureTaskManager.getInstance().runLater(() -> {
                    chatBot.setConfiguration(pnlConfiguration.getValue());
                    chatBot.setSystemMessage(pnlSystemMessage.getValue());
                    this.chatBox.setChatBot(chatBot);
                });
            });
        }
    }

    @Override
    public @Nonnull JComponent getComponent() {
        return pnlRoot;
    }

    @Override
    public String getName() {
        return String.format("Azure OpenAI Playground (%s)", account.getName());
    }

    @Override
    public void dispose() {

    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
