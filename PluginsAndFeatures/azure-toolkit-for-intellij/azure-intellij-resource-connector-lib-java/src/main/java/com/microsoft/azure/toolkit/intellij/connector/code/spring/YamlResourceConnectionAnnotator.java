/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.intellij.psi.PsiElement;
import com.microsoft.azure.toolkit.intellij.connector.code.Utils;
import com.microsoft.azure.toolkit.intellij.connector.code.AbstractResourceConnectionAnnotator;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import javax.annotation.Nonnull;

public class YamlResourceConnectionAnnotator extends AbstractResourceConnectionAnnotator {
    @Override
    protected String extractVariableValue(@Nonnull PsiElement element) {
        return Utils.extractVariableFromSpringProperties(element.getText());
    }

    @Override
    protected boolean shouldAccept(@Nonnull PsiElement element) {
        return element instanceof YAMLPlainTextImpl;
    }
}
