/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocumentContainer;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCollection;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;
import java.util.function.Function;

import static com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions.FILE_CHANGED;

public class CreateNewDocumentAction {

    private static final String DEFAULT_TEMPLATE = "{\n" +
            "\t\"id\": \"replace_with_new_document_id\"\n" +
            "}";
    private static final String SHARED_KEY_TEMPLATE = "{\n" +
            "\t\"id\": \"replace_with_new_document_id\",\n" +
            "\t\"%s\": \"replace_with_new_document_shared_key\"\n" +
            "}";

    public static void create(@Nonnull final ICosmosDocumentContainer<?> container, @Nonnull final Project project) {
        create(container, project, getDocumentTemplate(container));
    }

    private static String getDocumentTemplate(@Nonnull final ICosmosDocumentContainer<?> container) {
        if (container instanceof MongoCollection) {
            final String sharedKey = ((MongoCollection) container).getSharedKey();
            return StringUtils.isEmpty(sharedKey) ? DEFAULT_TEMPLATE : String.format(SHARED_KEY_TEMPLATE, sharedKey);
        }
        return DEFAULT_TEMPLATE;
    }

    @SneakyThrows
    public static void create(@Nonnull final ICosmosDocumentContainer<?> container, @Nonnull final Project project, @Nonnull final String template) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final VirtualFile virtualFile = getOrCreateVirtualFile(container, fileEditorManager, template);
        virtualFile.putUserData(FILE_CHANGED, true); // mark file changed to prompt when close with default content
        final Function<String, Boolean> onSave = content -> {
            AzureTaskManager.getInstance().runInBackground(new AzureTask<>(OperationBundle.description("internal/cosmos.create_document.container", container.getName()), () -> {
                try {
                    final ObjectNode node = new ObjectMapper().readValue(content, ObjectNode.class);
                    container.importDocument(node);
                    AzureMessager.getMessager().info(String.format("Save document to %s successfully.", container.getName()));
                } catch (final RuntimeException | JsonProcessingException e) {
                    AzureMessager.getMessager().error(e);
                }
            }));
            return true;
        };
        final Runnable onClose = () -> WriteAction.run(() -> FileUtil.delete(new File(virtualFile.getPath())));
        VirtualFileActions.openFileInEditor(virtualFile, onSave, onClose, fileEditorManager);
    }

    private static synchronized VirtualFile getOrCreateVirtualFile(@Nonnull final ICosmosDocumentContainer<?> container,
                                                                   final FileEditorManager manager, final String content) {
        final VirtualFile virtualFile = VirtualFileActions.getVirtualFile(container.getId(), manager);
        return Objects.isNull(virtualFile) ? createVirtualFile(container, manager, content) : virtualFile;
    }

    @SneakyThrows
    private static VirtualFile createVirtualFile(@Nonnull final ICosmosDocumentContainer<?> container,
                                                 final FileEditorManager manager, final String content) {
        final File tempFile = FileUtil.createTempFile(String.format("New Document - %s", container.getName()), ".json", true);
        FileUtil.writeToFile(tempFile, content);
        return VirtualFileActions.createVirtualFile(container.getId(), tempFile.getName(), tempFile, manager);
    }
}
