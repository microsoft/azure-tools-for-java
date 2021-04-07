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
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class SpringDatasourceCompletionContributor extends CompletionContributor {

    public SpringDatasourceCompletionContributor() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
                if (module == null || !AuthMethodManager.getInstance().isSignedIn()) {
                    return;
                }
                final Project project = module.getProject();
                final List<Connection<? extends Resource, ? extends Resource>> connections = project
                    .getService(ConnectionManager.class)
                    .getConnectionsByConsumerId(module.getName());
                if (connections.size() > 0) {
                    resultSet.addElement(LookupElementBuilder
                        .create("spring.datasource.url")
                        .withIcon(AzureIconLoader.loadIcon(AzureIconSymbol.MySQL.BIND_INTO))
                        .withInsertHandler(new MyInsertHandler())
                        .withBoldness(true)
                        .withTypeText("String")
                        .withTailText(" (Azure Database for MySQL)")
                        .withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT));
                }
            }
        });
    }

    private static class MyInsertHandler implements InsertHandler<LookupElement> {

        @Override
        public void handleInsert(@Nonnull InsertionContext insertionContext, @Nonnull LookupElement lookupElement) {
            final Module module = ModuleUtil.findModuleForFile(insertionContext.getFile().getVirtualFile(), insertionContext.getProject());
            if (module != null) {
                final Project project = module.getProject();
                project.getService(ConnectionManager.class)
                    .getConnectionsByConsumerId(module.getName()).stream()
                    .filter(c -> MySQLDatabaseResource.Definition.AZURE_MYSQL == c.getConsumer().getDefinition())
                    .map(c -> ((Connection<MySQLDatabaseResource, ModuleResource>) c)).findAny()
                    .or(() -> createConnection(insertionContext, module).toSingle().toBlocking().value()) // TODO: might be blocked.
                    .ifPresentOrElse(c -> WriteCommandAction.runWriteCommandAction(insertionContext.getProject(), () -> {
                        this.insertSpringDatasourceProperties(c.getResource().getEnvPrefix(), insertionContext);
                    }), () -> WriteCommandAction.runWriteCommandAction(insertionContext.getProject(), () -> {
                        EditorModificationUtil.insertStringAtCaret(insertionContext.getEditor(), "=", true);
                    }));
            }
        }

        private Observable<Optional<Connection<MySQLDatabaseResource, ModuleResource>>> createConnection(@Nonnull InsertionContext insertionContext, Module module) {
            return AzureTaskManager.getInstance().runLaterAsObservable(new AzureTask<>(() -> {
                final var dialog = new ConnectorDialog<MySQLDatabaseResource, ModuleResource>(insertionContext.getProject());
                return dialog.showAndGet() ? Optional.of(dialog.getForm().getData()) : Optional.empty();
            }));
        }

        private void insertSpringDatasourceProperties(String envPrefix, @NotNull InsertionContext insertionContext) {
            final String builder = "=${" + envPrefix + "URL}" + StringUtils.LF
                + "spring.datasource.username=${" + envPrefix + "USERNAME}" + StringUtils.LF
                + "spring.datasource.password=${" + envPrefix + "PASSWORD}" + StringUtils.LF;
            EditorModificationUtil.insertStringAtCaret(insertionContext.getEditor(), builder, true);
        }
    }
}
