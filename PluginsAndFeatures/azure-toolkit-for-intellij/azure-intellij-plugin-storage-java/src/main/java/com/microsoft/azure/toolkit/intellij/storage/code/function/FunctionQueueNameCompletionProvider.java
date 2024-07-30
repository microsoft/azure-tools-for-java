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
import com.microsoft.azure.toolkit.lib.storage.queue.Queue;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.connector.code.Utils.getConnectedResources;

public class FunctionQueueNameCompletionProvider extends CompletionProvider<CompletionParameters> {
    public static final String[] QUEUE_ANNOTATIONS = new String[]{
            "com.microsoft.azure.functions.annotation.QueueTrigger",
            "com.microsoft.azure.functions.annotation.QueueOutput"
    };
    public static final PsiElementPattern<?, ?> QUEUE_NAME_PAIR_PATTERN = PsiJavaPatterns.psiNameValuePair().withName("queueName").withParent(
            PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
                    PsiJavaPatterns.psiAnnotation().with(new PatternCondition<>("queueAnnotation") {
                        @Override
                        public boolean accepts(@NotNull final PsiAnnotation psiAnnotation, final ProcessingContext context) {
                            return StringUtils.equalsAnyIgnoreCase(psiAnnotation.getQualifiedName(), QUEUE_ANNOTATIONS);
                        }
                    })));
    public static final PsiJavaElementPattern<?, ?> QUEUE_NAME_PATTERN = psiElement().withParent(PsiLiteralExpression.class).withSuperParent(2, QUEUE_NAME_PAIR_PATTERN);

    static {
        FunctionAnnotationTypeHandler.registerKeyPairPattern(QUEUE_NAME_PAIR_PATTERN);
        FunctionAnnotationCompletionConfidence.registerCodeCompletionPattern(QUEUE_NAME_PATTERN);
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
        accounts.stream().flatMap(a -> a.getQueueModule().list().stream())
                .filter(queue -> StringUtils.startsWithIgnoreCase(queue.getName(), fullPrefix))
                .map(queue -> createLookupElement(queue, module))
                .forEach(result::addElement);
        AzureTelemeter.log(AzureTelemetry.Type.OP_END, OperationBundle.description("boundary/connector.complete_queue_name"));
    }

    private LookupElement createLookupElement(Queue queue, Module module) {
        return LookupElementBuilder.create(queue.getName())
                .withBoldness(true)
                .withInsertHandler(new FunctionAnnotationValueInsertHandler(false, FunctionBlobPathCompletionProvider.getAdditionalPropertiesFromCompletion(queue.getParent(), module)))
                .withCaseSensitivity(false)
                .withTypeText("Queue")
                .withTailText(" " + queue.getParent().getName())
                .withIcon(IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.QUEUES));
    }
}
