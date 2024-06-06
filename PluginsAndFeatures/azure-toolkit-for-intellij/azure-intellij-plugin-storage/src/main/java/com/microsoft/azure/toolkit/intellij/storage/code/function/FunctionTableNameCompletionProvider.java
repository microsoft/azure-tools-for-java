/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.function;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.*;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationCompletionConfidence;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationTypeHandler;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationValueInsertHandler;
import com.microsoft.azure.toolkit.intellij.storage.code.spring.StoragePathCompletionContributor;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.table.Table;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.connector.code.Utils.getConnectedResources;

public class FunctionTableNameCompletionProvider extends CompletionProvider<CompletionParameters> {
    public static final String[] TABLE_ANNOTATIONS = new String[]{
            "com.microsoft.azure.functions.annotation.TableInput",
            "com.microsoft.azure.functions.annotation.TableOutput",
    };
    public static final PsiElementPattern<?, ?> TABLE_NAME_PAIR_PATTERN = PsiJavaPatterns.psiNameValuePair().withName("tableName").withParent(
            PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
                    PsiJavaPatterns.psiAnnotation().with(new PatternCondition<>("queueAnnotation") {
                        @Override
                        public boolean accepts(@NotNull final PsiAnnotation psiAnnotation, final ProcessingContext context) {
                            return StringUtils.equalsAnyIgnoreCase(psiAnnotation.getQualifiedName(), TABLE_ANNOTATIONS);
                        }
                    })));
    public static final PsiJavaElementPattern<?, ?> TABLE_NAME_PATTERN = psiElement().withParent(PsiLiteralExpression.class).withSuperParent(2, TABLE_NAME_PAIR_PATTERN);

    static {
        FunctionAnnotationTypeHandler.registerKeyPairPattern(TABLE_NAME_PAIR_PATTERN);
        FunctionAnnotationCompletionConfidence.registerCodeCompletionPattern(TABLE_NAME_PATTERN);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final PsiLiteralExpression literal = (PsiLiteralExpression) element.getParent();
        final String value = literal.getValue() instanceof String ? (String) literal.getValue() : StringUtils.EMPTY;
        final String fullPrefix = StringUtils.substringBefore(value, StoragePathCompletionContributor.DUMMY_IDENTIFIER);
        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
        if (Objects.isNull(module) || !Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiAnnotation.class);
        final IStorageAccount account = Optional.ofNullable(annotation).map(Utils::getBindingStorageAccount).orElse(null);
        final List<IStorageAccount> accounts = Objects.isNull(account) ? getConnectedResources(module, StorageAccountResourceDefinition.INSTANCE) : List.of(account);
        accounts.stream().flatMap(a -> a.getTableModule().list().stream())
                .filter(table -> StringUtils.startsWithIgnoreCase(table.getName(), fullPrefix))
                .map(queue -> createLookupElement(queue, module))
                .forEach(result::addElement);
        AzureTelemeter.log(AzureTelemetry.Type.OP_END, OperationBundle.description("boundary/connector.complete_table_name"));
    }

    private LookupElement createLookupElement(Table table, Module module) {
        return LookupElementBuilder.create(table.getName())
                .withBoldness(true)
                .withInsertHandler(new FunctionAnnotationValueInsertHandler(false, FunctionBlobPathCompletionProvider.getAdditionalPropertiesFromCompletion(table.getParent(), module)))
                .withCaseSensitivity(false)
                .withTypeText("Table")
                .withTailText(" " + table.getParent().getName())
                .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.TABLES));
    }
}
