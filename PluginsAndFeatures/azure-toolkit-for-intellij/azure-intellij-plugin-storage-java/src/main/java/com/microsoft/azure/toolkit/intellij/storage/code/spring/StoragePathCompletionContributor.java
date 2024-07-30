/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;

import java.util.Arrays;
import java.util.List;

import static com.intellij.patterns.PsiJavaPatterns.*;

public class StoragePathCompletionContributor extends CompletionContributor {

    public static final String DUMMY_IDENTIFIER = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;
    public static final List<Character> SPECIAL_CHARS = Arrays.asList(':', '/', '-');

    private static final PsiJavaElementPattern.Capture<PsiElement> insideAnnotation = psiElement().insideAnnotationParam("org.springframework.beans.factory.annotation.Value");
    private static final PsiMethodPattern resourceLoaderGetResource = psiMethod().withName("getResource").definedInClass("org.springframework.core.io.ResourceLoader");
    private static final PsiMethodPattern azureStorageBlobProtocolResolverGetResource = psiMethod().withName("getResource").definedInClass("com.azure.spring.cloud.core.resource.AzureStorageBlobProtocolResolver ");
    private static final PsiMethodPattern azureStorageBlobProtocolResolverGetResources = psiMethod().withName("getResources").definedInClass("com.azure.spring.cloud.core.resource.AzureStorageBlobProtocolResolver ");
    private static final PsiMethodPattern azureStorageFileProtocolResolverGetResource = psiMethod().withName("getResource").definedInClass("com.azure.spring.cloud.core.resource.AzureStorageFileProtocolResolver");
    private static final PsiMethodPattern azureStorageFileProtocolResolverGetResources = psiMethod().withName("getResources").definedInClass("com.azure.spring.cloud.core.resource.AzureStorageFileProtocolResolver");

    public static final ElementPattern<? extends PsiElement> PREFIX_PLACES = PlatformPatterns.or(
        insideAnnotation,
        psiElement(JavaTokenType.STRING_LITERAL).withParent(psiLiteral().methodCallParameter(0, PlatformPatterns.or(
            resourceLoaderGetResource,
            azureStorageBlobProtocolResolverGetResource,
            azureStorageFileProtocolResolverGetResource,
            azureStorageBlobProtocolResolverGetResources,
            azureStorageFileProtocolResolverGetResources
        ))));

    public StoragePathCompletionContributor() {
        super();
        extend(null, PREFIX_PLACES, new StoragePathPrefixCompletionProvider());
        extend(null, psiElement(JavaTokenType.STRING_LITERAL).withParent(literalExpression()), new StoragePathCompletionProvider());
    }
}
