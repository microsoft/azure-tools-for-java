/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.annotator;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.completion.function.FunctionUtils;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainer;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.cosmos.completion.CosmosDBContainerNameCompletionProvider.COSMOS_CONTAINER_PATTERN;
import static com.microsoft.azure.toolkit.intellij.cosmos.completion.CosmosDBDatabaseNameCompletionProvider.COSMOS_DATABASE_PATTERN;

public class CosmosDBFunctionPathAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (COSMOS_CONTAINER_PATTERN.accepts(element)) {
            validateContainer(element, holder);
        } else if (COSMOS_DATABASE_PATTERN.accepts(element)) {
            validateDatabase(element, holder);
        }
    }

    private void validateDatabase(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (Objects.isNull(annotation) || Objects.isNull(annotation.findAttribute("databaseName"))) {
            return;
        }
        final String connectionValue = FunctionUtils.getConnectionValueFromAnnotation(annotation);
        final Connection<?, ?> connection = FunctionUtils.getConnectionFromAnnotation(annotation);
        final SqlDatabase database = getSqlDatabaseFromConnection(connection);
        if (Objects.isNull(database)) {
            return;
        }
        final String databaseName = Optional.ofNullable(annotation.findAttributeValue("databaseName")).map(PsiElement::getText).map(text -> text.replace("\"", "")).orElse(StringUtils.EMPTY);
        final SqlDatabase targetDatabase = ((SqlCosmosDBAccount) database.getParent()).sqlDatabases().get(databaseName, database.getResourceGroupName());
        if (Objects.isNull(targetDatabase)) {
            final String message = StringUtils.isBlank(databaseName) ? "DatabaseName could not be empty" : String.format("Could not connect to database `%s` with connection `%s`", databaseName, connection.getEnvPrefix());
            holder.newAnnotation(HighlightSeverity.WARNING, message).range(element.getTextRange()).highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING).create();
        }
    }

    private void validateContainer(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (Objects.isNull(annotation) || Objects.isNull(annotation.findAttribute("databaseName")) || Objects.isNull(annotation.findAttribute("containerName"))) {
            return;
        }
        final String connectionValue = FunctionUtils.getConnectionValueFromAnnotation(annotation);
        final Connection<?, ?> connection = FunctionUtils.getConnectionFromAnnotation(annotation);
        final SqlDatabase database = getSqlDatabaseFromConnection(connection);
        if (Objects.isNull(database)) {
            return;
        }
        final String containerName = Optional.ofNullable(annotation.findAttributeValue("containerName")).map(PsiElement::getText).map(text -> text.replace("\"", "")).orElse(StringUtils.EMPTY);
        final SqlContainer targetContainer = database.containers().get(containerName, database.getResourceGroupName());
        if (Objects.isNull(targetContainer)) {
            final String message = StringUtils.isBlank(containerName) ? "ContainerName could not be empty" : String.format("Could not find container `%s` in database `%s`", containerName, database.getName());
            holder.newAnnotation(HighlightSeverity.WARNING, message).range(element.getTextRange()).highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING).create();
        }
    }

    @Nullable
    private static SqlDatabase getSqlDatabaseFromConnection(Connection<?, ?> connection) {
        return Optional.ofNullable(connection).map(Connection::getResource).map(Resource::getData).filter(data -> data instanceof SqlDatabase).map(data -> (SqlDatabase) data).orElse(null);
    }
}
