package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

public class LookupElements {

    public static final String SIGN_IN_TO_AZURE = "Sign in to Azure";
    public static final String CONNECT_AZURE_RESOURCE = "Connect Azure resource";

    public static LookupElement buildSignInLookupElement() {
        return LookupElementBuilder.create(SIGN_IN_TO_AZURE)
            .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE.getIconPath()))
            .withPresentableText("Sign in to Azure to select resources...")
            .withTypeText("Action")
            .withCaseSensitivity(false)
            .withInsertHandler(new SignInInsertHandler());
    }

    private static class SignInInsertHandler implements InsertHandler<LookupElement> {
        @Override
        @ExceptionNotification
        @AzureOperation(name = "user/connector.sign_in_from_code_completion")
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement lookupElement) {
            context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
            if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle((a) ->
                    AzureTaskManager.getInstance().runLater(() ->
                        AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor())));
            }
        }
    }

    public static LookupElement buildConnectLookupElement(@Nullable final ResourceDefinition<?> definition, BiConsumer<Connection<?, ?>, InsertionContext> onResult) {
        return LookupElementBuilder.create(CONNECT_AZURE_RESOURCE)
            .withIcon(IntelliJAzureIcons.getIcon(Optional.ofNullable(definition).map(ResourceDefinition::getIcon).orElse(AzureIcons.Common.AZURE.getIconPath())))
            .withPresentableText(String.format("Connect %s...", Optional.ofNullable(definition).map(ResourceDefinition::getTitle).orElse("Azure resource")))
            .withTypeText("Action")
            .withCaseSensitivity(false)
            .withInsertHandler(new ConnectInsertHandler(definition, onResult));
    }

    @RequiredArgsConstructor
    private static class ConnectInsertHandler implements InsertHandler<LookupElement> {
        @Nullable
        private final ResourceDefinition<?> definition;
        private final BiConsumer<Connection<?, ?>, InsertionContext> onResult;

        @Override
        @ExceptionNotification
        @AzureOperation(name = "user/connector.create_connection_from_code_completion")
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement lookupElement) {
            context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
            context.commitDocument();
            final Project project = context.getProject();
            final Module module = ModuleUtil.findModuleForFile(context.getFile().getVirtualFile(), project);
            AzureTaskManager.getInstance().write(() -> Optional.ofNullable(module).map(AzureModule::from)
                .map(AzureModule::initializeWithDefaultProfileIfNot).map(Profile::getConnectionManager)
                .ifPresent(connectionManager -> createAndInsert(module, context)));
        }

        private void createAndInsert(Module module, @Nonnull InsertionContext context) {
            AzureTaskManager.getInstance().runLater(() -> {
                final Project project = context.getProject();
                final var dialog = new ConnectorDialog(project);
                dialog.setConsumer(new ModuleResource(module.getName()));
                if (Objects.nonNull(definition)) {
                    dialog.setResourceDefinition(definition);
                }
                if (dialog.showAndGet()) {
                    final Connection<?, ?> c = dialog.getValue();
                    WriteCommandAction.runWriteCommandAction(project, () -> onResult.accept(c, context));
                } else {
                    WriteCommandAction.runWriteCommandAction(project, () -> onResult.accept(null, context));
                }
            });
        }
    }
}
