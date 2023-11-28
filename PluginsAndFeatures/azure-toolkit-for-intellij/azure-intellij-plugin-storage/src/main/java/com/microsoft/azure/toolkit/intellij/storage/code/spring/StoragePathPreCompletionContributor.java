/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.LookupElements;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class StoragePathPreCompletionContributor extends CompletionContributor {

    public StoragePathPreCompletionContributor() {
        super();
        extend(null, psiElement(JavaTokenType.STRING_LITERAL).withParent(literalExpression()), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull final CompletionParameters parameters, @NotNull final ProcessingContext context, @NotNull final CompletionResultSet result) {
                final PsiElement element = parameters.getPosition();
                final PsiLiteralExpression literal = ((PsiLiteralExpression) element.getParent());
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : element.getText();
                final String fullPrefix = StringUtils.substringBefore(value, StoragePathCompletionContributor.DUMMY_IDENTIFIER);
                final boolean isBlobContainer = fullPrefix.startsWith("azure-blob://");
                final boolean isFileShare = fullPrefix.startsWith("azure-file://");

                if (isBlobContainer || isFileShare) {
                    final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
                    if (Objects.isNull(module)) {
                        return;
                    }
                    if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                        AzureTelemeter.info("connector.not_signed_in.storage_string_code_completion");
                        result.addElement(LookupElements.buildSignInLookupElement());
                    } else if (AzureModule.from(module).getConnections(StorageAccountResourceDefinition.INSTANCE).stream().noneMatch(Connection::isValidConnection)) {
                        AzureTelemeter.info("connector.signed_in_no_connections.storage_string_code_completion");
                        result.addElement(LookupElements.buildConnectLookupElement(StorageAccountResourceDefinition.INSTANCE, (definition, ctx) -> {
                            if (Objects.nonNull(definition)) {
                                AutoPopupController.getInstance(ctx.getProject()).scheduleAutoPopup(ctx.getEditor());
                            }
                        }));
                    }
                }
            }
        });
    }
}
