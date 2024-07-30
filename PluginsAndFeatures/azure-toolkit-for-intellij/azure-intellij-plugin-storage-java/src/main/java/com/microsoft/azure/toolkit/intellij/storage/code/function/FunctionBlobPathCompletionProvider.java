/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.function;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
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
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationCompletionConfidence;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationTypeHandler;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionAnnotationValueInsertHandler;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.storage.code.spring.StoragePathCompletionContributor;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.connector.code.Utils.getConnectedResources;
import static com.microsoft.azure.toolkit.intellij.storage.code.spring.StoragePathCompletionProvider.*;

public class FunctionBlobPathCompletionProvider extends CompletionProvider<CompletionParameters> {
    public static final String[] BLOB_ANNOTATIONS = new String[]{
            "com.microsoft.azure.functions.annotation.BlobTrigger",
            "com.microsoft.azure.functions.annotation.BlobInput",
            "com.microsoft.azure.functions.annotation.BlobOutput"
    };
    public static final PsiElementPattern<?, ?> BLOB_PATH_PAIR_PATTERN = PsiJavaPatterns.psiNameValuePair().withName("path").withParent(
            PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
                    PsiJavaPatterns.psiAnnotation().with(new PatternCondition<>("blobAnnotation") {
                        @Override
                        public boolean accepts(@NotNull final PsiAnnotation psiAnnotation, final ProcessingContext context) {
                            return StringUtils.equalsAnyIgnoreCase(psiAnnotation.getQualifiedName(), BLOB_ANNOTATIONS);
                        }
                    })));
    public static final PsiJavaElementPattern<?, ?> BLOB_PATH_PATTERN = psiElement().withParent(PsiLiteralExpression.class).withSuperParent(2, BLOB_PATH_PAIR_PATTERN);

    static {
        FunctionAnnotationTypeHandler.registerKeyPairPattern(BLOB_PATH_PAIR_PATTERN);
        FunctionAnnotationCompletionConfidence.registerCodeCompletionPattern(BLOB_PATH_PATTERN);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
        if (Objects.isNull(module) || !Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiAnnotation.class);
        final IStorageAccount account = Optional.ofNullable(annotation).map(Utils::getBindingStorageAccount).orElse(null);
        final String connection = FunctionUtils.getConnectionValueFromAnnotation(annotation);
        if (Objects.isNull(account) && StringUtils.isNotBlank(connection)) {
            return;
        }
        final PsiLiteralExpression literal = (PsiLiteralExpression) element.getParent();
        final String value = literal.getValue() instanceof String ? (String) literal.getValue() : StringUtils.EMPTY;
        final String fullPrefix = StringUtils.substringBefore(value, StoragePathCompletionContributor.DUMMY_IDENTIFIER);
        final List<IStorageAccount> accountsToSearch = Objects.nonNull(account) ? List.of(account) : getConnectedResources(module, StorageAccountResourceDefinition.INSTANCE);
        final List<? extends StorageFile> files = getFiles("azure-blob://" + fullPrefix, accountsToSearch);
        final BiFunction<StorageFile, String, LookupElementBuilder> builder = (file, title) -> LookupElementBuilder.create(title)
                .withInsertHandler(new FunctionAnnotationValueInsertHandler(title.endsWith("/"), getAdditionalPropertiesFromCompletion(getStorageAccount(file), module)))
                .withBoldness(true)
                .withCaseSensitivity(false)
                .withTypeText(file.getResourceTypeName())
                .withTailText(" " + Optional.ofNullable(getStorageAccount(file)).map(IStorageAccount::getName).orElse(""))
                .withIcon(IntelliJAzureIcons.getIcon(getFileIcon(file)));
        for (final StorageFile file : files) {
            result.addElement(builder.apply(file, file.getName()));
            if (file.isDirectory()) {
                result.addElement(builder.apply(file, file.getName() + "/"));
            }
        }
        AzureTelemeter.log(AzureTelemetry.Type.OP_END, OperationBundle.description("boundary/connector.complete_blob_path"));
    }

    public static Map<String, String> getAdditionalPropertiesFromCompletion(@Nullable final IStorageAccount account, @Nonnull final Module module) {
        final Connection<?, ?> connection = Objects.isNull(account) ? null :
            Utils.getConnectionWithStorageAccount(account, module).stream().findFirst().orElse(null);
        return connection == null ? Collections.emptyMap() : Collections.singletonMap("connection", connection.getEnvPrefix());
    }
}
