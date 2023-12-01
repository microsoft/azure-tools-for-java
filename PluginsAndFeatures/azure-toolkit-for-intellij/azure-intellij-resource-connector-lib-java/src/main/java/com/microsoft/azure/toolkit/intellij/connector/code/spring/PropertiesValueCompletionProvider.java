/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.google.common.collect.ImmutableMap;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJvmModifiersOwner;
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
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PropertiesValueCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
        if (Objects.isNull(module)) {
            return;
        }
        final String key = parameters.getPosition().getParent().getFirstChild().getText();
        final List<? extends SpringSupported<?>> definitions = getSupportedDefinitions(key);
        ProgressManager.checkCanceled();
        if (!definitions.isEmpty()) {
            AzureTelemeter.log(AzureTelemetry.Type.OP_END, OperationBundle.description("boundary/connector.complete_resources_in_properties"));
            if (Azure.az(AzureAccount.class).isLoggedIn()) {
                final List<LookupElementBuilder> elements = buildCompletionItems(definitions, module, key);
                ProgressManager.checkCanceled();
                AzureTelemeter.info("connector.resources_count.properties_value_code_completion", ImmutableMap.of("count", elements.size() + "", "key", key));
                elements.forEach(result::addElement);
                result.addLookupAdvertisement("Press enter to configure all required properties to connect Azure resource.");
            }
            // it's not safe to `stopHere()` immediately considering e.g. other cloud service may also
            // provide similar completion items for Redis/MongoDB related properties...
            result.runRemainingContributors(parameters, r -> {
                if (!(r.getLookupElement().getObject() instanceof PsiJvmModifiersOwner)) {
                    result.passResult(r);
                }
            });
        }
    }

    @Nonnull
    @AzureOperation("boundary/connector.build_value_completion_items_in_properties")
    private static List<LookupElementBuilder> buildCompletionItems(final List<? extends SpringSupported<?>> definitions, final Module module, final String key) {
        return definitions.stream().flatMap(d -> Utils.checkCancelled(() -> d.getResources(module.getProject())).stream()
            .map(r -> LookupElementBuilder.create(r, r.getName())
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(d.getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .bold()
                .withCaseSensitivity(false)
                .withLookupStrings(Arrays.asList(r.getName(), ((AzResource) r.getData()).getResourceGroupName()))
                .withInsertHandler(new PropertyValueInsertHandler(r))
                .withTailText(" " + ((AzResource) r.getData()).getResourceGroupName())
                .withTypeText(((AzResource) r.getData()).getResourceTypeName()))).collect(Collectors.toList());
    }

    public static List<? extends SpringSupported<?>> getSupportedDefinitions(String key) {
        final List<ResourceDefinition<?>> definitions = ResourceManager.getDefinitions(ResourceDefinition.RESOURCE).stream()
            .filter(d -> d instanceof SpringSupported<?>).collect(Collectors.toList());
        return definitions.stream().map(d -> (SpringSupported<?>) d)
            .filter(d -> d.getSpringProperties(key).stream().anyMatch(p -> p.getKey().equals(key)))
            .collect(Collectors.toList());
    }

    @RequiredArgsConstructor
    protected static class PropertyValueInsertHandler implements InsertHandler<LookupElement> {

        @SuppressWarnings("rawtypes")
        private final Resource resource;

        @Override
        @ExceptionNotification
        @AzureOperation(name = "user/connector.select_resource_completion_item_in_properties")
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement lookupElement) {
            final Optional<String> optKey = Optional.ofNullable(context.getFile().findElementAt(context.getStartOffset()))
                .map(PsiElement::getParent).map(PsiElement::getFirstChild).map(PsiElement::getText).map(String::trim);
            if (optKey.isEmpty()) {
                return;
            }
            context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
            final Project project = context.getProject();
            final Module module = ModuleUtil.findModuleForFile(context.getFile().getVirtualFile(), project);
            Optional.ofNullable(module).map(AzureModule::from)
                .map(AzureModule::initializeWithDefaultProfileIfNot).map(Profile::getConnectionManager)
                .ifPresent(connectionManager -> connectionManager
                    .getConnectionsByConsumerId(module.getName()).stream()
                    .filter(c -> Objects.equals(resource, c.getResource())).findAny()
                    .ifPresentOrElse(c -> insert(c, context, optKey.get()),
                        () -> connectionManager.getProfile().getModule().connect(resource, c -> insert(c, context, optKey.get()))));
        }

        @AzureOperation(name = "user/connector.insert_value_in_properties")
        public static void insert(@Nullable Connection<?, ?> c, @Nonnull InsertionContext context, String key) {
            if (Objects.isNull(c)) {
                return;
            }
            final List<Pair<String, String>> properties = SpringSupported.getProperties(c, key);
            if (properties.size() < 1) {
                return;
            }
            properties.stream().filter(p -> p.getKey().equals(key)).findAny().ifPresent(p -> {
                properties.remove(p);
                properties.add(0, p);
            });
            final StringBuilder result = new StringBuilder(properties.get(0).getValue()).append(StringUtils.LF);
            for (int i = 1; i < properties.size(); i++) {
                final Pair<String, String> p = properties.get(i);
                result.append(p.getKey()).append("=").append(p.getValue()).append(StringUtils.LF);
            }

            final CaretModel caretModel = context.getEditor().getCaretModel();
            context.getDocument().insertString(caretModel.getOffset(), result.toString());
        }
    }
}
