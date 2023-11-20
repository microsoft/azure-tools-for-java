package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.google.common.collect.ImmutableMap;
import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.intellij.connector.code.LookupElements;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLPsiElement;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public class YamlQuickCompletionContributor extends CompletionContributor implements DumbAware {

    public static final String APPLICATION = "application";

    public YamlQuickCompletionContributor() {
        super();
        extend(CompletionType.BASIC, YamlCompletionContributor.YAML_TEXT.withSuperParent(2, PsiJavaPatterns.psiElement(YAMLKeyValue.class)), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@Nonnull final CompletionParameters parameters, @Nonnull final ProcessingContext context, @Nonnull final CompletionResultSet result) {
                final PsiElement element = parameters.getPosition();
                final YAMLPsiElement yamlElement = PsiTreeUtil.getParentOfType(element, YAMLPsiElement.class);
                if (Objects.isNull(yamlElement)) {
                    return;
                }
                final String key = YAMLUtil.getConfigFullName(yamlElement);
                final List<? extends SpringSupported<?>> definitions = PropertiesValueCompletionProvider.getSupportedDefinitions(key);
                if (!definitions.isEmpty()) {
                    if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                        AzureTelemeter.info("connector.not_signed_in.yaml_value_code_completion", ImmutableMap.of("key", key));
                        result.addElement(LookupElements.buildSignInLookupElement());
                    } else {
                        result.addElement(LookupElements.buildConnectLookupElement(definitions.get(0), (c, ctx) -> YamlUtils.insertYamlConnection(c, ctx, key)));
                    }
                }
            }
        });
    }
}
