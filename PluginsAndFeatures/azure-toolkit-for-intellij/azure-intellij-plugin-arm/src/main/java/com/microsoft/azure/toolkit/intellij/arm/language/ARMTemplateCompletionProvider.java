/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.language;

import com.intellij.json.psi.JsonProperty;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.json.psi.JsonValue;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class ARMTemplateCompletionProvider extends CompletionProvider<CompletionParameters> {
    public static final ARMTemplateCompletionProvider INSTANCE = new ARMTemplateCompletionProvider();
    private static final Set<String> KEYWORDS = Sets.newConcurrentHashSet(Arrays.asList("$schema", "contentVersion",
        "parameters", "variables", "resources", "outputs"));
    private static final Set<String> RESOURCES = Sets.newConcurrentHashSet(Arrays.asList("type", "apiVersion", "name",
        "location", "sku", "properties"));
    private static final Set<String> PARAMETERS = Sets.newConcurrentHashSet();
    private static final Set<String> VARIABLES = Sets.newConcurrentHashSet();
    private static final Set<String> OUTPUTS = Sets.newConcurrentHashSet();
    private static final String[] SCOPES = new String[]{"parameters", "variables", "resources", "outputs"};

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
        @NotNull CompletionResultSet result) {
        try {
            if (parameters.getOriginalFile().getLanguage().getID().equals(ARMTemplateLanguage.ID)) {
                PsiElement position = parameters.getPosition();
                if (isProperty(position)) {
                    Scope scope = findScope(position);
                    if (scope == Scope.root) {
                        for (String keyword : KEYWORDS) {
                            result.addElement(LookupElementBuilder.create(keyword).bold());
                        }
                    } else if (scope == Scope.resources) {
                        for (String resource : RESOURCES) {
                            result.addElement(LookupElementBuilder.create(resource).bold());
                        }
                    } else if (scope == Scope.variables) {
                        // todo
                    }
                } else {
                    // todo
                }
            }
        } catch (Exception ignore) {
        }
    }

    private boolean isProperty(PsiElement position) {
        final PsiElement parentElement = position.getParent().getParent().getOriginalElement();
        if (parentElement instanceof JsonProperty) {
            final String value = Optional.ofNullable(((JsonProperty) parentElement).getValue()).map(JsonValue::getText).orElse(null);
            return Objects.isNull(value) || !position.getText().equals(value);
        }
        return false;
    }

    private Scope findScope(PsiElement position) {
        try {
            // The json schema like: { "parameters":{}, "resources":{}, "variables": {} }
            // The target is to find the property is on which scope, here just use a while to find the root.
            // The 3 parent recursive is based on test...
            while (position.getParent().getParent().getParent() != null) {
                position = position.getParent();
            }

//            String positionName = ((JsonProperty) position).namespace();
//            return Scope.valueOf(positionName);
            return Scope.root;
        } catch (Exception ignore) {
            return Scope.root;
        }
    }

    enum Scope {
        parameters, variables, resources, outputs, root
    }
}
