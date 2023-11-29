/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.code.spring;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.yaml.psi.impl.YAMLFileImpl;

import java.util.Objects;

public class EnvVarCharFilter extends CharFilter {
    @Override
    public Result acceptChar(char c, final int prefixLength, final Lookup lookup) {
        final PsiFile file = lookup.getPsiFile();
        final PsiElement ele = lookup.getPsiElement();
        if (Objects.isNull(ele) || !(EnvVarCompletionContributor.SPECIAL_CHARS.contains(c) && (file instanceof PropertiesFileImpl || file instanceof YAMLFileImpl || file instanceof PsiJavaFile))) {
            return null;
        }
        final String text = ele.getText();
        if (EnvVarCompletionContributor.KEYVAULT_SECRET_ENV_VAR_PLACES.accepts(ele) && (c == '$' || (text.endsWith("$") && c == '{'))) {
            return Result.ADD_TO_PREFIX;
        }
        return null;
    }
}
