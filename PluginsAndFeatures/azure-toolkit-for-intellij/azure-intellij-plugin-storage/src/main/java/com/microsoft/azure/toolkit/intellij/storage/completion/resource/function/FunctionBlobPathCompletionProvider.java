/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource.function;

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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.completion.function.AzureFunctionAnnotationCompletionConfidence;
import com.microsoft.azure.toolkit.intellij.connector.completion.function.AzureFunctionAnnotationTypeHandler;
import com.microsoft.azure.toolkit.intellij.connector.completion.function.FunctionAnnotationValueInsertHandler;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.AzureStorageJavaCompletionContributor;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.Utils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.AzureStorageResourceStringLiteralCompletionProvider.*;

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
    public static final PsiJavaElementPattern<?, ?> BLOB_PATH_PATTERN = psiElement().withSuperParent(2, BLOB_PATH_PAIR_PATTERN);

    static {
        AzureFunctionAnnotationTypeHandler.registerKeyPairPattern(BLOB_PATH_PAIR_PATTERN);
        AzureFunctionAnnotationCompletionConfidence.registerCodeCompletionPattern(BLOB_PATH_PATTERN);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
        if (Objects.isNull(module) || !Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiAnnotation.class);
        final StorageAccount account = Optional.ofNullable(annotation).map(Utils::getBindingStorageAccount).orElse(null);
        final String connection = Optional.ofNullable(annotation).map(a -> a.findAttributeValue("connection"))
                .map(value -> value.getText().replace("\"", "").trim()).orElse(null);
        if (Objects.isNull(account) && StringUtils.isNotBlank(connection)) {
            return;
        }
        final String fullPrefix = StringUtils.substringBefore(element.getText(), AzureStorageJavaCompletionContributor.DUMMY_IDENTIFIER).replace("\"", "").trim();
        final List<StorageAccount> accountsToSearch = Objects.nonNull(account) ? List.of(account) : Utils.getConnectedStorageAccounts(module);
        final List<? extends StorageFile> files = getFiles("azure-blob://" + fullPrefix, accountsToSearch);
        final BiFunction<StorageFile, String, LookupElementBuilder> builder = (file, title) -> LookupElementBuilder.create(title)
                .withInsertHandler(new FunctionAnnotationValueInsertHandler(title.endsWith("/"), getAdditionalPropertiesFromCompletion(getStorageAccount(file), module)))
                .withBoldness(true)
                .withCaseSensitivity(false)
                .withTypeText(file.getResourceTypeName())
                .withTailText(" " + Optional.ofNullable(getStorageAccount(file)).map(AbstractAzResource::getName).orElse(""))
                .withIcon(IntelliJAzureIcons.getIcon(getFileIcon(file)));
        for (final StorageFile file : files) {
            result.addElement(builder.apply(file, file.getName()));
            if (file.isDirectory()) {
                result.addElement(builder.apply(file, file.getName() + "/"));
            }
        }
    }

    public static Map<String, String> getAdditionalPropertiesFromCompletion(@Nullable final StorageAccount account, @Nonnull final Module module) {
        final Connection<?, ?> connection = Objects.isNull(account) ? null :
                Utils.getConnectionWithStorageAccount(account, module).stream().findFirst().orElse(null);
        return connection == null ? Collections.emptyMap() : Collections.singletonMap("connection", connection.getEnvPrefix());
    }
}
