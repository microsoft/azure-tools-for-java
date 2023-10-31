/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

public class PropertiesKeyCharFilter extends CharFilter {

    @Override
    public Result acceptChar(char c, final int prefixLength, final Lookup lookup) {
        final PsiFile file = lookup.getPsiFile();
        if (file == null || file.getFileType() != PropertiesFileType.INSTANCE) {
            return null;
        }
        final PsiElement element = lookup.getPsiElement();
        if (c == '.' && PlatformPatterns.psiElement(PropertiesTokenTypes.KEY_CHARACTERS).accepts(element)) {
            return Result.ADD_TO_PREFIX;
        }
        return null;
    }
}
