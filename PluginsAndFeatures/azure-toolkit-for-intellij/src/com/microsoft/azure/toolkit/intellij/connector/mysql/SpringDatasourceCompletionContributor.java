/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
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
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class SpringDatasourceCompletionContributor extends CompletionContributor {

    public SpringDatasourceCompletionContributor() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
                if (module == null) {
                    return;
                }
                resultSet.addElement(LookupElementBuilder
                        .create("spring.datasource.url")
                        .withIcon(AzureIconLoader.loadIcon(AzureIconSymbol.MySQL.BIND_INTO))
                        .withInsertHandler(new MyInsertHandler())
                        .withBoldness(true)
                        .withTypeText("String")
                        .withTailText(" (Azure Database for MySQL)")
                        .withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT));
            }
        });
    }

    private static class MyInsertHandler implements InsertHandler<LookupElement> {

        @Override
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement lookupElement) {
            final Project project = context.getProject();
            final Module module = ModuleUtil.findModuleForFile(context.getFile().getVirtualFile(), project);
            if (module != null) {
                project.getService(ConnectionManager.class)
                        .getConnectionsByConsumerId(module.getName()).stream()
                        .filter(c -> MySQLDatabaseResource.TYPE.equals(c.getResource().getType()))
                        .map(c -> ((Connection<MySQLDatabaseResource, ModuleResource>) c)).findAny()
                        .ifPresentOrElse(c -> this.insert(c, context), () -> this.createAndInsert(module, context));
            }
        }

        private void createAndInsert(Module module, @NotNull InsertionContext context) {
            final Project project = context.getProject();
            AzureTaskManager.getInstance().runLater(() -> {
                final var dialog = new ConnectorDialog<MySQLDatabaseResource, ModuleResource>(project);
                dialog.setConsumer(new ModuleResource(module.getName()));
                if (dialog.showAndGet()) {
                    final Connection<MySQLDatabaseResource, ModuleResource> c = dialog.getData();
                    WriteCommandAction.runWriteCommandAction(project, () -> this.insert(c, context));
                } else {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        EditorModificationUtil.insertStringAtCaret(context.getEditor(), "=", true);
                    });
                }
            });
        }

        private void insert(Connection<MySQLDatabaseResource, ModuleResource> c, @NotNull InsertionContext context) {
            final String envPrefix = c.getResource().getEnvPrefix();
            final String builder = "=${" + envPrefix + "URL}" + StringUtils.LF
                    + "spring.datasource.username=${" + envPrefix + "USERNAME}" + StringUtils.LF
                    + "spring.datasource.password=${" + envPrefix + "PASSWORD}" + StringUtils.LF;
            EditorModificationUtil.insertStringAtCaret(context.getEditor(), builder, true);
        }
    }
}
