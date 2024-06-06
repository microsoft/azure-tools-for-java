package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
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
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AnnotationFixes {
    public static final Runnable DO_NOTHING = () -> {
    };
    public static final Consumer<Connection<?, ?>> DO_NOTHING_CONSUMER = (r) -> {
    };

    public static IntentionAction createNewConnection(ResourceDefinition<?> definition, Consumer<Connection<?, ?>> callback) {
        return createNewConnection(definition, callback, null);
    }

    public static IntentionAction createNewConnection(ResourceDefinition<?> definition, Consumer<Connection<?, ?>> callback, String defaultEnvPrefix) {
        return simple("Connect an " + definition.getTitle(), new BiConsumer<>() {
            @Override
            @AzureOperation("user/connector.create_connection_quick_fix")
            public void accept(final Editor editor, final PsiFile file) {
                final Module module = ModuleUtil.findModuleForFile(file);
                if (Objects.nonNull(module)) {
                    final var dialog = new ConnectorDialog(editor.getProject());
                    dialog.setConsumer(new ModuleResource(module.getName()));
                    dialog.setResourceDefinition(definition);
                    Optional.ofNullable(defaultEnvPrefix).filter(StringUtils::isNoneBlank).ifPresent(dialog::setEnvPrefix);
                    if (dialog.showAndGet()) {
                        callback.accept(dialog.getValue());
                    } else {
                        callback.accept(null);
                    }
                }
            }
        });
    }

    public static IntentionAction signIn(Runnable callback) {
        return simple("Sign in to Azure", new BiConsumer<>() {
            @Override
            @AzureOperation("user/connector.sign_in_quick_fix")
            public void accept(final Editor editor, final PsiFile file) {
                if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                    AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle((a) -> callback.run());
                }
            }
        });
    }

    public static void createSignInAnnotation(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "You are not signed in to Azure")
            .range(element.getTextRange())
            .highlightType(ProblemHighlightType.WEAK_WARNING)
            .withFix(AnnotationFixes.signIn(AnnotationFixes.DO_NOTHING))
            .create();
    }

    public static IntentionAction simple(String text, BiConsumer<Editor, PsiFile> action) {
        return new IntentionAction() {

            @Override
            public @IntentionName
            @Nonnull String getText() {
                return text;
            }

            @Override
            public @Nonnull
            @IntentionFamilyName String getFamilyName() {
                return "Azure general fixes";
            }

            @Override
            public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
                return true;
            }

            @Override
            @AzureOperation("user/connector.create_connection_quick_fix")
            public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                action.accept(editor, file);
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }
}
