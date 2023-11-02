/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiEditorUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.AnnotationFixes;
import com.microsoft.azure.toolkit.intellij.connector.code.ChangeEnvironmentVariableFix;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.intellij.connector.code.AbstractResourceConnectionAnnotator.isAzureFacetEnabled;
import static com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionConnectionCompletionContributor.FUNCTION_ANNOTATION_CONNECTION_PATTERN;

public class FunctionConnectionAnnotator implements Annotator {
    @Override
    public void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        // get correspond storage account
        if (!(isAzureFacetEnabled(element) && FUNCTION_ANNOTATION_CONNECTION_PATTERN.accepts(element) &&
                Azure.az(AzureAccount.class).isLoggedIn())) {
            return;
        }
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        final PsiAnnotation annotation = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, PsiAnnotation.class), "Could not get function annotation");
        final FunctionSupported<?> definition = FunctionConnectionCompletionContributor.getResourceDefinition(annotation);
        final String connectionValue = FunctionUtils.getConnectionValueFromAnnotation(annotation);
        final Connection<?, ?> connection = FunctionUtils.getConnectionWithEnvPrefix(connectionValue, module);
        if (Objects.isNull(connection)) {
            final String message = StringUtils.isEmpty(connectionValue) ? "Connection could not be empty" :
                    String.format("Could not find connection with envPrefix '%s'", connectionValue);
            final AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.ERROR, message);
            //noinspection ResultOfMethodCallIgnored
            builder.range(element.getTextRange())
                    .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            addChangeEnvironmentVariableFix(element, definition, builder);
            addCreateNewConnectionFix(element, definition, builder);
            builder.create();
        }
    }

    private void addCreateNewConnectionFix(@Nonnull final PsiElement element, FunctionSupported<?> definition, @Nonnull final AnnotationBuilder builder) {
        final Consumer<Connection<?, ?>> consumer = connection -> {
            final Editor editor = PsiEditorUtil.findEditor(element);
            if (Objects.isNull(editor) || Objects.isNull(connection)) {
                return;
            }
            WriteCommandAction.runWriteCommandAction(element.getProject(), () -> {
                editor.getDocument().replaceString(element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset(), String.format("\"%s\"", connection.getEnvPrefix()));
                PsiDocumentManager.getInstance(element.getProject()).commitDocument(editor.getDocument());
            });
        };
        final PsiLiteralExpression parent = (PsiLiteralExpression) element.getParent();
        final String value = parent.getValue() instanceof String ? (String) parent.getValue() : StringUtils.EMPTY;
        //noinspection ResultOfMethodCallIgnored
        builder.withFix(AnnotationFixes.createNewConnection(definition, consumer, value));
    }

    private void addChangeEnvironmentVariableFix(PsiElement element, FunctionSupported<?> definition, AnnotationBuilder builder) {
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        final Profile profile = Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(profile)) {
            return;
        }
        final PsiLiteralExpression parent = (PsiLiteralExpression) element.getParent();
        final String value = parent.getValue() instanceof String ? (String) parent.getValue() : StringUtils.EMPTY;
        final List<Connection<?, ?>> storageConnections = profile.getConnections().stream()
                .filter(c -> Objects.equals(c.getResource().getDefinition(), definition))
                .collect(Collectors.toList());
        final SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(element);
        //noinspection ResultOfMethodCallIgnored
        storageConnections.forEach(connection -> builder.withFix(new ChangeEnvironmentVariableFix(value, connection.getEnvPrefix(), pointer)));
    }
}
