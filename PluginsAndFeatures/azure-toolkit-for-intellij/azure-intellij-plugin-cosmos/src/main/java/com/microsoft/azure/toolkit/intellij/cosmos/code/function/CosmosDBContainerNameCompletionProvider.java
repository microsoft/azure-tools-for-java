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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.Utils;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationCompletionConfidence;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationTypeHandler;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationValueInsertHandler;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.SqlCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainer;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.cosmos.code.function.AzureCosmosDBFunctionAnnotationCompletionContributor.COSMOS_ANNOTATIONS;

public class CosmosDBContainerNameCompletionProvider extends CompletionProvider<CompletionParameters> {

    public static final PsiElementPattern<?, ?> COSMOS_CONTAINER_NAME_PAIR_PATTERN = PsiJavaPatterns.psiNameValuePair().withName("containerName").withParent(
            psiElement(PsiAnnotationParameterList.class).withParent(
                    PsiJavaPatterns.psiAnnotation().with(new PatternCondition<>("cosmosAnnotation") {
                        @Override
                        public boolean accepts(@NotNull final PsiAnnotation psiAnnotation, final ProcessingContext context) {
                            return StringUtils.equalsAnyIgnoreCase(psiAnnotation.getQualifiedName(), COSMOS_ANNOTATIONS);
                        }
                    })));
    public static final PsiElementPattern<?, ?> COSMOS_CONTAINER_PATTERN = psiElement().withSuperParent(2, COSMOS_CONTAINER_NAME_PAIR_PATTERN);

    static {
        FunctionAnnotationTypeHandler.registerKeyPairPattern(COSMOS_CONTAINER_NAME_PAIR_PATTERN);
        FunctionAnnotationCompletionConfidence.registerCodeCompletionPattern(COSMOS_CONTAINER_PATTERN);
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
        final String databaseValue = FunctionUtils.getPropertyValueFromAnnotation(annotation, "databaseName");
        final SqlDatabase database = CosmosDBDatabaseNameCompletionProvider.getConnectedDatabase(annotation);
        if (Objects.isNull(database) && (StringUtils.isNotBlank(connectionValue) || StringUtils.isNotBlank(databaseValue))) {
            return;
        }
        final List<SqlDatabase> databasesToSearch = Objects.nonNull(database) ? List.of(database) :
                Utils.getConnectedResources(module, SqlCosmosDBAccountResourceDefinition.INSTANCE);
        databasesToSearch.stream()
                .flatMap(db -> db.containers().list().stream())
                .map(container -> createLookupElement(container, module))
                .forEach(result::addElement);
    }

    private LookupElement createLookupElement(@Nonnull final SqlContainer container, Module module) {
        final Connection<?, ?> connection = Utils.getConnectionWithResource(module, container.getParent());
        final Map<String, String> properties = connection == null ? Collections.emptyMap() :
                Map.of("connection", connection.getEnvPrefix(), "databaseName", container.getParent().getName());
        return LookupElementBuilder.create(container.getName())
                .withInsertHandler(new FunctionAnnotationValueInsertHandler(false, properties))
                .withBoldness(true)
                .withCaseSensitivity(false)
                .withTypeText(container.getResourceTypeName())
                .withTailText(" " + container.getParent().getName())
                .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.Cosmos.MODULE));
    }
}
