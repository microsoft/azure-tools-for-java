/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.java;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.psi.*;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.code.AnnotationFixes;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.storage.connection.BaseStorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition.TempData;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.ConnectionStringStorageAccount;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import static com.intellij.patterns.PsiJavaPatterns.*;
import static com.microsoft.azure.toolkit.intellij.connector.code.AbstractResourceConnectionAnnotator.isAzureFacetEnabled;

public class ConnectionStringStorageClientAnnotator implements Annotator {
    private static final PsiMethodPattern blobServiceClientConnectionString = psiMethod().withName("connectionString").definedInClass("com.azure.storage.blob.BlobServiceClientBuilder");
    private static final PsiMethodPattern shareServiceClientConnectionString = psiMethod().withName("connectionString").definedInClass("com.azure.storage.file.share.ShareServiceClientBuilder.ShareServiceClientBuilder");
    private static final PsiMethodPattern queueServiceClientConnectionString = psiMethod().withName("connectionString").definedInClass("com.azure.storage.queue.QueueServiceClientBuilder");
    public static final ElementPattern<PsiMethod> connectionStringMethods = PlatformPatterns.or(
        blobServiceClientConnectionString,
        shareServiceClientConnectionString,
        queueServiceClientConnectionString
    );

    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        final PsiJavaElementPattern.Capture<PsiElement> stringLiteralParam = psiElement(JavaTokenType.STRING_LITERAL).withParent(psiLiteral().methodCallParameter(0, connectionStringMethods));
        final PsiJavaElementPattern.Capture<PsiElement> identifierParam = psiElement(JavaTokenType.IDENTIFIER).withParent(psiReferenceExpression().methodCallParameter(0, connectionStringMethods));
        if (stringLiteralParam.accepts(element) || identifierParam.accepts(element)) {
            if (!isAzureFacetEnabled(element)) {
                return;
            }
            final StorageAccountResourceDefinition definition = StorageAccountResourceDefinition.INSTANCE;
            final boolean hasConnectionStringConnection = Optional.ofNullable(ModuleUtil.findModuleForPsiElement(element))
                .map(AzureModule::from)
                .stream().flatMap(m -> m.getConnectedResources(definition).stream())
                .anyMatch(s -> s instanceof ConnectionStringStorageAccount);
            if (!hasConnectionStringConnection) {
                final PsiElement parent = element.getParent();
                final TempData tempData = new TempData(BaseStorageAccountResourceDefinition.METHOD_STRING, null);
                if (parent instanceof PsiLiteralExpression) {
                    final PsiLiteralExpression literal = ((PsiLiteralExpression) parent);
                    final String connectionString = literal.getValue() instanceof String ? (String) literal.getValue() : element.getText();
                    tempData.setConnectionString(connectionString);
                    element = parent;
                }
                final String message = "Connect Azure storage using connection string and explore files.";
                final SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(element);
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, message)
                    .range(element.getTextRange())
                    .highlightType(ProblemHighlightType.WEAK_WARNING)
                    .withFix(AnnotationFixes.simple("Connect to this Azure Storage", new BiConsumer<>() {
                        @Override
                        @AzureOperation("user/connector.create_connection_quick_fix")
                        public void accept(final Editor editor, final PsiFile file) {
                            final Module module = ModuleUtil.findModuleForFile(file);
                            if (Objects.nonNull(module)) {
                                final var dialog = new ConnectorDialog(editor.getProject());
                                dialog.setConsumer(new ModuleResource(module.getName()));
                                definition.setTempData(tempData);
                                dialog.setResourceDefinition(definition);
                                if (dialog.showAndGet()) {
                                    final Connection<?, ?> c = dialog.getValue();
                                    replace(c, pointer, editor);
                                }
                            }
                        }
                    }))
                    .create();
            }
        }
    }

    private static void replace(final Connection<?, ?> connection, @Nonnull SmartPsiElementPointer<PsiElement> pointer, Editor editor) {
        final Project project = pointer.getProject();
        WriteCommandAction.runWriteCommandAction(project, () ->
            Optional.ofNullable(pointer.getElement()).map(PsiElement::getTextRange).ifPresent(range -> {
                final Document document = editor.getDocument();
                document.replaceString(range.getStartOffset(), range.getEndOffset(), String.format("System.getenv(\"%s_CONNECTION_STRING\")", connection.getEnvPrefix()));
                PsiDocumentManager.getInstance(project).commitDocument(document);
            })
        );
    }
}
