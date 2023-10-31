package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

public class AnnotationFixes {
    public static final Runnable DO_NOTHING = () -> {
    };
    public static final Consumer<Connection<?, ?>> DO_NOTHING_CONSUMER = (r) -> {
    };

    public static IntentionAction createNewConnection(ResourceDefinition<?> definition, Consumer<Connection<?, ?>> callback) {
        return new IntentionAction() {

            @Override
            public @IntentionName
            @Nonnull String getText() {
                return "Connect an " + definition.getTitle();
            }

            @Override
            public @Nonnull
            @IntentionFamilyName String getFamilyName() {
                return "Azure General fixes";
            }

            @Override
            public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
                return true;
            }

            @Override
            public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                final Module module = ModuleUtil.findModuleForFile(file);
                if (Objects.nonNull(module)) {
                    final var dialog = new ConnectorDialog(project);
                    dialog.setConsumer(new ModuleResource(module.getName()));
                    dialog.setResourceDefinition(definition);
                    if (dialog.showAndGet()) {
                        callback.accept(dialog.getValue());
                    } else {
                        callback.accept(null);
                    }

                }
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }

    public static IntentionAction signIn(Runnable callback) {
        return new IntentionAction() {

            @Override
            public @IntentionName
            @Nonnull String getText() {
                return "Sign in to Azure";
            }

            @Override
            public @Nonnull
            @IntentionFamilyName String getFamilyName() {
                return "Azure General fixes";
            }

            @Override
            public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
                return true;
            }

            @Override
            public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                    AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle((a) -> callback.run());
                }
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }

}
