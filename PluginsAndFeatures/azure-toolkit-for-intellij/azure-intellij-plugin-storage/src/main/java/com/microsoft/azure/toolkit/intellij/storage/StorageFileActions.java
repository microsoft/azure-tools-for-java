/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.intellij.storage.component.FileCreationDialog;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobFileDraft;
import com.microsoft.azure.toolkit.lib.storage.blob.IBlobFile;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class StorageFileActions {

    @SneakyThrows
    public static void openFileInEditor(StorageFile file, Project project) {
        final String failure = String.format("Can not open file (%s). Try downloading it first and open it manually.", file.getName());
        if (file.getSize() > 10 * FileUtils.ONE_MB) {
            AzureTaskManager.getInstance().runLater(() -> Messages.showWarningDialog(failure, "Open File"));
            return;
        }
        final AzureString title = OperationBundle.description("boundary/storage.load_content.file", file.getName());
        final AzureTask<Void> task = new AzureTask<>(project, title, false, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            final FileEditorManager manager = FileEditorManager.getInstance(project);
            final VirtualFile vf = VirtualFileActions.getVirtualFile(file.getId(), manager);
            if (Objects.nonNull(vf)) {
                AzureTaskManager.getInstance().runLater(() -> manager.openFile(vf, true));
            } else {
                downloadAndOpen(file, project);
            }
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    @SneakyThrows
    private static void downloadAndOpen(@Nonnull StorageFile file, Project project) {
        final String failure = String.format("Can not open file (%s). Try downloading it first and open it manually.", file.getName());
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final File temp = FileUtil.createTempFile("", file.getName(), true);
        temp.delete();
        file.download(Path.of(temp.getAbsolutePath()));
        final VirtualFile virtualFile = VirtualFileActions.createVirtualFile(file.getId(), file.getName(), temp, fileEditorManager);
        final Function<String, Boolean> onSave = content -> {
            final AzureString title = OperationBundle.description("internal/storage.save_content.file", file.getName());
            AzureTaskManager.getInstance().runInBackground(title, () -> {
                @SuppressWarnings("rawtypes") final StorageFile.Draft<? extends StorageFile, ?> draft = (StorageFile.Draft<? extends StorageFile, ?>) ((AbstractAzResource) file).update();
                draft.setSourceFile(temp.toPath());
                draft.updateIfExist();
            });
            return true;
        };
        final Runnable onClose = () -> {
            if (Path.of(file.getPath()).toString().contains(Path.of(FileUtil.getTempDirectory()).toString())) {
                WriteAction.run(() -> FileUtil.delete(temp));
            }
        };
        AzureTaskManager.getInstance().runLater(() -> VirtualFileActions.openFileInEditor(virtualFile, onSave, onClose, fileEditorManager));
    }

    public static void createBlob(IBlobFile file, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FileCreationDialog dialog = new FileCreationDialog(file, project, "New Blob");
            final Action.Id<String> actionId = Action.Id.of("user/storage.create_blob.blob");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(s -> s)
                .withSource(file)
                .withHandler(p -> {
                    final Path rawPath = Paths.get(p);
                    IBlobFile current = file;
                    int i = 0;
                    for (; i < rawPath.getNameCount(); i++) {
                        final String name = rawPath.getName(i).toString();
                        final IBlobFile temp = (IBlobFile) current.getFile(name);
                        if (Objects.isNull(temp)) {
                            break;
                        }
                        current = temp;
                    }
                    final Path relativePath = rawPath.subpath(i, rawPath.getNameCount());
                    final AbstractAzResourceModule<? extends StorageFile, ? extends StorageFile, ?> module = current.getSubFileModule();
                    final BlobFileDraft draft = (BlobFileDraft) module.create(relativePath.getName(0).toString(), "");
                    draft.setRelativePath(relativePath.toString());
                    draft.setDirectory(relativePath.getNameCount() > 1);
                    draft.createIfNotExist();
                    openFileInEditor(Objects.requireNonNull(current.getFile(relativePath.toString())), project);
                }));
            dialog.show();
        });
    }

    public static void createFile(StorageFile file, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FileCreationDialog dialog = new FileCreationDialog(file, project, "New Empty File");
            final Action.Id<String> actionId = Action.Id.of("user/storage.create_file.file");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(s -> s)
                .withSource(file)
                .withHandler(name -> {
                    final AbstractAzResourceModule<? extends StorageFile, ? extends StorageFile, ?> module = file.getSubFileModule();
                    final AzResource.Draft<? extends StorageFile, ?> draft = module.create(name, "");
                    draft.createIfNotExist();
                    openFileInEditor((StorageFile) draft, project);
                }));
            dialog.show();
        });
    }

    public static void createDirectory(StorageFile file, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FileCreationDialog dialog = new FileCreationDialog(file, project, "New Subdirectory");
            final Action.Id<String> actionId = Action.Id.of("user/storage.create_directory.dir");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(s -> s)
                .withSource(file)
                .withHandler(name -> {
                    final StorageFile.Draft<?, ?> draft = (StorageFile.Draft<?, ?>) file.getSubFileModule().create(name, "");
                    draft.setDirectory(true);
                    draft.createIfNotExist();
                }));
            dialog.show();
        });
    }

    public static void uploadFiles(StorageFile file, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, true, false, true);
            descriptor.setTitle("Choose Files to Upload");
            final VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, null);
            final AtomicInteger count = new AtomicInteger(0);
            for (final VirtualFile virtualFile : files) {
                final AzureString title = OperationBundle.description("internal/storage.upload_files.source|dir", virtualFile.getName(), file.getName());
                final AzureTask<Void> task = new AzureTask<>(project, title, false, () -> {
                    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                    indicator.setIndeterminate(true);
                    final AbstractAzResourceModule<? extends StorageFile, ? extends StorageFile, ?> module = file.getSubFileModule();
                    final StorageFile.Draft<?, ?> draft = (StorageFile.Draft<?, ?>) module.create(virtualFile.getName(), "");
                    draft.setSourceFile(Paths.get(virtualFile.getPath()));
                    draft.createIfNotExist();
                    count.incrementAndGet();
                    if (count.get() == files.length) {
                        AzureMessager.getMessager().success(AzureString.format("Successfully uploaded %s file(s) to directory \"%s\".", files.length, file.getName()));
                    }
                });
                AzureTaskManager.getInstance().runInBackground(task);
            }
        });
    }

    public static void uploadFile(StorageFile file, Project project) {
        if (AzureMessager.getMessager().confirm(AzureString.format("This will overwrite content of file (%s), are you sure to do this?", file.getName()))) {
            AzureTaskManager.getInstance().runLater(() -> {
                final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, true, false, false);
                descriptor.setTitle("Choose File to Upload");
                final VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, null);
                if (files.length > 0) {
                    final VirtualFile virtualFile = files[0];
                    final AzureString title = OperationBundle.description("internal/storage.upload_file.source|file", virtualFile.getName(), file.getName());
                    final AzureTask<Void> task = new AzureTask<>(project, title, false, () -> {
                        @SuppressWarnings("rawtypes") final StorageFile.Draft<? extends StorageFile, ?> draft = (StorageFile.Draft<? extends StorageFile, ?>) ((AbstractAzResource) file).update();
                        draft.setSourceFile(Paths.get(virtualFile.getPath()));
                        draft.updateIfExist();
                        final AzureString msg = AzureString.format("Successfully overwrite content of %s with that of file %s.", file.getName(), virtualFile.getName());
                        AzureMessager.getMessager().success(msg);
                    });
                    AzureTaskManager.getInstance().runInBackground(task);
                }
            });
        }
    }

    public static void uploadFolder(StorageFile file, Project project) {

    }

    public static void downloadFile(StorageFile file, Project project) {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            fileChooserDescriptor.setTitle("Choose Where to Save the File");
            final VirtualFile vf = FileChooser.chooseFile(fileChooserDescriptor, null, null);
            if (vf != null) {
                final AzureString title = OperationBundle.description("boundary/storage.download_file.file|dir", file.getName(), vf.getPath());
                manager.runInModal(title, () -> {
                    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                    indicator.setIndeterminate(true);
                    final Path dest = Paths.get(Objects.requireNonNull(vf).getPath(), file.getName());
                    file.download(dest);
                    final File destFile = dest.toFile();
                    if (destFile.exists()) {
                        VirtualFileActions.notifyDownloadSuccess(file.getName(), destFile, project);
                    }
                });
            }
        });
    }

    public static void copyUrl(StorageFile file, Project project) {
        final String url = file.getUrl();
        CopyPasteManager.getInstance().setContents(new StringSelection(url));
        AzureMessager.getMessager().success(AzureString.format("URL of %s copied to clipboard: %s", file.getName(), url));
    }

    public static void copySasUrl(StorageFile file, Project project) {
        final String url = file.getSasUrl();
        CopyPasteManager.getInstance().setContents(new StringSelection(url));
        final AzureString message = AzureString.format("SAS Token and URL of %s copied to clipboard: %s. SAS token will expire after %s day.", file.getName(), url, 1);
        AzureMessager.getMessager().success(message, "SAS Token and URL copied");
    }
}
