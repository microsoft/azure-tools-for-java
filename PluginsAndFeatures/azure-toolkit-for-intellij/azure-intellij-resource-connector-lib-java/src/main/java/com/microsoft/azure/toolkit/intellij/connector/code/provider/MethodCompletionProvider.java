/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.CompletionMetadataManager;
import com.microsoft.azure.toolkit.intellij.connector.code.model.MethodIdentifier;
import com.microsoft.azure.toolkit.intellij.connector.code.model.ParameterIdentifier;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class MethodCompletionProvider extends AbstractResourceConnectionCompletionProvider {

    private static final Map<ParameterIdentifier, MethodCompletionMetadata> COMPLETION_METADATA = new HashMap<>();

    static {
        // fill meta data
        COMPLETION_METADATA.putAll(CompletionMetadataManager.getInputProviders().stream()
                .flatMap(provider -> provider.getMethodCompletionMetadata().stream())
                .collect(Collectors.toMap(MethodCompletionMetadata::getIdentifier, metadata -> metadata)));
    }

    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        try {
            final PsiElement position = parameters.getPosition();
            final PsiCallExpression callExpression = getParentCallExpression(position);
            final PsiMethod method = Optional.ofNullable(callExpression).map(PsiCallExpression::resolveMethod).orElse(null);
            if (Objects.isNull(method)) {
                return;
            }
            final ParameterIdentifier parameterIdentifier = parseParameterIdentifier(position, callExpression, method);
            final MethodCompletionMetadata metadata = COMPLETION_METADATA.get(parameterIdentifier);
            if (Objects.isNull(metadata)) {
                return;
            }
            addConnectedResourceCompletions(parameters, metadata, result);
            addCreateConnectionCompletions(parameters, metadata, result);
        } catch (final Exception e) {
            // swallow exception to avoid breaking code completion, just record them
            e.printStackTrace();
        }
    }

    private void addConnectedResourceCompletions(@Nonnull CompletionParameters parameters, MethodCompletionMetadata metadata, CompletionResultSet result) {
        final Module module = ModuleUtil.findModuleForPsiElement(parameters.getPosition());
        metadata.getConnectedResourcesFunction().apply(module).stream()
                .map(con -> metadata.getCompletionItemsFunction().apply(con))
                .flatMap(Collection::stream)
                .forEach(item -> result.addElement(toLookupElement(item)));
    }

    private void addCreateConnectionCompletions(@Nonnull final CompletionParameters parameters,
                                                @Nonnull final MethodCompletionMetadata metadata, @Nonnull final CompletionResultSet result) {
        if (Objects.isNull(metadata.getAzureResourcesFunction()) || Objects.isNull(metadata.getResourceDefinition())) {
            return;
        }
        final Module module = ModuleUtil.findModuleForPsiElement(parameters.getPosition());
        final List<Connection<? extends AzResource, ?>> connections = metadata.getConnectedResourcesFunction().apply(module);
        final List<? extends AzResource> resources = metadata.getAzureResourcesFunction().apply(module)
                .stream().filter(resource -> connections.stream().noneMatch(connection -> connection.getResource().getData().equals(resource)))
                .toList();
        resources.stream()
                .map(resource -> getCreateConnectionElement(module, resource, metadata.getResourceDefinition(), metadata.getCompletionItemsFunction()))
                .forEach(result::addElement);
    }

    private ParameterIdentifier parseParameterIdentifier(PsiElement position, PsiCallExpression callExpression, PsiMethod method) {
        final int parameterIndex = getParameterLocation(position, callExpression);
        final String name = method.getName();
        final String className = ((PsiClass) method.getParent()).getQualifiedName();
        final List<String> parameterTypes = Arrays.stream(method.getParameterList().getParameters())
                .map(PsiParameter::getType).map(PsiType::getCanonicalText)
                .collect(Collectors.toList());
        final MethodIdentifier methodIdentifier = MethodIdentifier.builder()
                .className(className).methodName(name).parameterTypes(parameterTypes).build();
        return ParameterIdentifier.builder().method(methodIdentifier).parameterIndex(parameterIndex).build();
    }

    private int getParameterLocation(final PsiElement position, final PsiCallExpression expression) {
        final PsiExpressionList argumentList = expression.getArgumentList();
        int result = 0;
        for (final PsiElement element : Objects.requireNonNull(argumentList).getChildren()) {
            if (element instanceof PsiJavaToken && element.textContains(',')) {
                result++;
            }
            if (PsiTreeUtil.isAncestor(element, position, true)) {
                return result;
            }
        }
        return 0;
    }

    @Nullable
    private PsiCallExpression getParentCallExpression(@Nullable final PsiElement element) {
        return Objects.isNull(element) ? null :
                element instanceof PsiCallExpression ? (PsiCallExpression) element : getParentCallExpression(element.getParent());
    }
}
