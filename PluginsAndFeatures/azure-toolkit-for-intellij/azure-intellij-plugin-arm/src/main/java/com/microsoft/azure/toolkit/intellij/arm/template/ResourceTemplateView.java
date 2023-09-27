/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.template;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.ide.arm.DeploymentActionsContributor;
import com.microsoft.azure.toolkit.intellij.arm.language.ARMTemplateLanguage;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.component.UIUtils;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeploymentDraft;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

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

    private AzureActionButton<ResourceDeployment> exportTemplateButton;
    private AzureActionButton<Void> updateDeploymentButton;
    private JPanel contentPane;
    private JPanel templatePanel;
    private JPanel parameterPanel;
    private JLabel lblEditorPanel;
    private JLabel lblParametersPanel;
    private JSplitPane armSplitPanel;
    private AzureActionButton<ResourceDeployment> exportParameterFileButton;

    private FileEditor templateEditor;
    private FileEditor parameterEditor;

    public ResourceTemplateView(@Nonnull Project project, @Nonnull ResourceDeployment deployment, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile, deployment, project);
        this.project = project;
        this.deployment = deployment;
        this.draft = (ResourceDeploymentDraft) this.deployment.update();
        initListeners();
        this.rerender();
    }

    private void initListeners() {

        exportTemplateButton.setAction(DeploymentActionsContributor.EXPORT_TEMPLATE, this.deployment);
        exportParameterFileButton.setAction(DeploymentActionsContributor.EXPORT_PARAMETER, this.deployment);
        final Action<Void> updateDeploymentAction = new Action<Void>(Action.Id.of("user/arm.update_deployment.deployment"))
                .withAuthRequired(true)
                .withIdParam(deployment.getName())
                .withSource(deployment)
                .withHandler((ignore, e) -> AzureTaskManager.getInstance().runLater(this::updateDeployment));
        updateDeploymentButton.setAction(updateDeploymentAction);
        final MessageBusConnection connection = project.getMessageBus().connect(this);
        connection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, new FileEditorManagerListener.Before() {
            @Override
            public void beforeFileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
                if (file.getFileType().getName().equals(ResourceTemplateViewProvider.TYPE) && file.getName().equals(deployment.getName())) {
                    try {
                        if (deployment.exists() && isModified() && UIUtils.showYesNoDialog(PROMPT_TITLE, PROMPT_MESSAGE_CLOSE)) {
                            apply();
                        }
                    } finally {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    private void updateDeployment() {
        if (UIUtils.showYesNoDialog(PROMPT_TITLE, PROMPT_MESSAGE_UPDATE_DEPLOYMENT)) {
            apply();
        }
    }

    @Override
    protected void rerender() {
        AzureTaskManager.getInstance().runLater(() -> this.setData(this.draft));
    }

    public synchronized void setData(ResourceDeployment deployment) {
        if (!deployment.exists()) {
            return;
        }
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
            OperationContext.action().setTelemetryProperty("subscriptionId", subscriptionId);
            this.draft.commit();
            this.updateDeploymentButton.setEnabled(this.isModified());
        };
        final AzureString title = OperationBundle.description("user/arm.update_deployment.deployment", this.draft.getName());
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
        return Optional.ofNullable((PsiAwareTextEditorImpl) templateEditor)
            .map(TextEditorImpl::getEditor)
            .map(Editor::getDocument)
            .map(Document::getText).orElse("");
    }

    public String getParameters() {
        final Gson gson = new Gson();
        return Optional.ofNullable((PsiAwareTextEditorImpl) parameterEditor)
            .map(TextEditorImpl::getEditor)
            .map(Editor::getDocument)
            .map(Document::getText)
            .filter(StringUtils::isNotBlank)
            .map(parameters -> Optional.ofNullable(gson.fromJson(parameters, JsonElement.class))
                .map(JsonElement::getAsJsonObject)
                .map(o -> o.get("parameters"))
                .map(JsonElement::toString).orElse(parameters))
            .orElse("");
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
        if (Objects.nonNull(templateEditor)) {
            PsiAwareTextEditorProvider.getInstance().disposeEditor(templateEditor);
        }
        if (Objects.nonNull(parameterEditor)) {
            PsiAwareTextEditorProvider.getInstance().disposeEditor(parameterEditor);
        }
    }
}
