/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.code.ChangeEnvironmentVariableFix;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionConnectionCompletionProvider.FUNCTION_ANNOTATION_CONNECTION_PATTERN;

public class FunctionConnectionAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        // get correspond storage account
        if (!FUNCTION_ANNOTATION_CONNECTION_PATTERN.accepts(element)) {
            return;
        }
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        final PsiAnnotation annotation = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, PsiAnnotation.class), "Could not get function annotation");
        final FunctionSupported<?> definition = FunctionConnectionCompletionProvider.getResourceDefinition(annotation);
        final String connectionValue = FunctionUtils.getConnectionValueFromAnnotation(annotation);
        final Connection<?, ?> connection = FunctionUtils.getConnectionWithEnvPrefix(connectionValue, module);
        if (Objects.isNull(connection)) {
            final String message = StringUtils.isEmpty(connectionValue) ? "Connection could not be empty" :
                    String.format("Could not find connection with envPrefix '%s'", element.getText());
            final AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.ERROR, message);
            builder.range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            addChangeEnvironmentVariableFix(element, definition, builder);
            addCreateNewConnectionFix(element, definition, builder);
            builder.create();
        }
    }

    private void addCreateNewConnectionFix(@Nonnull final PsiElement element, FunctionSupported<?> definition, @Nonnull final AnnotationBuilder builder) {
        builder.withFix(new IntentionAction() {

            @Override
            public @IntentionName
            @Nonnull String getText() {
                return "Create New Connection";
            }

            @Override
            public @Nonnull
            @IntentionFamilyName String getFamilyName() {
                return "Azure Function Fixes";
            }

            @Override
            public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
                return true;
            }

            @Override
            public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                final Module module = ModuleUtil.findModuleForPsiElement(element);
                final var dialog = new ConnectorDialog(project);
                dialog.setConsumer(new ModuleResource(module.getName()));
                dialog.setResourceDefinition(definition);
                if (dialog.showAndGet()) {
                    final Connection<?, ?> c = dialog.getValue();
                    editor.getDocument().replaceString(element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset(), String.format("\"%s\"", c.getEnvPrefix()));
                    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
                }
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        });
    }

    private void addChangeEnvironmentVariableFix(PsiElement element, FunctionSupported<?> definition, AnnotationBuilder builder) {
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        final Profile profile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        final List<Connection<?, ?>> storageConnections = profile.getConnections().stream()
                .filter(c -> Objects.equals(c.getResource().getDefinition(), definition))
                .toList();
        final String originalValue = element.getText().replace("\"", "");
        final SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(element);
        storageConnections.forEach(connection -> builder.withFix(new ChangeEnvironmentVariableFix(originalValue, connection.getEnvPrefix(), pointer)));
    }
}
