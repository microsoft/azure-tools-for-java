/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class SpringDatasourceCompletionContributor extends CompletionContributor {

    public SpringDatasourceCompletionContributor() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
                if (module == null) {
                    return;
                }
                final List<LookupElement> lookupElements = generateLookupElements();
                if (CollectionUtils.isNotEmpty(lookupElements)) {
                    lookupElements.forEach(resultSet::addElement);
                }
            }
        });
    }

    public abstract List<LookupElement> generateLookupElements();

    protected static class MyInsertHandler implements InsertHandler<LookupElement> {

        private final String resourceType;

        public MyInsertHandler(String resourceType) {
            this.resourceType = resourceType;
        }

        @Override
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement lookupElement) {
            final Project project = context.getProject();
            final Module module = ModuleUtil.findModuleForFile(context.getFile().getVirtualFile(), project);
            if (module != null) {
                project.getService(ConnectionManager.class)
                        .getConnectionsByConsumerId(module.getName()).stream()
                        .filter(c -> StringUtils.equals(resourceType, c.getResource().getType()))
                        .map(c -> ((Connection<DatabaseResource, ModuleResource>) c)).findAny()
                        .ifPresentOrElse(c -> this.insert(c, context), () -> this.createAndInsert(module, context));
            }
        }

        private void createAndInsert(Module module, @NotNull InsertionContext context) {
            final Project project = context.getProject();
            AzureTaskManager.getInstance().runLater(() -> {
                final var dialog = new ConnectorDialog(project);
                dialog.setConsumer(new ModuleResource(module.getName()));
                dialog.setResourceType(resourceType);
                if (dialog.showAndGet()) {
                    final Connection<? extends Resource, ? extends Resource> c = dialog.getData();
                    WriteCommandAction.runWriteCommandAction(project, () -> this.insert(c, context));
                } else {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        EditorModificationUtil.insertStringAtCaret(context.getEditor(), "=", true);
                    });
                }
            });
        }

        private void insert(Connection<? extends Resource, ? extends Resource> c, @NotNull InsertionContext context) {
            final String envPrefix = ((DatabaseResource) c.getResource()).getEnvPrefix();
            final String builder = "=${" + envPrefix + "URL}" + StringUtils.LF
                    + "spring.datasource.username=${" + envPrefix + "USERNAME}" + StringUtils.LF
                    + "spring.datasource.password=${" + envPrefix + "PASSWORD}" + StringUtils.LF;
            EditorModificationUtil.insertStringAtCaret(context.getEditor(), builder, true);
        }
    }
}
