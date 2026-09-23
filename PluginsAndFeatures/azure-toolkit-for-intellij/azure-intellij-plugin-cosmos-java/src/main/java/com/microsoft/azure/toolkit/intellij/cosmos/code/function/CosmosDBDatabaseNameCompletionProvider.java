/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.code.function;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.code.Utils;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationCompletionConfidence;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationTypeHandler;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationValueInsertHandler;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.SqlCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.AzureCosmosDBFunctionAnnotationCompletionContributor.COSMOS_ANNOTATIONS;

public class CosmosDBDatabaseNameCompletionProvider extends CompletionProvider<CompletionParameters> {

    public static final PsiElementPattern<?, ?> COSMOS_DATABASE_NAME_PAIR_PATTERN = PsiJavaPatterns.psiNameValuePair().withName("databaseName").withParent(
            psiElement(PsiAnnotationParameterList.class).withParent(
                    PsiJavaPatterns.psiAnnotation().with(new PatternCondition<>("cosmosAnnotation") {
                        @Override
                        public boolean accepts(@NotNull final PsiAnnotation psiAnnotation, final ProcessingContext context) {
                            return StringUtils.equalsAnyIgnoreCase(psiAnnotation.getQualifiedName(), COSMOS_ANNOTATIONS);
                        }
                    })));
    public static final PsiElementPattern<?, ?> COSMOS_DATABASE_PATTERN = psiElement().withParent(PsiLiteralExpression.class).withSuperParent(2, COSMOS_DATABASE_NAME_PAIR_PATTERN);

    static {
        FunctionAnnotationTypeHandler.registerKeyPairPattern(COSMOS_DATABASE_NAME_PAIR_PATTERN);
        FunctionAnnotationCompletionConfidence.registerCodeCompletionPattern(COSMOS_DATABASE_PATTERN);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiAnnotation.class);
        if (Objects.isNull(module) || Objects.isNull(annotation) || !Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final String connectionValue = FunctionUtils.getConnectionValueFromAnnotation(annotation);
        final Connection<?, ?> connection = FunctionUtils.getConnectionFromAnnotation(annotation);
        final SqlDatabase database = (SqlDatabase) Optional.ofNullable(connection).map(Connection::getResource)
                .map(Resource::getData).filter(data -> data instanceof SqlDatabase).orElse(null);
        if (Objects.isNull(database) && StringUtils.isNotBlank(connectionValue)) {
            return;
        }
        final List<SqlDatabase> accountsToSearch = Objects.nonNull(database) ? List.of(database) :
            Utils.getConnectedResources(module, SqlCosmosDBAccountResourceDefinition.INSTANCE);
        accountsToSearch.stream()
                .map(d -> createLookupElement(d, module))
                .forEach(result::addElement);
        AzureTelemeter.log(AzureTelemetry.Type.OP_END, OperationBundle.description("boundary/connector.complete_cosmos_database"));
    }

    private LookupElement createLookupElement(@Nonnull final SqlDatabase database, Module module) {
        final Connection<?, ?> connection = Utils.getConnectionWithResource(module, database);
        final Map<String, String> properties = connection == null ? Collections.emptyMap() :
                Collections.singletonMap("connection", connection.getEnvPrefix());
        return LookupElementBuilder.create(database.getName())
                .withInsertHandler(new FunctionAnnotationValueInsertHandler(false, properties))
                .withBoldness(true)
                .withCaseSensitivity(false)
                .withTypeText(database.getResourceTypeName())
                .withTailText(" " + database.getParent().getName())
                .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.Cosmos.MODULE));
    }

    @Nullable
    public static SqlDatabase getConnectedDatabase(@Nullable final PsiAnnotation annotation) {
        if (Objects.isNull(annotation) || Objects.isNull(annotation.findAttribute("databaseName"))) {
            return null;
        }
        final Connection<?, ?> connection = FunctionUtils.getConnectionFromAnnotation(annotation);
        final SqlDatabase database = (SqlDatabase) Optional.ofNullable(connection).map(Connection::getResource)
                .map(Resource::getData).filter(data -> data instanceof SqlDatabase).orElse(null);
        if (Objects.isNull(database)) {
            return null;
        }
        final String databaseName = Optional.ofNullable(annotation.findAttributeValue("databaseName")).map(PsiElement::getText).map(text -> text.replace("\"", "")).orElse(StringUtils.EMPTY);
        return ((SqlCosmosDBAccount) database.getParent()).sqlDatabases().get(databaseName, database.getResourceGroupName());
    }
}
