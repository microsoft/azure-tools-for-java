/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvaults.code.spring;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.keyvaults.secret.Secret;
import lombok.Getter;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.microsoft.azure.toolkit.intellij.connector.code.ResourceConnectionLineMarkerInfo.LINE_MARKER_NAVIGATE_TOOLTIP;

public class EnvVarLineMarkerProvider implements LineMarkerProvider {

    @Override
    @Nullable
    public LineMarkerInfo<?> getLineMarkerInfo(@Nonnull PsiElement element) {
        if (PlatformPatterns.or(psiElement(PropertyValueImpl.class), psiElement(LeafPsiElement.class).withParent(YAMLPlainTextImpl.class)).accepts(element)) {
            final Module module = ModuleUtil.findModuleForPsiElement(element);
            if (Objects.isNull(module)) {
                return null;
            }
            final List<EnvVarReference> references = EnvVarReferenceContributor.getEnvVarReferences(element);
            return references.stream()
                .flatMap(r -> Optional.ofNullable(r.resolve()).stream()).filter(Objects::nonNull).findFirst()
                .map(r -> new ResourceLineMarkerInfo(element, r))
                .orElse(null);
        }
        return null;
    }

    static class ResourceLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
        @Getter
        private final Secret secret;

        public ResourceLineMarkerInfo(final PsiElement element, @Nonnull final EnvVarReference.EnvVarPsiElement envVarEle) {
            super(element,
                envVarEle.getTextRange(),
                envVarEle.getIcon(true),
                ignore -> Azure.az(AzureAccount.class).isLoggedIn() ?
                    String.format(LINE_MARKER_NAVIGATE_TOOLTIP, envVarEle.getSecret().getResourceTypeName(), envVarEle.getName()) :
                    "Navigate to Secret in Project Explorer",
                null,
                (e, ele) -> envVarEle.navigate(true),
                GutterIconRenderer.Alignment.LEFT,
                envVarEle::getName);
            this.secret = envVarEle.getSecret();
        }

        @Override
        public boolean canMergeWith(@Nonnull MergeableLineMarkerInfo<?> info) {
            return info instanceof ResourceLineMarkerInfo &&
                Objects.equals(((ResourceLineMarkerInfo) info).secret, this.secret);
        }

        @Override
        public Icon getCommonIcon(@Nonnull List<? extends MergeableLineMarkerInfo<?>> infos) {
            return infos.stream()
                .filter(i -> i instanceof ResourceLineMarkerInfo)
                .map(i -> (ResourceLineMarkerInfo) i)
                .map(ResourceLineMarkerInfo::getIcon)
                .findFirst().orElse(null);
        }
    }
}
