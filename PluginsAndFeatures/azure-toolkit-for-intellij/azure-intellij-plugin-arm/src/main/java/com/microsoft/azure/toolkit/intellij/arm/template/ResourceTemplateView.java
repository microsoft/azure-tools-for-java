/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.template;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.intellij.lang.Language;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.intellij.arm.action.DeploymentActions;
import com.microsoft.azure.toolkit.intellij.arm.language.ARMTemplateLanguage;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeploymentDraft;
import com.microsoft.intellij.ui.util.UIUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Objects;

public class ResourceTemplateView extends AzResourcePropertiesEditor<ResourceDeployment> {

    public static final String ID = "com.microsoft.intellij.helpers.arm.ResourceTemplateView";
    private static final String PROMPT_TITLE = "Azure Explorer";
    private static final String PROMPT_MESSAGE_CLOSE = "Would you like to update the deployment before you exit?";
    private static final String PROMPT_MESSAGE_UPDATE_DEPLOYMENT = "Are you sure to update the deployment?";
    private static final Language LANGUAGE = new Language("arm-resource-template", "application/x-template", "application/template") {
    };
    private final ResourceDeployment deployment;
    private final Project project;
    private final ResourceDeploymentDraft draft;

    private JButton exportTemplateButton;
    private JButton updateDeploymentButton;
    private JPanel contentPane;
    private JPanel templatePanel;
    private JPanel parameterPanel;
    private JLabel lblEditorPanel;
    private JLabel lblParametersPanel;
    private JSplitPane armSplitPanel;
    private JButton exportParameterFileButton;

    private FileEditor templateEditor;
    private FileEditor parameterEditor;

    private MessageBusConnection messageBusConnection;

    public ResourceTemplateView(@Nonnull Project project, @Nonnull ResourceDeployment deployment, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile, deployment, project);
        this.project = project;
        this.deployment = deployment;
        this.draft = (ResourceDeploymentDraft) this.deployment.update();
        initListeners();
        this.rerender();
    }

    private void initListeners() {
        exportTemplateButton.addActionListener((e) -> DeploymentActions.exportTemplate(deployment));
        exportParameterFileButton.addActionListener((e) -> DeploymentActions.exportParameters(deployment));
        updateDeploymentButton.addActionListener((e) -> {
            if (UIUtils.showYesNoDialog(PROMPT_TITLE, PROMPT_MESSAGE_UPDATE_DEPLOYMENT)) {
                apply();
            }
        });
        if (messageBusConnection == null) {
            messageBusConnection = project.getMessageBus().connect(this);
            messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, new FileEditorManagerListener.Before() {
                @Override
                public void beforeFileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
                    if (file.getFileType().getName().equals(ResourceTemplateViewProvider.TYPE) && file.getName().equals(deployment.getName())) {
                        try {
                            if (isModified() && UIUtils.showYesNoDialog(PROMPT_TITLE, PROMPT_MESSAGE_CLOSE)) {
                                apply();
                            }
                        } finally {
                            PsiAwareTextEditorProvider.getInstance().disposeEditor(templateEditor);
                            PsiAwareTextEditorProvider.getInstance().disposeEditor(parameterEditor);
                            messageBusConnection.disconnect();
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void rerender() {
        AzureTaskManager.getInstance().runLater(() -> this.setData(this.draft));
    }

    public synchronized void setData(ResourceDeployment deployment) {
        final GridConstraints constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);
        constraints.setAnchor(GridConstraints.ANCHOR_WEST);

        final TextEditorProvider editorProvider = PsiAwareTextEditorProvider.getInstance();
        final LightVirtualFile templateFile = new LightVirtualFile(this.draft.getName() + ".json", ARMTemplateLanguage.INSTANCE, deployment.getTemplateAsJson());
        final LightVirtualFile parameterFile = new LightVirtualFile(this.draft.getName() + ".json", ARMTemplateLanguage.INSTANCE, deployment.getParametersAsJson());
        templateEditor = editorProvider.createEditor(project, templateFile);
        templatePanel.removeAll();
        templatePanel.add(templateEditor.getComponent(), constraints);
        parameterEditor = editorProvider.createEditor(project, parameterFile);
        parameterPanel.removeAll();
        parameterPanel.add(parameterEditor.getComponent(), constraints);

        // Init the split panel
        armSplitPanel.setDividerLocation(0.6); // template : parameter = 6:4
    }

    private void apply() {
        this.updateDeploymentButton.setEnabled(false);
        final Runnable runnable = () -> {
            final String subscriptionId = draft.getSubscriptionId();
            this.draft.setTemplateAsJson(getTemplate());
            this.draft.setParametersAsJson(getParameters());
            AzureTelemetry.getActionContext().setProperty("subscriptionId", subscriptionId);
            this.draft.commit();
            this.updateDeploymentButton.setEnabled(this.isModified());
        };
        final AzureString title = AzureOperationBundle.title("arm.update_deployment.deployment", this.draft.getName());
        AzureTaskManager.getInstance().runInBackground(title, false, runnable);
    }

    private void reset() {
        this.draft.reset();
        this.rerender();
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(this.draft.getParametersAsJson(), getParameters()) ||
            !Objects.equals(this.draft.getTemplateAsJson(), getTemplate());
    }

    public String getTemplate() {
        return ((PsiAwareTextEditorImpl) templateEditor).getEditor().getDocument().getText();
    }

    public String getParameters() {
        final String parameters = ((PsiAwareTextEditorImpl) parameterEditor).getEditor().getDocument().getText();
        final Gson gson = new Gson();
        final JsonElement parametersElement = gson.fromJson(parameters, JsonElement.class).getAsJsonObject().get("parameters");
        return parametersElement == null ? parameters : parametersElement.toString();
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return contentPane;
    }

    protected void refresh() {
        this.updateDeploymentButton.setEnabled(false);
        final String refreshTitle = String.format("Refreshing Resource Template (%s)...", this.draft.getName());
        AzureTaskManager.getInstance().runInBackground(refreshTitle, () -> {
            this.draft.reset();
            this.draft.refresh();
            this.rerender();
        });
    }

    @Nonnull
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public void dispose() {
        super.dispose();
        templateEditor.dispose();
    }
}
