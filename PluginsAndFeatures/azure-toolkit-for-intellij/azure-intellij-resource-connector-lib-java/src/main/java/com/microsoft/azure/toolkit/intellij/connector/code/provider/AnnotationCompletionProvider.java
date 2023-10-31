/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.CompletionMetadataManager;
import com.microsoft.azure.toolkit.intellij.connector.code.model.AnnotationIdentifier;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AnnotationCompletionProvider extends AbstractResourceConnectionCompletionProvider {

    private static final Map<AnnotationIdentifier, AnnotationCompletionMetadata> COMPLETION_METADATA = new HashMap<>();

    static {
        // fill meta data
        COMPLETION_METADATA.putAll(CompletionMetadataManager.getInputProviders().stream()
                .flatMap(provider -> provider.getAnnotationCompletionMetadata().stream())
                .collect(Collectors.toMap(AnnotationCompletionMetadata::getIdentifier, metadata -> metadata)));
    }

    @Override
    protected void addCompletions(@Nonnull CompletionParameters param, @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        try {
            final PsiElement element = param.getPosition();
            final PsiAnnotation annotation = getParentAnnotation(element);
            if (Objects.isNull(annotation)) {
                return;
            }
            final AnnotationIdentifier identifier = getAnnotationIdentifier(element, annotation);
            final AnnotationCompletionMetadata metadata = Optional.ofNullable(identifier).map(COMPLETION_METADATA::get).orElse(null);
            if (Objects.isNull(metadata)) {
                return;
            }
            final Module module = ModuleUtil.findModuleForPsiElement(element);
            final Map<String, String> parameters = getParameters(element, annotation);
            addConnectedResourceCompletions(module, parameters, metadata, result);
            addCreateConnectionCompletions(module, parameters, metadata, result);
        } catch (final Exception e) {
            // swallow exception to avoid breaking code completion, just record them
            e.printStackTrace();
        }
    }

    private void addConnectedResourceCompletions(@Nonnull final Module module, @Nonnull final Map<String, String> parameters,
                                                 @Nonnull final AnnotationCompletionMetadata metadata, @Nonnull final CompletionResultSet result) {
        metadata.getConnectedResourcesFunction().apply(module).stream()
                .map(con -> metadata.getCompletionItemsFunction().apply(con, parameters))
                .flatMap(Collection::stream)
                .forEach(item -> result.addElement(toLookupElement(item)));
    }

    private void addCreateConnectionCompletions(@Nonnull final Module module, @Nonnull final Map<String, String> parameters,
                                                @Nonnull final AnnotationCompletionMetadata metadata, @Nonnull final CompletionResultSet result) {
        if (Objects.isNull(metadata.getAzureResourcesFunction())) {
            return;
        }
        final List<Connection<? extends AzResource, ?>> connections = metadata.getConnectedResourcesFunction().apply(module);
        final List<? extends AzResource> resources = metadata.getAzureResourcesFunction().apply(module)
                .stream().filter(resource -> connections.stream().noneMatch(connection -> connection.getResource().getData().equals(resource)))
                .toList();
        resources.stream()
                .map(resource -> getCreateConnectionElement(module, resource, metadata.getResourceDefinition(),
                        connection -> metadata.getCompletionItemsFunction().apply(connection, null)))
                .forEach(result::addElement);
    }

    private Map<String, String> getParameters(@Nonnull final PsiElement element, @Nonnull final PsiAnnotation annotation) {
        final Function<PsiNameValuePair, String> parameterValueFunction = pair ->
                Objects.isNull(pair.getValue()) ? StringUtils.EMPTY : StringUtils.strip(StringUtils.replace(pair.getValue().getText(), INDICATOR, StringUtils.EMPTY), "\"");
        return Arrays.stream(annotation.getParameterList().getAttributes())
                .collect(Collectors.toMap(PsiNameValuePair::getName, parameterValueFunction));
    }

    @Nullable
    private AnnotationIdentifier getAnnotationIdentifier(@Nonnull final PsiElement element, @Nonnull final PsiAnnotation annotation) {
        final String qualifiedName = annotation.getQualifiedName();
        final String attributeName = Arrays.stream(annotation.getParameterList().getAttributes())
                .filter(pair -> PsiTreeUtil.isAncestor(pair, element, true))
                .findFirst()
                .map(PsiNameValuePair::getAttributeName).orElse(null);
        return StringUtils.isAnyEmpty(qualifiedName, attributeName) ? null : new AnnotationIdentifier(qualifiedName, attributeName);
    }

    @Nullable
    private PsiAnnotation getParentAnnotation(PsiElement element) {
        return Objects.isNull(element) ? null :
                element instanceof PsiAnnotation ? (PsiAnnotation) element : getParentAnnotation(element.getParent());
    }
}
