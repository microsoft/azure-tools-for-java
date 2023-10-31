package com.microsoft.azure.toolkit.intellij.connector.code.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.intellij.connector.code.model.CompletionItem;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractResourceConnectionCompletionProvider extends CompletionProvider<CompletionParameters> {
    public static final String INDICATOR = "IntellijIdeaRulezzz ";
    protected LookupElement toLookupElement(@Nonnull final CompletionItem item) {
        return LookupElementBuilder.create(item.getValue())
                .withPresentableText(item.getDisplayName())
                .withLookupStrings(item.getLookupValues())
                .withIcon(IntelliJAzureIcons.getIcon(item.getIcon()))
                .withBoldness(true)
                .withTypeText("String")
                .withTailText(item.getHintMessage());
    }

    protected <T extends AzResource> LookupElement getCreateConnectionElement(@Nonnull final Module module, @Nonnull final T resource,
                                                                              @Nonnull final ResourceDefinition<T> definition,
                                                                              @Nonnull final Function<Connection<? extends AzResource, ?>, List<CompletionItem>> valueFunction) {
        final Icon icon = Optional.ofNullable(definition.getIcon()).map(IntelliJAzureIcons::getIcon)
                .orElseGet(() -> IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        final LookupElementBuilder builder = LookupElementBuilder.create(resource.getName())
                .withInsertHandler(((context, item) -> onInsertNewConnection(module, definition.define(resource), valueFunction, context, item)))
                .withIcon(icon)
                .withBoldness(true)
                .withTypeText("String")
                .withTailText(String.format("connect to resource %s", resource.getName()));
        return builder;
    }

    private <T extends AzResource> void onInsertNewConnection(@Nonnull final Module module, @Nonnull final Resource<T> resource,
                                                              @Nonnull final Function<Connection<? extends AzResource, ?>, List<CompletionItem>> valueFunction,
                                                              @Nonnull final InsertionContext context, @Nonnull final LookupElement lookupElement) {
        AzureTaskManager.getInstance().runLater(() -> {
            final var dialog = new ConnectorDialog(module.getProject());
            dialog.setConsumer(new ModuleResource(module.getName()));
            dialog.setResource(resource);
            if (dialog.showAndGet()) {
                final Connection<T, ?> connection = (Connection<T, ?>) dialog.getValue();
                final List<CompletionItem> items = valueFunction.apply(connection);
                final String value = items.stream().map(CompletionItem::getValue).findFirst().orElse("");
                WriteCommandAction.runWriteCommandAction(module.getProject(), () -> {
                    context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
                    context.getDocument().insertString(context.getStartOffset(), value);
                    context.commitDocument();
                });
            }
        });
    }
}
