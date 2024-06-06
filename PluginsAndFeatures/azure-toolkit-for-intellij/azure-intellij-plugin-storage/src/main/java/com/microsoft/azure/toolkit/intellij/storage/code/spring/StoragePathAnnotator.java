/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.code.AnnotationFixes;
import com.microsoft.azure.toolkit.intellij.connector.code.Utils;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AzComponent;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;

public class StoragePathAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (psiElement(JavaTokenType.STRING_LITERAL).withParent(literalExpression()).accepts(element)) {
            final PsiLiteralExpression literal = (PsiLiteralExpression) element.getParent();
            final String valueWithPrefix = StringUtils.substringBefore((String) literal.getValue(), StoragePathCompletionContributor.DUMMY_IDENTIFIER);
            if (Objects.nonNull(valueWithPrefix) && (valueWithPrefix.startsWith("azure-blob://") || valueWithPrefix.startsWith("azure-file://"))) {
                final String prefix = valueWithPrefix.startsWith("azure-blob://") ? "azure-blob://" : "azure-file://";
                final String path = valueWithPrefix.substring(prefix.length());
                if (StringUtils.isBlank(path)) {
                    return;
                }
                final TextRange range = new TextRange(prefix.length() + 1, valueWithPrefix.length() + 1).shiftRight(element.getTextOffset());
                final List<Connection<IStorageAccount, ?>> connections = Optional.of(element)
                    .map(ModuleUtil::findModuleForPsiElement)
                    .map(module -> Utils.getConnections(module, StorageAccountResourceDefinition.INSTANCE))
                    .orElse(Collections.emptyList());
                if (connections.isEmpty()) {
                    holder.newAnnotation(HighlightSeverity.WARNING, "No Azure Storage account connected")
                        .range(range)
                        .highlightType(ProblemHighlightType.WEAK_WARNING)
                        .withFix(AnnotationFixes.createNewConnection(StorageAccountResourceDefinition.INSTANCE, AnnotationFixes.DO_NOTHING_CONSUMER))
                        .create();
                } else {
                    if (Utils.hasEnvVars(valueWithPrefix)) { // skip if environment variables are used.
                        return;
                    }
                    final List<IStorageAccount> accounts = connections.stream()
                        .filter(Connection::isValidConnection).map(Connection::getResource).map(Resource::getData).filter(Objects::nonNull).toList();
                    if (accounts.isEmpty() && !Azure.az(AzureAccount.class).isLoggedIn()) {
                        AnnotationFixes.createSignInAnnotation(element, holder);
                        return;
                    }
                    final StorageFile file = StoragePathCompletionProvider.getFile(valueWithPrefix, accounts);
                    if (Objects.isNull(file)) {
                        final String message = String.format("Could not find '%s' in connected Azure Storage account(s) [%s]", path, accounts.stream().map(AzComponent::getName).collect(Collectors.joining(",")));
                        holder.newAnnotation(HighlightSeverity.WARNING, message)
                            .range(range)
                            .highlightType(ProblemHighlightType.WEAK_WARNING)
                            .create();
                    }
                }
            }
        }
    }
}
