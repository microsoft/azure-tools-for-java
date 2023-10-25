/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.annotator.ChangeEnvironmentVariableFix;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionConnectionCompletionProvider;
import com.microsoft.azure.toolkit.intellij.storage.completion.resource.function.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StorageFunctionConnectionAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // get correspond storage account
        if (!FunctionConnectionCompletionProvider.STORAGE_ANNOTATION_CONNECTION_PATTERN.accepts(element)) {
            return;
        }
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        final StorageAccount storageAccount = Optional.ofNullable(annotation)
                .map(FunctionUtils::getBindingStorageAccount)
                .orElse(null);
        final PsiAnnotationMemberValue connectionValue = Optional.ofNullable(annotation)
                .map(a -> a.findAttributeValue("connection"))
                .orElseGet(() -> annotation.findAttributeValue("value"));
        if (Objects.isNull(storageAccount)) {
            final AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.ERROR,
                    String.format("Could not find storage account for connection '%s'", element.getText()));
            builder.range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            addChangeEnvironmentVariableFix(element, builder);
            addCreateNewConnectionFix(element, builder);
            builder.create();
        }
    }

    private void addCreateNewConnectionFix(@Nonnull final PsiElement element, @Nonnull final AnnotationBuilder builder) {
        builder.withFix(new IntentionAction() {

            @Override
            public @IntentionName @NotNull String getText() {
                return "Create New Connection";
            }

            @Override
            public @NotNull @IntentionFamilyName String getFamilyName() {
                return "Azure Function Fixes";
            }

            @Override
            public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
                return true;
            }

            @Override
            public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                final Module module = ModuleUtil.findModuleForPsiElement(element);
                final var dialog = new ConnectorDialog(project);
                dialog.setConsumer(new ModuleResource(module.getName()));
                dialog.setResourceDefinition(StorageAccountResourceDefinition.INSTANCE);
                if (dialog.showAndGet()) {
                    final Connection<?, ?> c = dialog.getValue();
                    WriteCommandAction.runWriteCommandAction(project, (ThrowableComputable<PsiElement, RuntimeException>) () ->
                            element.replace(JavaPsiFacade.getElementFactory(project).createExpressionFromText(String.format("\"%s\"", c.getEnvPrefix()), element.getContainingFile())));
                }
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        });
    }

    private void addChangeEnvironmentVariableFix(PsiElement element, AnnotationBuilder builder) {
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        final Profile profile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        final List<Connection<?, ?>> storageConnections = profile.getConnections().stream()
                .filter(c -> Objects.equals(c.getResource().getDefinition(), StorageAccountResourceDefinition.INSTANCE))
                .collect(Collectors.toList());
        final String originalValue = element.getText().replace("\"", "");
        final SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(element);
        storageConnections.stream().forEach(connection -> builder.withFix(new ChangeEnvironmentVariableFix(originalValue, connection.getEnvPrefix(), pointer)));
    }
}
