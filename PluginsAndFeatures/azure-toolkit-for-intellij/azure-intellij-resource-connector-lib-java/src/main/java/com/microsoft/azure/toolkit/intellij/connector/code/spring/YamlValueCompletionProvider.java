/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.google.common.collect.ImmutableMap;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.code.Utils;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLPsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class YamlValueCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final YAMLPsiElement yamlElement = PsiTreeUtil.getParentOfType(element, YAMLPsiElement.class);
        if (Objects.isNull(yamlElement) || !Azure.az(AzureAccount.class).isLoggedIn()) {
            return;
        }
        final String key = YAMLUtil.getConfigFullName(yamlElement);
        final List<? extends SpringSupported<?>> definitions = ResourceManager.getDefinitions().stream()
                .filter(d -> d instanceof SpringSupported<?>)
                .map(d -> (SpringSupported<?>) d)
                .filter(d -> d.getSpringProperties(key).stream().anyMatch(p -> StringUtils.equals(p.getKey(), key)))
                .collect(Collectors.toList());
        ProgressManager.checkCanceled();
        final Module module = ModuleUtil.findModuleForPsiElement(parameters.getOriginalFile());
        if (Objects.isNull(module) || CollectionUtils.isEmpty(definitions)) {
            return;
        }
        final List<? extends Resource<?>> resources = definitions.stream()
                .flatMap(d -> Utils.checkCancelled(() -> d.getResources(module.getProject())).stream())
                .collect(Collectors.toList());
        ProgressManager.checkCanceled();
        final List<LookupElement> elements = resources.stream()
                .map(resource -> createYamlValueLookupElement(module, resource))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        elements.forEach(result::addElement);
        AzureTelemeter.log(AzureTelemetry.Type.OP_END, OperationBundle.description("boundary/connector.complete_values_in_yaml"));
        AzureTelemeter.info("connector.resources_count.yaml_value_code_completion", ImmutableMap.of("count", elements.size() + "", "key", key));
    }

    @Nullable
    private LookupElement createYamlValueLookupElement(@Nonnull final Module module, @Nonnull final Resource<?> resource) {
        final AzureModule azureModule = AzureModule.from(module);
        final ResourceDefinition<?> definition = resource.getDefinition();
        return LookupElementBuilder.create(resource.getName())
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(definition.getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .withBoldness(true)
                .withPresentableText(resource.getName())
                .withTypeText(((AzResource) resource.getData()).getResourceTypeName())
                .withTailText(" " + ((AzResource) resource.getData()).getResourceGroupName())
                .withLookupStrings(Arrays.asList(resource.getName(), ((AzResource) resource.getData()).getResourceGroupName()))
                .withInsertHandler((context, item) -> handleYamlConnection(azureModule, resource, context));
    }

    @AzureOperation(name = "user/connector.insert_spring_yaml_properties")
    private void handleYamlConnection(AzureModule azureModule, Resource<?> resource, InsertionContext context) {
        final Profile defaultProfile = azureModule.getDefaultProfile();
        final List<Connection<?, ?>> connections = Objects.isNull(defaultProfile) ? Collections.emptyList() : defaultProfile.getConnections();
        final Connection<?, ?> connection = connections.stream().filter(c -> Objects.equals(c.getResource(), resource)).findFirst().orElse(null);
        if (Objects.nonNull(connection)) {
            insertConnection(connection, context);
        } else {
            azureModule.connect(resource, c -> insertConnection(c, context));
        }
    }

    private void insertConnection(@Nullable Connection<?, ?> connection, @Nonnull InsertionContext context) {
        if (Objects.isNull(connection)) {
            return;
        }
        // delete value insert by LookupElement
        final PsiElement element = PsiUtil.getElementAtOffset(context.getFile(), context.getStartOffset());
        final YAMLPsiElement yamlElement = Objects.requireNonNull(PsiTreeUtil.getParentOfType(element, YAMLPsiElement.class));
        final String key = YAMLUtil.getConfigFullName(yamlElement);
        final SpringSupported<?> definition = (SpringSupported<?>) connection.getResource().getDefinition();
        final String value = definition.getSpringProperties(key).stream()
                .filter(pair -> StringUtils.equalsIgnoreCase(pair.getKey(), key))
                .findFirst()
                .map(Pair::getValue)
                .map(v -> YamlUtils.getPropertiesCompletionValue(v, connection)).orElse(StringUtils.EMPTY);
        context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
        context.getDocument().insertString(context.getStartOffset(), value);
        context.commitDocument();
        // add up missing connection parameters
        YamlUtils.insertYamlConnection(connection, context, key);
    }
}

