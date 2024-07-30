/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public class StoragePathPrefixCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final PsiElement parent = element.getParent();
        if (!(parent instanceof PsiLiteralExpression)) {
            return;
        }
        final PsiLiteralExpression literal = ((PsiLiteralExpression) parent);
        final String value = literal.getValue() instanceof String ? (String) literal.getValue() : element.getText();
        final String fullPrefix = StringUtils.substringBefore(value, StoragePathCompletionContributor.DUMMY_IDENTIFIER);
        final boolean isBlobContainer = fullPrefix.startsWith("azure-blob://");
        final boolean isFileShare = fullPrefix.startsWith("azure-file://");

        if (!isBlobContainer && !isFileShare) {
            result = result.withPrefixMatcher(fullPrefix);
            result.addElement(LookupElementBuilder.create("azure-blob://")
                .withInsertHandler(new MyInsertHandler())
                .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.CONTAINERS)));
            result.addElement(LookupElementBuilder.create("azure-file://")
                .withInsertHandler(new MyInsertHandler())
                .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.SHARES)));
            AzureTelemeter.log(AzureTelemetry.Type.OP_END, OperationBundle.description("boundary/connector.complete_storage_resource_prefixes_in_string_literal"));
            result.stopHere();
        }
    }

    private static class MyInsertHandler implements InsertHandler<LookupElement> {
        @Override
        @AzureOperation("user/connector.insert_storage_resource_prefix_from_code_completion")
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement item) {
            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
        }
    }
}
