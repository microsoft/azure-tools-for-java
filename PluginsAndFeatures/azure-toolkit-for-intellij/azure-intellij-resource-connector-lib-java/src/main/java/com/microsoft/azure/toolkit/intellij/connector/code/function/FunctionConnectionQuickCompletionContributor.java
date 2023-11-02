/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.function;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.code.LookupElements;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionConnectionCompletionContributor.FUNCTION_ANNOTATION_CONNECTION_PATTERN;

public class FunctionConnectionQuickCompletionContributor extends CompletionContributor implements DumbAware {

    public static final String APPLICATION = "application";

    public FunctionConnectionQuickCompletionContributor() {
        super();
        extend(null, FUNCTION_ANNOTATION_CONNECTION_PATTERN, new CompletionProvider<>() {
            @Override
            protected void addCompletions(@Nonnull final CompletionParameters parameters, @Nonnull final ProcessingContext context, @Nonnull final CompletionResultSet result) {
                final PsiElement element = parameters.getPosition();
                final PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
                final FunctionSupported<?> resourceDefinition = Optional.ofNullable(annotation)
                        .map(FunctionConnectionCompletionContributor::getResourceDefinition).orElse(null);
                if (Objects.isNull(resourceDefinition)) {
                    return;
                }
                if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                    AzureTelemeter.info("connector.not_signed_in.function_connection_code_completion");
                    result.addElement(LookupElements.buildSignInLookupElement());
                } else {
                    result.addElement(LookupElements.buildConnectLookupElement(resourceDefinition, FunctionConnectionCompletionContributor::onInsertConnection));
                }
            }
        });
    }
}
