/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiFile;

public class PropertiesCompletionContributor extends CompletionContributor {
    public static final ElementPattern<? extends PsiFile> APPLICATION_PROPERTIES_FILE = PlatformPatterns.psiFile(PropertiesFileImpl.class).withName(StandardPatterns.string().startsWith("application"));

    public PropertiesCompletionContributor() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PropertiesTokenTypes.KEY_CHARACTERS).inFile(APPLICATION_PROPERTIES_FILE), new PropertiesKeyCompletionProvider());
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PropertiesTokenTypes.KEY_VALUE_SEPARATOR).inFile(APPLICATION_PROPERTIES_FILE), new PropertiesValueCompletionProvider());
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PropertiesTokenTypes.VALUE_CHARACTERS).inFile(APPLICATION_PROPERTIES_FILE), new PropertiesValueCompletionProvider());
    }
}
