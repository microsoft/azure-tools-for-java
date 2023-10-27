/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtilCore;
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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.Utils;
import com.microsoft.azure.toolkit.intellij.connector.completion.function.AzureFunctionAnnotationCompletionConfidence;
import com.microsoft.azure.toolkit.intellij.connector.completion.function.AzureFunctionAnnotationTypeHandler;
import com.microsoft.azure.toolkit.intellij.connector.completion.function.FunctionAnnotationValueInsertHandler;
import com.microsoft.azure.toolkit.intellij.connector.completion.function.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.SqlCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.cosmos.completion.AzureCosmosDBFunctionAnnotationCompletionContributor.COSMOS_ANNOTATIONS;

public class CosmosDBDatabaseNameCompletionProvider extends CompletionProvider<CompletionParameters> {

    public static final PsiElementPattern<?, ?> COSMOS_DATABASE_NAME_PAIR_PATTERN = PsiJavaPatterns.psiNameValuePair().withName("databaseName").withParent(
            psiElement(PsiAnnotationParameterList.class).withParent(
                    PsiJavaPatterns.psiAnnotation().with(new PatternCondition<>("cosmosAnnotation") {
                        @Override
                        public boolean accepts(@NotNull final PsiAnnotation psiAnnotation, final ProcessingContext context) {
                            return StringUtils.equalsAnyIgnoreCase(psiAnnotation.getQualifiedName(), COSMOS_ANNOTATIONS);
                        }
                    })));
    public static final PsiElementPattern<?, ?> COSMOS_DATABASE_PATTERN = psiElement().withSuperParent(2, COSMOS_DATABASE_NAME_PAIR_PATTERN);

    static {
        AzureFunctionAnnotationTypeHandler.registerKeyPairPattern(COSMOS_DATABASE_NAME_PAIR_PATTERN);
        AzureFunctionAnnotationCompletionConfidence.registerCodeCompletionPattern(COSMOS_DATABASE_PATTERN);
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
        final Object resource = Optional.ofNullable(FunctionUtils.getConnectionFromAnnotation(annotation))
                .map(Connection::getResource).map(Resource::getData).orElse(null);
        final SqlDatabase account = resource instanceof SqlDatabase ? (SqlDatabase) resource : null;
        if (Objects.isNull(account) && StringUtils.isNotBlank(connectionValue)) {
            return;
        }
        final String fullPrefix = StringUtils.substringBefore(element.getText(), CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED).replace("\"", "").trim();
        final List<SqlDatabase> accountsToSearch = Objects.nonNull(account) ? List.of(account) :
                Utils.getConnectedResources(module, SqlCosmosDBAccountResourceDefinition.INSTANCE);
        accountsToSearch.stream()
                .map(database -> createLookupElement(database, module))
                .forEach(result::addElement);
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
}
