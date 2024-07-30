/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiJavaFileImpl;

import java.util.Arrays;
import java.util.Objects;

public class StoragePathCharFilter extends CharFilter {
    @Override
    public Result acceptChar(char c, final int prefixLength, final Lookup lookup) {
        final PsiFile file = lookup.getPsiFile();
        if (!Arrays.asList(':', '-').contains(c) || !(file instanceof PsiJavaFileImpl)) {
            return null;
        }
        final PsiElement ele = lookup.getPsiElement();
        if (Objects.isNull(ele)) {
            return null;
        }
        final String text = ele.getText().replace("\"", "");
        if (StoragePathCompletionContributor.PREFIX_PLACES.accepts(ele)
            || text.startsWith("azure-blob") || text.startsWith("azure-file")) {
            return Result.ADD_TO_PREFIX;
        }
        return null;
    }
}
