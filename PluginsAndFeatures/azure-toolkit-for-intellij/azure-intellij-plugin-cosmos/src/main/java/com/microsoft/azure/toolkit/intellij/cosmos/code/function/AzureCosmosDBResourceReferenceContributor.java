/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.code.function;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationResourceReference;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.CosmosDBContainerNameCompletionProvider.COSMOS_CONTAINER_NAME_PAIR_PATTERN;
import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.CosmosDBDatabaseNameCompletionProvider.COSMOS_DATABASE_NAME_PAIR_PATTERN;

public class AzureCosmosDBResourceReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).inside(COSMOS_DATABASE_NAME_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                if (StringUtils.isNotBlank(value)) {
                    final TextRange range = new TextRange(1, value.length() + 1);
                    return new PsiReference[]{new FunctionAnnotationResourceReference(element, range,
                            (ann, con) -> CosmosDBDatabaseNameCompletionProvider.getConnectedDatabase(ann))};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression.class).inside(COSMOS_CONTAINER_NAME_PAIR_PATTERN), new PsiReferenceProvider() {
            @Override
            public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                final PsiLiteralExpression literal = (PsiLiteralExpression) element;
                final String value = literal.getValue() instanceof String ? (String) literal.getValue() : null;
                final BiFunction<PsiAnnotation, Connection<?, ?>, AzResource> function = (annotation, connection) -> {
                    final SqlDatabase database = CosmosDBDatabaseNameCompletionProvider.getConnectedDatabase(annotation);
                    return database.containers().get(value, database.getResourceGroupName());
                };
                if (StringUtils.isNotBlank(value)) {
                    final TextRange range = new TextRange(1, value.length() + 1);
                    return new PsiReference[]{new FunctionAnnotationResourceReference(element, range, function)};
                }
                return PsiReference.EMPTY_ARRAY;
            }
        });
    }
}
