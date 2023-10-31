/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JavaResourceConnectionLineMarkerProvider extends AbstractResourceConnectionLineMarkerProvider {

    @Override
    protected boolean shouldAccept(@Nonnull PsiElement element) {
        return element instanceof PsiJavaToken && Objects.equals(((PsiJavaToken) element).getTokenType(), JavaTokenType.STRING_LITERAL);
    }

    @Override
    protected Connection<? extends AzResource, ?> getConnectionForPsiElement(@Nonnull PsiElement element) {
        final String value = StringUtils.strip(element.getText(), "\"");
        final Module module = ModuleUtil.findModuleForPsiElement(element);
        return Utils.getConnectionWithEnvironmentVariable(module, value);
    }
}
