package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class EditConnectionFix implements IntentionAction {
    private final Connection<? extends AzResource, ?> connection;

    @Override
    public @IntentionName @NotNull String getText() {
        return "Edit Connection";
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Resource Connection Fixes";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
        final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), null, "azure.annotator.action", context);
        AzureActionManager.getInstance().getAction(ResourceConnectionActionsContributor.EDIT_CONNECTION).handle(connection, event);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}