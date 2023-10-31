/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.toolkit.intellij.connector.code.Utils;
import com.microsoft.azure.toolkit.intellij.connector.code.AbstractResourceConnectionAnnotator;

import javax.annotation.Nonnull;

public class PropertiesResourceConnectionAnnotator extends AbstractResourceConnectionAnnotator {
    @Override
    protected String extractVariableValue(@Nonnull PsiElement element) {
        return Utils.extractVariableFromSpringProperties(element.getText());
    }

    @Override
    protected boolean shouldAccept(@Nonnull PsiElement element) {
        return element instanceof PropertyValueImpl;
    }
}
