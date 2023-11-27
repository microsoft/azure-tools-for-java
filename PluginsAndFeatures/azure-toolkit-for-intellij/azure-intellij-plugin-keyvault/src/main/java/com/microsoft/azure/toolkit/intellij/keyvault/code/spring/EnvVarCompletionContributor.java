/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.code.spring;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.code.LookupElements;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.keyvault.connection.KeyVaultResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.secret.Secret;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLPsiElement;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.toolkit.intellij.connector.code.spring.PropertiesCompletionContributor.APPLICATION_PROPERTIES_FILE;
import static com.microsoft.azure.toolkit.intellij.connector.code.spring.YamlCompletionContributor.APPLICATION_YAML_FILE;

public class EnvVarCompletionContributor extends CompletionContributor {

    public static final List<Character> SPECIAL_CHARS = Arrays.asList('$', '{', '}');
    public static final PsiElementPattern.Capture<PsiElement> PROPERTY_VALUE = PlatformPatterns.psiElement(PropertiesTokenTypes.VALUE_CHARACTERS).inFile(APPLICATION_PROPERTIES_FILE);
    public static final PsiElementPattern.Capture<PsiElement> YAML_VALUE = PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withSuperParent(2, PlatformPatterns.psiElement(YAMLKeyValue.class)).inFile(APPLICATION_YAML_FILE);
    public static final ElementPattern<PsiElement> SPRING_CONFIG_VALUE_PLACES = PlatformPatterns.or(PROPERTY_VALUE, YAML_VALUE);

    public EnvVarCompletionContributor() {
        super();
        extend(CompletionType.BASIC, SPRING_CONFIG_VALUE_PLACES, new PropertiesKeyVaultVariableCompletionProvider());
    }

    private static class PropertiesKeyVaultVariableCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@Nonnull final CompletionParameters parameters, @Nonnull final ProcessingContext context, @Nonnull CompletionResultSet result) {
            final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
            final String prefix = result.getPrefixMatcher().getPrefix();
            if (Objects.isNull(module) || !hasKeyVaultDependencies(module)) {
                return;
            }
            if (!Azure.az(AzureAccount.class).isLoggedIn()) {
                result.addElement(LookupElements.buildSignInLookupElement());
                return;
            }
            if (StringUtils.contains(prefix, "$")) {
                final String after = StringUtils.substringAfterLast(prefix, "$");
                if (after.contains("}")) {
                    return;
                } else {
                    result = result.withPrefixMatcher("$" + after);
                }
            }
            final Optional<String> key = parameters.getOriginalFile() instanceof PropertiesFileImpl ?
                Optional.of(parameters.getPosition()).map(PsiElement::getParent).map(PsiElement::getFirstChild)
                    .map(PsiElement::getText) :
                Optional.of(parameters.getPosition()).map(e -> PsiTreeUtil.getParentOfType(e, YAMLPsiElement.class))
                    .map(YAMLUtil::getConfigFullName);
            if (!key.map(EnvVarCompletionContributor::isSecretKey).orElse(false)) {
                return;
            }
            final List<KeyVault> vaults = AzureModule.from(module).getConnections(KeyVaultResourceDefinition.INSTANCE).stream()
                .filter(Connection::isValidConnection).map(Connection::getResource).map(Resource::getData).toList();
            if (!vaults.isEmpty()) {
                vaults.stream().flatMap(v -> listSecrets(v).stream())
                    .map(s -> LookupElementBuilder.create(String.format("${%s}", s.getName()))
                        .withBoldness(true)
                        .withCaseSensitivity(false)
                        .withTypeText(s.getResourceTypeName())
                        .withTailText(" " + s.getParent().getName())
                        .withIcon(IntelliJAzureIcons.getIcon(AzureResourceIconProvider.getResourceBaseIconPath(s))))
                    .forEach(result::addElement);
            }
        }


        private static boolean hasKeyVaultDependencies(@Nonnull final Module module) {
            final Pattern PATTERN = Pattern.compile("(Gradle|Maven): com\\.azure\\.spring:spring-cloud-azure-starter-keyvault:(.+)");
            final boolean[] hasKeyVaultDependencies = {false};
            OrderEnumerator.orderEntries(module).forEachLibrary(library -> {
                Optional.ofNullable(library.getName()).filter(StringUtils::isNotBlank)
                    .map(PATTERN::matcher).filter(Matcher::matches)
                    .ifPresent(m -> hasKeyVaultDependencies[0] = true);
                return !hasKeyVaultDependencies[0];
            });
            return hasKeyVaultDependencies[0];
        }
    }

    @Nonnull
    static List<Secret> listSecrets(KeyVault v) {
        try {
            return v.secrets().list();
        } catch (final Exception e) {
            return Collections.emptyList();
        }
    }

    static boolean isSecretKey(final String key) {
        return !key.startsWith("spring.cloud.azure.keyvault") &&
            StringUtils.containsAnyIgnoreCase(key, "password", "passwd", "pwd", "key", "secret", "token", "sig", "signature");
    }

    static boolean hasEnvVars(final String value) {
        return value.matches(".*\\$\\{[^}]+}.*");
    }

    @RequiredArgsConstructor
    private static class ConnectKeyVaultInsertHandler implements InsertHandler<LookupElement> {
        private final Resource<KeyVault> vault;

        @Override
        @AzureOperation("user/connector.insert_keyvault_secrets_in_spring_config_from_code_completion")
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement item) {
            context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
            final Module module = ModuleUtil.findModuleForFile(context.getFile());
            Optional.ofNullable(ModuleUtil.findModuleForFile(context.getFile()))
                .map(AzureModule::from)
                .ifPresent(m -> m.connect(vault,
                    (c) -> AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor())));
        }
    }

}
