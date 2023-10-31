/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.spring;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.code.Utils;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

public class PropertiesKeyCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final List<? extends SpringSupported<?>> definitions = ResourceManager
        .getDefinitions(ResourceDefinition.RESOURCE).stream()
        .filter(d -> d instanceof SpringSupported)
        .map(d -> (SpringSupported<?>) d).toList();

    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        final PsiFile file = parameters.getOriginalFile();
        definitions.stream().flatMap(definition ->
            definition.getSpringProperties().stream().filter(p -> !p.getKey().trim().startsWith("#")).map(p -> LookupElementBuilder
                .create(definition.getName(), p.getKey())
                .withIcon(IntelliJAzureIcons.getIcon(StringUtils.firstNonBlank(definition.getIcon(), AzureIcons.Common.AZURE.getIconPath())))
                .withPsiElement(Utils.getPropertyField(definition.getSpringPropertyFields().get(p.getKey()), file))
                .withBoldness(true)
                .withInsertHandler(new PropertyKeyInsertHandler())
                .withTypeText("Property Key")
                .withTailText(String.format(" (%s)", definition.getTitle())))
        ).forEach(result::addElement);
        AzureTelemeter.log(AzureTelemetry.Type.OP_END, OperationBundle.description("boundary/connector.complete_keys_in_properties"));
        result.addLookupAdvertisement("Press enter and select a Azure Storage Account to connect");
    }

    @RequiredArgsConstructor
    private static class PropertyKeyInsertHandler implements InsertHandler<LookupElement> {
        @Override
        @AzureOperation("user/connector.insert_key_in_properties")
        public void handleInsert(@NotNull InsertionContext context, @Nonnull LookupElement item) {
            final CaretModel caretModel = context.getEditor().getCaretModel();
            context.getDocument().insertString(caretModel.getOffset(), "=");
            context.getEditor().getCaretModel().moveToOffset(caretModel.getOffset() + 1);
            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
        }
    }
}
