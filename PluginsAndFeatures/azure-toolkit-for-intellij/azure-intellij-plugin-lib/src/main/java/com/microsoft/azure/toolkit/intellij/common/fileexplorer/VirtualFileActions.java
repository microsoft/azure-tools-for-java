/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.fileexplorer;

import com.intellij.AppTopics;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class VirtualFileActions {
    public static final Key<Boolean> FILE_CHANGED = new Key<>("FILE_CHANGED");
    private static final Key<String> FILE_ID = new Key<>("FILE_ID");
    private static final String FILE_EDITING = "Save File";
    private static final String SAVE_CHANGES = "Do you want to save your changes?";
    private static final String SUCCESS_DOWNLOADING = "File %s is successfully downloaded to %s.";
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";

    @SneakyThrows(IOException.class)
    public static void openJsonStringInEditor(@Nonnull String jsonContent, @Nonnull String title, @Nonnull Project project) {
        final FileEditorManager manager = FileEditorManager.getInstance(project);
        VirtualFile vf = VirtualFileActions.getVirtualFile(title, manager);
        if (Objects.isNull(vf)) {
            final File temp = FileUtil.createTempFile("", title, true);
            Files.writeString(temp.toPath(), jsonContent);
            vf = VirtualFileActions.createVirtualFile(title, title, temp, manager);
            final VirtualFile finalVf = vf;
            WriteAction.run(() -> finalVf.setWritable(false)); // make it readonly.
        }
        final FileEditor[] editors = manager.openFile(vf, true, true);
    }

    @AzureOperation(name = "boundary/common.open_file_in_editor.file", params = {"file.getName()"})
    public static void openFileInEditor(VirtualFile file, final Function<? super String, Boolean> onSave, Runnable onClose, FileEditorManager manager) {
        final Project project = manager.getProject();
        final AzureTaskManager taskManager = AzureTaskManager.getInstance();
        taskManager.runOnPooledThread(() -> {
            final FileType type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(file, project);
            if (type == null) {
                return;
            } else if (type.isBinary()) {
                AzureMessager.getMessager().alert("Binary file is not supported to open in editor.");
                return;
            }
            taskManager.runLater(() -> {
                final FileEditor[] editors = manager.openFile(file, true, true);
                if (editors.length == 0) {
                    throw new AzureToolkitRuntimeException(String.format("Failed to open file %s in editor. Try downloading it first and open it manually.", file.getName()));
                }
                Arrays.stream(editors).filter(e -> e instanceof TextEditor).forEach(e -> addFileListeners(file, onSave, onClose, manager, (TextEditor) e));
            });
        });
    }

    private static void addFileListeners(VirtualFile virtualFile, Function<? super String, Boolean> onSave, Runnable onClose, FileEditorManager manager, TextEditor editor) {
        final MessageBusConnection messageBusConnection = manager.getProject().getMessageBus().connect(editor);
        final Document editorDoc = editor.getEditor().getDocument();
        editorDoc.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@Nonnull DocumentEvent event) {
                virtualFile.putUserData(FILE_CHANGED, true);
            }
        });
        messageBusConnection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerListener() {
            @Override
            public void beforeDocumentSaving(@Nonnull Document document) {
                if (Objects.equals(document, editorDoc) && Boolean.TRUE.equals(virtualFile.getUserData(FILE_CHANGED))) {
                    if (onSave.apply(document.getText())) {
                        virtualFile.putUserData(FILE_CHANGED, false);
                    }
                }
            }
        });
        messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, new FileEditorManagerListener.Before() {
            @SneakyThrows
            @Override
            public void beforeFileClosed(FileEditorManager source, VirtualFile file) {
                try {
                    if (Objects.equals(file, virtualFile) && Boolean.TRUE.equals(virtualFile.getUserData(FILE_CHANGED))) {
                        if (AzureMessager.getMessager().confirm(SAVE_CHANGES, FILE_EDITING)) {
                            final String content = editorDoc.getText();
                            WriteAction.run(() -> LoadTextUtil.write(manager.getProject(), file, this, content, editorDoc.getModificationStamp()));
                            onSave.apply(content);
                        }
                        virtualFile.putUserData(FILE_CHANGED, false);
                        onClose.run();
                    }
                } finally {
                    messageBusConnection.disconnect();
                }
            }
        });
    }

    public static VirtualFile getVirtualFile(String fileId, FileEditorManager manager) {
        return Arrays.stream(manager.getOpenFiles())
                .filter(f -> StringUtils.equals(f.getUserData(FILE_ID), fileId))
                .findAny().orElse(null);
    }

    public static VirtualFile createVirtualFile(String fileId, String fileName, File file, FileEditorManager manager) {
        return Arrays.stream(manager.getOpenFiles())
                .filter(f -> StringUtils.equals(f.getUserData(FILE_ID), fileId))
                .findAny().orElse(createTempVirtualFile(fileId, fileName, file, manager));
    }

    @SneakyThrows
    private static VirtualFile createTempVirtualFile(final String fileId, final String fileName, File tempFile, FileEditorManager manager) {
        final VirtualFile origin = Objects.requireNonNull(VfsUtil.findFileByIoFile(tempFile, true));
        final VirtualFile virtualFile = new RemoteVirtualFile(origin, fileName);
        virtualFile.setCharset(StandardCharsets.UTF_8);
        virtualFile.putUserData(FILE_ID, fileId);
        AzureTaskManager.getInstance().runLater(() -> {
            try {
                WriteAction.run(() -> virtualFile.setWritable(true));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return virtualFile;
    }

    private static String getTextEditorContent(TextEditor textEditor) {
        return textEditor.getEditor().getDocument().getText();
    }

    public static void notifyDownloadSuccess(final String name, final File dest, final Project project) {
        final String title = "File downloaded";
        final File directory = dest.getParentFile();
        final String message = String.format(SUCCESS_DOWNLOADING, name, directory.getAbsolutePath());
        final AzureString msg = AzureString.format(SUCCESS_DOWNLOADING, name, directory.getAbsolutePath());
        AzureMessager.getMessager().success(msg, title, newOpenInEditorAction(dest, project), newShowInExplorerAction(dest));
    }

    private static Action<File> newShowInExplorerAction(@Nonnull final File file) {
        return AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.REVEAL_FILE).bind(file);
    }

    private static Action<File> newOpenInEditorAction(@Nonnull final File file, @Nonnull final Project project) {
        return AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_FILE).bind(file);
    }

    @AzureOperation(name = "boundary/common.reveal_file_in_explorer.file", params = {"file.getName()"})
    public static void revealInExplorer(@Nonnull File file) {
        AzureTaskManager.getInstance().runLater(() -> RevealFileAction.openFile(file));
    }

    @RequiredArgsConstructor
    private static class RemoteVirtualFile extends VirtualFile {

        @Delegate(types = VirtualFile.class, excludes = {Customize.class, UserDataHolderBase.class})
        private final VirtualFile origin;
        private final String path;

        @Nonnull
        public String getPresentableName() {
            return path;
        }

        private interface Customize {
            public String getPresentableName();

            public String getPresentableUrl();

            public void setBinaryContent(byte[] content) throws IOException;

            public OutputStream getOutputStream(Object requestor) throws IOException;
        }
    }
}
