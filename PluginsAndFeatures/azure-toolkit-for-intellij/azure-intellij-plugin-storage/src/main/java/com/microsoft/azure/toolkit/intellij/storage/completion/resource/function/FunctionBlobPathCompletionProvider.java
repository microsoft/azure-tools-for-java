/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.completion.resource.function;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.*;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.AzureStorageJavaCompletionContributor;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.Utils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.intellij.patterns.PsiJavaPatterns.literalExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.AzureStorageResourceStringLiteralCompletionProvider.*;
import static com.microsoft.azure.toolkit.intellij.storage.completion.resource.Utils.getConnectionWithStorageAccount;

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

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
        if (Objects.isNull(module) || !Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiAnnotation.class);
        final StorageAccount account = Optional.ofNullable(annotation).map(FunctionUtils::getBindingStorageAccount).orElse(null);
        final String connection = Optional.ofNullable(annotation).map(a -> a.findAttributeValue("connection"))
                .map(value -> value.getText().replace("\"", "").trim()).orElse(null);
        if (Objects.isNull(account) && StringUtils.isNotBlank(connection)) {
            return;
        }
        final String fullPrefix = StringUtils.substringBefore(element.getText(), AzureStorageJavaCompletionContributor.DUMMY_IDENTIFIER).replace("\"", "").trim();
        final List<StorageAccount> accountsToSearch = Objects.nonNull(account) ? List.of(account) : Utils.getConnectedStorageAccounts(module);
        final List<? extends StorageFile> files = getFiles("azure-blob://" + fullPrefix, accountsToSearch);
        final BiFunction<StorageFile, String, LookupElementBuilder> builder = (file, title) -> LookupElementBuilder.create(title)
                .withInsertHandler(new MyInsertHandler(title.endsWith("/"), getStorageAccount(file)))
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

    @RequiredArgsConstructor
    public static class MyInsertHandler implements InsertHandler<LookupElement> {
        private final boolean popup;
        private final StorageAccount storageAccount;

        @Override
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement item) {
            if (popup) {
                AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
            }
            final PsiElement element = PsiUtil.getElementAtOffset(context.getFile(), context.getStartOffset());
            final boolean hasSpace = element.getPrevSibling() instanceof PsiWhiteSpace;
            // handle when insert not happen in string literal
            final String property = element.getText();
            if (!psiElement().inside(literalExpression()).accepts(element)) {
                final String newElementValue = (hasSpace ? StringUtils.EMPTY : StringUtils.SPACE) + String.format("\"%s\"", property);
                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), newElementValue);
                context.commitDocument();
            }
            final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
            final PsiAnnotationMemberValue value = Optional.ofNullable(annotation)
                    .map(a -> annotation.findAttributeValue("connection")).orElse(null);
            if (Objects.isNull(value) || Objects.isNull(storageAccount)) {
                return;
            }
            final String connectionValue = value.getText().replace("\"", "");
            if (StringUtils.isBlank(connectionValue)) {
                final Module module = ModuleUtil.findModuleForPsiElement(element);
                final Connection<?, ?> connection = Optional.ofNullable(module)
                        .flatMap(m -> getConnectionWithStorageAccount(storageAccount, m).stream().findFirst())
                        .orElse(null);
                if (Objects.nonNull(connection)) {
                    final String newConnectionValue = "\"" + connection.getEnvPrefix() + "\"";
                    value.replace(JavaPsiFacade.getElementFactory(context.getProject()).createExpressionFromText(newConnectionValue, context.getFile()));
                }
            }
        }
    }
}
