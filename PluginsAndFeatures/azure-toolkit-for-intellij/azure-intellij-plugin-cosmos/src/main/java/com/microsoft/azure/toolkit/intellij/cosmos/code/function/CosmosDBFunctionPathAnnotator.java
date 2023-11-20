/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.code.function;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainer;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.connector.code.AbstractResourceConnectionAnnotator.isAzureFacetEnabled;
import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.CosmosDBContainerNameCompletionProvider.COSMOS_CONTAINER_PATTERN;
import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.CosmosDBDatabaseNameCompletionProvider.COSMOS_DATABASE_PATTERN;

public class CosmosDBFunctionPathAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (!(isAzureFacetEnabled(element) && Azure.az(AzureAccount.class).isLoggedIn())) {
            return;
        }
        if (COSMOS_CONTAINER_PATTERN.accepts(element)) {
            validateContainer(element, holder);
        } else if (COSMOS_DATABASE_PATTERN.accepts(element)) {
            validateDatabase(element, holder);
        }
    }

    private void validateDatabase(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        final PsiLiteralExpression literal = (PsiLiteralExpression) element.getParent();
        final String databaseName = literal.getValue() instanceof String ? (String) literal.getValue() : StringUtils.EMPTY;
        if (StringUtils.isBlank(databaseName)) {
            holder.newAnnotation(HighlightSeverity.WARNING, "DatabaseName could not be empty")
                    .range(element.getTextRange()).highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING).create();
        }
        final String connection = Optional.ofNullable(annotation)
                .map(FunctionUtils::getConnectionValueFromAnnotation).orElse(StringUtils.EMPTY);
        final SqlDatabase targetDatabase = CosmosDBDatabaseNameCompletionProvider.getConnectedDatabase(annotation);
        if (Objects.isNull(targetDatabase)) {
            final String message = String.format("Could not connect to database `%s` with connection `%s`", databaseName, connection);
            holder.newAnnotation(HighlightSeverity.WARNING, message).range(element.getTextRange()).highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING).create();
        }
    }

    private void validateContainer(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        final SqlDatabase database = CosmosDBDatabaseNameCompletionProvider.getConnectedDatabase(annotation);
        if (Objects.isNull(database)) {
            return;
        }
        final PsiLiteralExpression literal = (PsiLiteralExpression) element.getParent();
        final String containerName = literal.getValue() instanceof String ? (String) literal.getValue() : StringUtils.EMPTY;
        if (StringUtils.isBlank(containerName)) {
            holder.newAnnotation(HighlightSeverity.WARNING, "ContainerName could not be empty")
                    .range(element.getTextRange()).highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING).create();
        }
        final SqlContainer container = database.containers().get(containerName, database.getResourceGroupName());
        if (Objects.isNull(container)) {
            final String message = String.format("Could not find container `%s` in database `%s`", containerName, database.getName());
            holder.newAnnotation(HighlightSeverity.WARNING, message).range(element.getTextRange()).highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING).create();
        }
    }
}
