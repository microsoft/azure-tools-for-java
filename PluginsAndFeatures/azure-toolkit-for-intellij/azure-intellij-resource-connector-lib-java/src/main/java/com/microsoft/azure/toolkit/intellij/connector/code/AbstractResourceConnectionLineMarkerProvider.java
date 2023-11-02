/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractResourceConnectionLineMarkerProvider implements LineMarkerProvider {

    @Override
    @Nullable
    public LineMarkerInfo<?> getLineMarkerInfo(@Nonnull PsiElement element) {
        if (isAzureFacetEnabled(element) && shouldAccept(element)) {
            final Connection<?, ?> connection = getConnectionForPsiElement(element);
            final AzureServiceResource<?> resource = Optional.ofNullable(connection)
                    .filter(c -> c.getResource() instanceof AzureServiceResource)
                    .map(c -> ((AzureServiceResource<?>) c.getResource())).orElse(null);
            if (Objects.nonNull(resource)) {
                return new ResourceConnectionLineMarkerInfo(connection, resource, element);
            }
        }
        return null;
    }

    protected boolean isAzureFacetEnabled(@Nonnull PsiElement element){
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        return Optional.ofNullable(module).map(AzureModule::from).map(AzureModule::hasAzureFacet).orElse(false);
    }

    protected abstract boolean shouldAccept(@Nonnull final PsiElement element);

    @Nullable
    protected abstract Connection<?, ?> getConnectionForPsiElement(@Nonnull final PsiElement element);
}
