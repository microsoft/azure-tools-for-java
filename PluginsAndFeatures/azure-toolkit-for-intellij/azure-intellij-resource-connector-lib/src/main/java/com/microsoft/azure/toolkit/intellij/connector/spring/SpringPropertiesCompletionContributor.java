/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.spring;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public class SpringPropertiesCompletionContributor extends CompletionContributor {
    public SpringPropertiesCompletionContributor() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            public void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet resultSet) {
                final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
                if (module == null) {
                    return;
                }
                ResourceManager.getDefinitions(ResourceDefinition.RESOURCE).stream()
                        .filter(d -> d instanceof SpringSupported)
                        .map(d -> (SpringSupported<?>) d)
                        .filter(d -> CollectionUtils.isNotEmpty(d.getSpringProperties()))
                        .map(definition -> LookupElementBuilder
                                .create(definition.getName(), definition.getSpringProperties().get(0).getKey())
                                .withIcon(AzureIcons.getIcon("/icons/connector/connect.svg"))
                                .withInsertHandler(new MyInsertHandler(definition))
                                .withBoldness(true)
                                .withTypeText("String")
                                .withTailText(String.format(" (%s)", definition.getTitle())))
                        .forEach(resultSet::addElement);
            }
        });
    }

    @RequiredArgsConstructor
    protected static class MyInsertHandler implements InsertHandler<LookupElement> {

        private final ResourceDefinition<?> definition;

        @Override
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement lookupElement) {
            final Project project = context.getProject();
            final Module module = ModuleUtil.findModuleForFile(context.getFile().getVirtualFile(), project);
            if (module != null) {
                project.getService(ConnectionManager.class)
                        .getConnectionsByConsumerId(module.getName()).stream()
                        .filter(c -> Objects.equals(definition, c.getResource().getDefinition())).findAny()
                        .ifPresentOrElse(c -> this.insert(c, context), () -> this.createAndInsert(module, context));
            }
        }

        private void createAndInsert(Module module, @Nonnull InsertionContext context) {
            final Project project = context.getProject();
            AzureTaskManager.getInstance().runLater(() -> {
                final var dialog = new ConnectorDialog(project);
                dialog.setConsumer(new ModuleResource(module.getName()));
                dialog.setResourceDefinition(definition);
                if (dialog.showAndGet()) {
                    final Connection<?, ?> c = dialog.getValue();
                    WriteCommandAction.runWriteCommandAction(project, () -> this.insert(c, context));
                } else {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        EditorModificationUtil.insertStringAtCaret(context.getEditor(), "=", true);
                    });
                }
            });
        }

        private void insert(Connection<?, ?> c, @Nonnull InsertionContext context) {
            final List<Pair<String, String>> properties = SpringSupported.getProperties(c);
            final StringBuilder result = new StringBuilder("=").append(properties.get(0).getValue()).append(StringUtils.LF);
            for (int i = 1; i < properties.size(); i++) {
                result.append(properties.get(i).getKey()).append("=").append(properties.get(i).getValue()).append(StringUtils.LF);
            }
            EditorModificationUtil.insertStringAtCaret(context.getEditor(), result.toString(), true);
        }
    }
}
