/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.code.spring;

import com.google.common.collect.ImmutableMap;
import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.util.ProcessingContext;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.storage.StorageActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.projectexplorer.AbstractAzureFacetNode;
import com.microsoft.azure.toolkit.intellij.storage.code.Utils;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainer;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainerModule;
import com.microsoft.azure.toolkit.lib.storage.blob.IBlobFile;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import com.microsoft.azure.toolkit.lib.storage.share.IShareFile;
import com.microsoft.azure.toolkit.lib.storage.share.Share;
import com.microsoft.azure.toolkit.lib.storage.share.ShareModule;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class StringLiteralResourceCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@Nonnull CompletionParameters parameters, @Nonnull ProcessingContext context, @Nonnull CompletionResultSet result) {
        final PsiElement element = parameters.getPosition();
        final PsiLiteralExpression literal = ((PsiLiteralExpression) element.getParent());
        final String value = literal.getValue() instanceof String ? (String) literal.getValue() : element.getText();
        final String fullPrefix = StringUtils.substringBefore(value, StringLiteralCompletionContributor.DUMMY_IDENTIFIER);
        final boolean isBlobContainer = fullPrefix.startsWith("azure-blob://");
        final boolean isFileShare = fullPrefix.startsWith("azure-file://");

        if ((isBlobContainer || isFileShare) && Azure.az(AzureAccount.class).isLoggedIn()) {
            final Module module = ModuleUtil.findModuleForFile(parameters.getOriginalFile());
            if (Objects.isNull(module)) {
                return;
            }
            final List<? extends StorageFile> files = getFiles(fullPrefix, Utils.getConnectedStorageAccounts(module));
            final String[] parts = result.getPrefixMatcher().getPrefix().trim().split("/", -1);
            result = result.withPrefixMatcher(parts[parts.length - 1]);
            AzureTelemeter.info("connector.resources_count.storage_resources_code_completion", ImmutableMap.of("count", files.size() + ""));
            final BiFunction<StorageFile, String, LookupElementBuilder> builder = (file, title) -> LookupElementBuilder.create(title)
                .withInsertHandler(new MyInsertHandler(title.endsWith("/")))
                .withBoldness(true)
                .withCaseSensitivity(false)
                .withTypeText(file.getResourceTypeName())
                .withTailText(" " + Optional.ofNullable(getStorageAccount(file)).map(AbstractAzResource::getName).orElse(""))
                .withIcon(IntelliJAzureIcons.getIcon(getFileIcon(file)));
            for (final StorageFile file : files) {
                result.addElement(builder.apply(file, file.getName()));
                if (file.isDirectory()) {
                    result.addElement(builder.apply(file, file.getName() + "/"));
                }
            }
            AzureTelemeter.log(AzureTelemetry.Type.OP_END, OperationBundle.description("boundary/connector.complete_storage_resources_in_string_literal"));
        }
    }

    public static List<? extends StorageFile> getFiles(String fullPrefix, @Nonnull final List<StorageAccount> accounts) {
        final String fixedFullPrefix = fullPrefix.replace("azure-blob://", "").replace("azure-file://", "").trim();
        final String[] parts = fixedFullPrefix.split("/", -1);
        final var getModule = fullPrefix.startsWith("azure-blob://") ?
            (Function<StorageAccount, BlobContainerModule>) StorageAccount::getBlobContainerModule :
            (Function<StorageAccount, ShareModule>) StorageAccount::getShareModule;
        List<? extends StorageFile> files = accounts.stream().map(getModule).flatMap(m -> m.list().stream()).map(r -> ((StorageFile) r)).toList();
        for (int i = 1; i < parts.length; i++) {
            final String parentName = parts[i - 1];
            files = files.stream().filter(f -> f.getName().equalsIgnoreCase(parentName)).filter(StorageFile::isDirectory).flatMap(f -> f.getSubFileModule().list().stream()).toList();
        }
        return files;
    }

    public static List<Connection<?, ?>> getConnections(Module module) {
        return Optional.of(module).map(AzureModule::from)
            .map(AzureModule::getDefaultProfile).map(Profile::getConnectionManager).stream()
            .flatMap(m -> m.getConnections().stream())
            .filter(c -> c.getDefinition().getResourceDefinition() instanceof StorageAccountResourceDefinition)
            .filter(c -> c.getResource().isValidResource())
            .toList();
    }

    @Nullable
    public static StorageFile getFile(String fullPrefix, Module module) {
        return getFile(fullPrefix, Utils.getConnectedStorageAccounts(module));
    }

    @Nullable
    public static StorageFile getFile(String fullPrefix, @Nonnull final List<StorageAccount> accounts) {
        final List<? extends StorageFile> files = getFiles(fullPrefix, accounts);
        final String[] parts = fullPrefix.trim().split("/", -1);
        return files.stream().filter(f -> f.getName().equalsIgnoreCase(parts[parts.length - 1].trim())).findFirst().orElse(null);
    }

    @RequiredArgsConstructor
    public static class MyInsertHandler implements InsertHandler<LookupElement> {
        private final boolean popup;

        @Override
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement item) {
            if (popup) {
                AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
            }
        }
    }

    @Nullable
    public static StorageAccount getStorageAccount(final StorageFile file) {
        if (file instanceof IBlobFile) {
            return ((IBlobFile) file).getContainer().getParent();
        } else if (file instanceof IShareFile) {
            return ((IShareFile) file).getShare().getParent();
        }
        return null;
    }

    public static AzureIcon getFileIcon(StorageFile file) {
        if (file instanceof Share || file instanceof BlobContainer) {
            return AzureIcon.builder().iconPath(String.format("/icons/%s/default.svg", file.getFullResourceType())).build();
        }
        final String fileIconName = file.isDirectory() ? "folder" : FilenameUtils.getExtension(file.getName());
        return AzureIcon.builder().iconPath("file/" + fileIconName).build();
    }

    public static void navigateToFile(StorageFile file, Module module) {
        if (Objects.nonNull(module)) {
            final List<Connection<?, ?>> connections = StringLiteralResourceCompletionProvider.getConnections(module);
            if (connections.size() > 0) {
                AbstractAzureFacetNode.selectConnectedResource(connections.get(0), file.getId(), file.isDirectory());
                if (!file.isDirectory()) {
                    DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(context -> {
                        final AnActionEvent event = AnActionEvent.createFromInputEvent(null, ActionPlaces.EDITOR_GUTTER, null, context);
                        AzureActionManager.getInstance().getAction(StorageActionsContributor.OPEN_FILE).handle(file, event);
                    });
                }
            }
        }
    }
}
