/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

public class JavaResourceConnectionAnnotator extends AbstractResourceConnectionAnnotator {
    @Override
    protected String extractVariableValue(@Nonnull PsiElement element) {
        return StringUtils.strip(element.getText(), "\"");
    }

    @Override
    protected boolean shouldAccept(@Nonnull PsiElement element) {
        return element instanceof PsiJavaToken && Objects.equals(((PsiJavaToken) element).getTokenType(), JavaTokenType.STRING_LITERAL);
    }
}
