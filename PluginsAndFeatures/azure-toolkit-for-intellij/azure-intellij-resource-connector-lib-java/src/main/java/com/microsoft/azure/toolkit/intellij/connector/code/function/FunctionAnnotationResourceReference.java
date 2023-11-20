/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.projectexplorer.AbstractAzureFacetNode;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

public class FunctionAnnotationResourceReference extends PsiReferenceBase<PsiElement> {
    private static final BiFunction<PsiAnnotation, Connection<?, ?>, AzResource> DEFAULT_RESOURCE_FUNCTION =
            (element, connection) -> Optional.ofNullable(connection).map(Connection::getResource)
                    .map(Resource::getData)
                    .filter(AzResource.class::isInstance)
                    .map(AzResource.class::cast).orElse(null);

    private final BiFunction<PsiAnnotation, Connection<?, ?>, ? extends AzResource> resourceFunction;

    public FunctionAnnotationResourceReference(@Nonnull final PsiElement element, @Nonnull TextRange rangeInElement) {
        this(element, rangeInElement, DEFAULT_RESOURCE_FUNCTION);
    }

    public FunctionAnnotationResourceReference(@Nonnull final PsiElement element, @Nonnull TextRange rangeInElement,
                                               @Nonnull BiFunction<PsiAnnotation, Connection<?, ?>, ? extends AzResource> resourceFunction) {
        super(element, rangeInElement);
        this.resourceFunction = resourceFunction;
    }

    @Override
    public @Nullable PsiElement resolve() {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return null;
        }
        final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(getElement(), PsiAnnotation.class);
        final Connection<?, ?> connection = Optional.ofNullable(annotation)
                .map(FunctionUtils::getConnectionFromAnnotation).orElse(null);
        if (Objects.isNull(annotation) || Objects.isNull(connection)) {
            return null;
        }
        final AzResource resource = resourceFunction.apply(annotation, connection);
        return Optional.ofNullable(resource).map(r -> new AzureResourcePsiElement(r, connection)).orElse(null);
    }


    @AllArgsConstructor
    class AzureResourcePsiElement extends FakePsiElement implements SyntheticElement {
        private final AzResource resource;
        private final Connection<?, ?> connection;

        @Override
        public PsiElement getParent() {
            return myElement;
        }

        @Override
        @AzureOperation("user/connector.navigate_to_resource_from_function_string_literal")
        public void navigate(boolean requestFocus) {
            AbstractAzureFacetNode.selectConnectedResource(connection, resource.getId(), requestFocus);
        }

        @Override
        public String getPresentableText() {
            return resource.getName();
        }

        @Override
        public String getName() {
            return resource.getName();
        }

        @Override
        public @Nullable String getLocationString() {
            final ResourceId resourceId = ResourceId.fromString(resource.getId());
            return Optional.ofNullable(resourceId).map(ResourceId::parent).map(ResourceId::id).orElseGet(() -> resourceId.resourceGroupName());
        }

        @Override
        public @Nullable Icon getIcon(final boolean open) {
            return IntelliJAzureIcons.getIcon(AzureResourceIconProvider.getResourceIconPath(resource));
        }

        @Override
        public TextRange getTextRange() {
            final TextRange rangeInElement = getRangeInElement();
            final TextRange elementRange = myElement.getTextRange();
            return elementRange != null ? rangeInElement.shiftRight(elementRange.getStartOffset()) : rangeInElement;
        }
    }
}
