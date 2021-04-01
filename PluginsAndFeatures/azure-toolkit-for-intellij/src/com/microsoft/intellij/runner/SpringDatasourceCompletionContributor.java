/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.runner;

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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.link.LinkMySQLToModuleDialog;
import com.microsoft.azure.toolkit.intellij.link.base.LinkType;
import com.microsoft.azure.toolkit.intellij.link.po.LinkPO;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.AzureLinkStorage;
import com.microsoft.intellij.helpers.AzureIconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SpringDatasourceCompletionContributor extends CompletionContributor {

    public SpringDatasourceCompletionContributor() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(),
               new CompletionProvider<CompletionParameters>() {
                    @Override
                    public void addCompletions(@NotNull CompletionParameters parameters,
                                               @NotNull ProcessingContext context,
                                               @NotNull CompletionResultSet resultSet) {
                        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
                        if (!AuthMethodManager.getInstance().isSignedIn() && getLinkForModule(module) == null) {
                            // Do not show hint if user not signed in and no service link for file module
                            return;
                        }
                        resultSet.addElement(LookupElementBuilder
                                                 .create("spring.datasource.url")
                                                 .withIcon(AzureIconLoader.loadIcon(AzureIconSymbol.MySQL.BIND_INTO))
                                                 .withInsertHandler(new MyInsertHandler())
                                                 .withBoldness(true)
                                                 .withTypeText("String")
                                                 .withTailText(" (Connect to Azure Datasource for MySQL)")
                                                 .withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT));
                    }
                });
    }

    private static class MyInsertHandler implements InsertHandler<LookupElement> {

        @Override
        public void handleInsert(@NotNull InsertionContext insertionContext, @NotNull LookupElement lookupElement) {
            final Module module = ModuleUtil.findModuleForFile(insertionContext.getFile().getVirtualFile(), insertionContext.getProject());
            final LinkPO moduleLink = getLinkForModule(module);
            if (Objects.nonNull(moduleLink)) {
                final String envPrefix = moduleLink.getEnvPrefix();
                this.insertSpringDatasourceProperties(envPrefix, insertionContext);
            } else {
                ApplicationManager.getApplication().invokeLater(() -> {
                    final LinkMySQLToModuleDialog dialog = new LinkMySQLToModuleDialog(insertionContext.getProject(), null, module);
                    final String envPrefix = dialog.showAndGetEnvPrefix();
                    WriteCommandAction.runWriteCommandAction(insertionContext.getProject(), () -> {
                        if (StringUtils.isNotBlank(envPrefix)) {
                            this.insertSpringDatasourceProperties(envPrefix, insertionContext);
                        } else {
                            EditorModificationUtil.insertStringAtCaret(insertionContext.getEditor(), "=", true);
                        }
                    });
                });

            }
        }

        private void insertSpringDatasourceProperties(String envPrefix, @NotNull InsertionContext insertionContext) {
            final StringBuilder builder = new StringBuilder();
            builder.append("=${").append(envPrefix).append("URL}").append(StringUtils.LF)
                   .append("spring.datasource.username=${").append(envPrefix).append("USERNAME}").append(StringUtils.LF)
                   .append("spring.datasource.password=${").append(envPrefix).append("PASSWORD}").append(StringUtils.LF);
            EditorModificationUtil.insertStringAtCaret(insertionContext.getEditor(), builder.toString(), true);
        }
    }

    @Nullable
    private static LinkPO getLinkForModule(@Nullable final Module module) {
        return module == null ? null : AzureLinkStorage.getProjectStorage(module.getProject()).getLinkByModuleId(module.getName())
                                                       .stream()
                                                       .filter(e -> LinkType.SERVICE_WITH_MODULE == e.getType())
                                                       .findFirst().orElse(null);
    }

}
