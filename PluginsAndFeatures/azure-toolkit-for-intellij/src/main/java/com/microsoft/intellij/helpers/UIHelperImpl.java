/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.toolkit.intellij.common.AzureFileType;
import com.microsoft.azure.toolkit.intellij.docker.ContainerRegistryPropertyView;
import com.microsoft.azure.toolkit.intellij.docker.ContainerRegistryPropertyViewProvider;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.forms.ErrorMessageForm;
import com.microsoft.intellij.forms.OpenSSLFinderForm;
import com.microsoft.intellij.helpers.storage.BlobExplorerFileEditor;
import com.microsoft.intellij.helpers.storage.BlobExplorerFileEditorProvider;
import com.microsoft.intellij.helpers.storage.QueueExplorerFileEditorProvider;
import com.microsoft.intellij.helpers.storage.QueueFileEditor;
import com.microsoft.intellij.helpers.storage.TableExplorerFileEditorProvider;
import com.microsoft.intellij.helpers.storage.TableFileEditor;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.model.storage.StorageServiceTreeItem;
import com.microsoft.tooling.msservices.model.storage.Table;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.function.Supplier;

import static com.microsoft.azure.toolkit.intellij.springcloud.properties.SpringCloudAppPropertiesEditorProvider.SPRING_CLOUD_APP_PROPERTY_TYPE;


public class UIHelperImpl implements UIHelper {
    public static final Key<StorageAccount> STORAGE_KEY = new Key<>("storageAccount");
    public static final Key<ClientStorageAccount> CLIENT_STORAGE_KEY = new Key<>("clientStorageAccount");
    public static final Key<String> SUBSCRIPTION_ID = new Key<>("subscriptionId");
    public static final Key<String> RESOURCE_ID = new Key<>("resourceId");

    private final Map<Class<? extends StorageServiceTreeItem>, Key<? extends StorageServiceTreeItem>> name2Key =
        ImmutableMap.of(BlobContainer.class, BlobExplorerFileEditorProvider.CONTAINER_KEY,
                        Queue.class, QueueExplorerFileEditorProvider.QUEUE_KEY,
                        Table.class, TableExplorerFileEditorProvider.TABLE_KEY);

    private static final String UNABLE_TO_OPEN_BROWSER = "Unable to open external web browser";
    private static final String UNABLE_TO_OPEN_EDITOR_WINDOW = "Unable to open new editor window";
    private static final String CANNOT_GET_FILE_EDITOR_MANAGER = "Cannot get FileEditorManager";

    @Override
    public void showException(@NotNull final String message,
                              @Nullable final Throwable ex,
                              @NotNull final String title,
                              final boolean appendEx,
                              final boolean suggestDetail) {
        AzureTaskManager.getInstance().runLater(() -> {
            final String headerMessage = getHeaderMessage(message, ex, appendEx, suggestDetail);
            final String details = getDetails(ex);
            final ErrorMessageForm em = new ErrorMessageForm(title);
            em.showErrorMessageForm(headerMessage, details);
            em.show();
        });
    }

    @Override
    public void showError(@NotNull final String message, @NotNull final String title) {
        showError(null, message, title);
    }

    @Override
    public void showError(Component component, String message, String title) {
        AzureTaskManager.getInstance().runLater(() -> Messages.showErrorDialog(component, message, title));
    }

    @Override
    public boolean showConfirmation(@NotNull String message, @NotNull String title, @NotNull String[] options,
                                    String defaultOption) {
        return runFromDispatchThread(() -> 0 == Messages.showDialog(message,
                                                                    title,
                                                                    options,
                                                                    ArrayUtils.indexOf(options, defaultOption),
                                                                    null));
    }

    @Override
    public boolean showConfirmation(@NotNull Component node, @NotNull String message, @NotNull String title, @NotNull String[] options, String defaultOption) {
        return runFromDispatchThread(() -> 0 == Messages.showDialog(node,
                                                                    message,
                                                                    title,
                                                                    options,
                                                                    ArrayUtils.indexOf(options, defaultOption),
                                                                    null));
    }

    @Override
    public void showInfo(Node node, String s) {
        showNotification(node, s, MessageType.INFO);
    }

    @Override
    public void showError(Node node, String s) {
        showNotification(node, s, MessageType.ERROR);
    }

    private void showNotification(Node node, String s, MessageType type) {
        final StatusBar statusBar = WindowManager.getInstance().getStatusBar((Project) node.getProject());
        UIUtils.showNotification(statusBar, s, type);
    }

    @Override
    public void logError(String message, Throwable ex) {
        AzurePlugin.log(message, ex);
    }

    /**
     * returns File if file chosen and OK pressed; otherwise returns null
     * TODO: name confusion, FileChooser vs FileSaver
     */
    @Override
    public File showFileChooser(String title) {
        return showFileSaver(title, "");
    }

    @Override
    public File showFileSaver(String title, String fileName) {
        final FileSaverDescriptor fileDescriptor = new FileSaverDescriptor(title, "");
        final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileDescriptor, (Project) null);
        final VirtualFileWrapper save = dialog.save(LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")), fileName);

        if (save != null) {
            return save.getFile();
        }
        return null;
    }

    @Override
    public <T extends StorageServiceTreeItem> void openItem(@NotNull Object projectObject,
                                                            @Nullable StorageAccount storageAccount,
                                                            @NotNull T item,
                                                            @Nullable String itemType,
                                                            @NotNull final String itemName,
                                                            @Nullable final String iconName) {
        final LightVirtualFile itemVirtualFile = new LightVirtualFile(item.getName() + itemType);
        itemVirtualFile.putUserData((Key<T>) name2Key.get(item.getClass()), item);
        itemVirtualFile.putUserData(STORAGE_KEY, storageAccount);

        itemVirtualFile.setFileType(new AzureFileType(itemName, UIHelperImpl.loadIcon(iconName)));

        openItem(projectObject, itemVirtualFile);
    }

    @Override
    public <T extends StorageServiceTreeItem> void openItem(Object projectObject,
                                                            ClientStorageAccount clientStorageAccount,
                                                            T item, String itemType,
                                                            String itemName,
                                                            String iconName) {
        final LightVirtualFile itemVirtualFile = new LightVirtualFile(item.getName() + itemType);
        itemVirtualFile.putUserData((Key<T>) name2Key.get(item.getClass()), item);
        itemVirtualFile.putUserData(CLIENT_STORAGE_KEY, clientStorageAccount);

        itemVirtualFile.setFileType(new AzureFileType(itemName, UIHelperImpl.loadIcon(iconName)));

        openItem(projectObject, itemVirtualFile);
    }

    @Override
    public void openItem(@NotNull final Object projectObject, @NotNull final Object itemVirtualFile) {
        AzureTaskManager
            .getInstance()
            .runLater(() -> FileEditorManager.getInstance((Project) projectObject).openFile((VirtualFile) itemVirtualFile, true, true));
    }

    @Override
    public void refreshQueue(@NotNull final Object projectObject, @NotNull final StorageAccount storageAccount,
                             @NotNull final Queue queue) {
        AzureTaskManager.getInstance().read(() -> {
            final VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount.name(), queue);
            if (file != null) {
                final QueueFileEditor queueFileEditor = (QueueFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                AzureTaskManager.getInstance().runLater(() -> queueFileEditor.fillGrid());
            }
        });
    }

    @Override
    public void refreshBlobs(@NotNull final Object projectObject, @NotNull final String accountName, @NotNull final BlobContainer container) {
        AzureTaskManager.getInstance().read(() -> {
            final VirtualFile file = (VirtualFile) getOpenedFile(projectObject, accountName, container);
            if (file != null) {
                final BlobExplorerFileEditor containerFileEditor =
                    (BlobExplorerFileEditor) FileEditorManager.getInstance((Project) projectObject)
                                                              .getEditors(file)[0];
                AzureTaskManager.getInstance().runLater(() -> containerFileEditor.fillGrid());
            }
        });
    }

    @Override
    public void refreshTable(@NotNull final Object projectObject, @NotNull final StorageAccount storageAccount,
                             @NotNull final Table table) {
        AzureTaskManager.getInstance().read(() -> {
            final VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount.name(), table);
            if (file != null) {
                final TableFileEditor tableFileEditor = (TableFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                AzureTaskManager.getInstance().runLater(tableFileEditor::fillGrid);
            }
        });
    }

    @NotNull
    @Override
    public String promptForOpenSSLPath() {
        final OpenSSLFinderForm openSSLFinderForm = new OpenSSLFinderForm(null);
        openSSLFinderForm.setModal(true);
        openSSLFinderForm.show();

        return DefaultLoader.getIdeHelper().getPropertyWithDefault("MSOpenSSLPath", "");
    }

    @Override
    public void openRedisPropertyView(@NotNull RedisCacheNode node) {
        throw new UnsupportedOperationException("this method should not be called");
    }

    @Override
    public void openRedisExplorer(RedisCacheNode redisCacheNode) {
        throw new UnsupportedOperationException("this method should not be called");
    }

    @Override
    public void openInBrowser(String link) {
        try {
            Desktop.getDesktop().browse(URI.create(link));
        } catch (final Exception e) {
            showException(UNABLE_TO_OPEN_BROWSER, e, UNABLE_TO_OPEN_BROWSER, false, false);
        }
    }

    @Override
    public void openContainerRegistryPropertyView(@NotNull ContainerRegistryNode node) {
        final String registryName = node.getName() != null ? node.getName() : RedisCacheNode.TYPE;
        final String sid = node.getSubscriptionId();
        final String resId = node.getResourceId();
        if (isSubscriptionIdAndResourceIdEmpty(sid, resId)) {
            return;
        }
        final Project project = (Project) node.getProject();
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager,
                                                              ContainerRegistryPropertyViewProvider.TYPE, resId);
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(registryName, sid, resId);
            final AzureFileType fileType = new AzureFileType(ContainerRegistryPropertyViewProvider.TYPE,
                AzureIconLoader.loadIcon(AzureIconSymbol.ContainerRegistry.MODULE));
            itemVirtualFile.setFileType(fileType);
        }
        final FileEditor[] editors = fileEditorManager.openFile(itemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/);
        for (final FileEditor editor: editors) {
            if (editor.getName().equals(ContainerRegistryPropertyView.ID) &&
                editor instanceof ContainerRegistryPropertyView) {
                ((ContainerRegistryPropertyView) editor).onReadProperty(sid, resId);
            }
        }
    }

    @Override
    public void openWebAppPropertyView(@Nonnull WebAppNode webAppNode) {
        // moved to com.microsoft.azure.toolkit.intellij.appservice.actions.OpenAppServicePropertyViewAction
    }

    @Override
    public void openDeploymentSlotPropertyView(@Nonnull DeploymentSlotNode deploymentSlotNode) {
        // moved to com.microsoft.azure.toolkit.intellij.appservice.actions.OpenAppServicePropertyViewAction
    }

    protected FileEditorManager getFileEditorManager(@NotNull final String sid, @NotNull final String webAppId,
                                                     @NotNull final Project project) {
        if (isSubscriptionIdAndResourceIdEmpty(sid, webAppId)) {
            return null;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return null;
        }
        return fileEditorManager;
    }

    @Nullable
    @Override
    public <T extends StorageServiceTreeItem> Object getOpenedFile(@NotNull Object projectObject,
                                                                   @NotNull String accountName,
                                                                   @NotNull T item) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) projectObject);

        for (final VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            final T editedItem = editedFile.getUserData((Key<T>) name2Key.get(item.getClass()));
            final StorageAccount editedStorageAccount = editedFile.getUserData(STORAGE_KEY);
            final ClientStorageAccount editedClientStorageAccount = editedFile.getUserData(CLIENT_STORAGE_KEY);
            if (((editedStorageAccount != null && editedStorageAccount.name().equals(accountName)) ||
                    (editedClientStorageAccount != null && editedClientStorageAccount.getName().equals(accountName))) &&
                    editedItem != null &&
                    editedItem.getName().equals(item.getName())) {
                return editedFile;
            }
        }

        return null;
    }

    @Override
    public boolean isDarkTheme() {
        return UIUtil.isUnderDarcula();
    }

    public void closeSpringCloudAppPropertyView(@NotNull Object projectObject, String appId) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) projectObject);
        final LightVirtualFile file = searchExistingFile(fileEditorManager, SPRING_CLOUD_APP_PROPERTY_TYPE, appId);
        if (file != null) {
            AzureTaskManager.getInstance().runLater(() -> fileEditorManager.closeFile(file));
        }
    }

    @NotNull
    private static String getHeaderMessage(@NotNull String message, @Nullable Throwable ex,
                                           boolean appendEx, boolean suggestDetail) {
        String headerMessage = message.trim();

        if (ex != null && appendEx) {
            final String exMessage = (ex.getLocalizedMessage() == null || ex.getLocalizedMessage().isEmpty()) ? ex.getMessage() : ex.getLocalizedMessage();
            final String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + exMessage;
        }

        if (suggestDetail) {
            final String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + "Click on '" +
                ErrorMessageForm.advancedInfoText + "' for detailed information on the cause of the error.";
        }

        return headerMessage;
    }

    @NotNull
    private static String getDetails(@Nullable Throwable ex) {
        String details = "";

        if (ex != null) {
            final StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            details = sw.toString();

            if (ex instanceof AzureCmdException) {
                final String errorLog = ((AzureCmdException) ex).getErrorLog();
                if (errorLog != null && !errorLog.isEmpty()) {
                    details = errorLog;
                }
            }
        }

        return details;
    }

    @NotNull
    public static ImageIcon loadIcon(@Nullable String name) {
        final java.net.URL url = UIHelperImpl.class.getResource("/icons/" + name);
        return new ImageIcon(url);
    }

    private LightVirtualFile searchExistingFile(FileEditorManager fileEditorManager, String fileType, String resourceId) {
        LightVirtualFile virtualFile = null;
        for (final VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            final String fileResourceId = editedFile.getUserData(RESOURCE_ID);
            if (fileResourceId != null && fileResourceId.equals(resourceId) &&
                editedFile.getFileType().getName().equals(fileType)) {
                virtualFile = (LightVirtualFile) editedFile;
                break;
            }
        }
        return virtualFile;
    }

    private LightVirtualFile createVirtualFile(String name, String sid, String resId) {
        final LightVirtualFile itemVirtualFile = new LightVirtualFile(name);
        itemVirtualFile.putUserData(SUBSCRIPTION_ID, sid);
        itemVirtualFile.putUserData(RESOURCE_ID, resId);
        return itemVirtualFile;
    }

    private boolean isSubscriptionIdAndResourceIdEmpty(String sid, String resId) {
        if (Utils.isEmptyString(sid)) {
            showError("Cannot get Subscription ID", UNABLE_TO_OPEN_EDITOR_WINDOW);
            return true;
        }
        if (Utils.isEmptyString(resId)) {
            showError("Cannot get resource ID", UNABLE_TO_OPEN_EDITOR_WINDOW);
            return true;
        }
        return false;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        final int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public void showMessageDialog(Component component, String message, String title, Icon icon) {
        DefaultLoader.getIdeHelper().invokeLater(() -> Messages.showMessageDialog(component, message, title, icon));
    }

    @Override
    public int showConfirmDialog(Component component, String message, String title, String[] options,
                                 String defaultOption, Icon icon) {
        return runFromDispatchThread(() -> Messages.showDialog(component,
                                                               message,
                                                               title,
                                                               options,
                                                               ArrayUtils.indexOf(options, defaultOption),
                                                               icon));
    }

    @Override
    public boolean showYesNoDialog(Component component, String message, String title, Icon icon) {
        return runFromDispatchThread(() -> {
            return component == null ? Messages.showYesNoDialog(message, title, icon) == Messages.YES :
                   Messages.showYesNoDialog(component, message, title, icon) == Messages.YES;
        });
    }

    @Override
    public String showInputDialog(Component component, String message, String title, Icon icon) {
        return runFromDispatchThread(() -> Messages.showInputDialog(component, message, title, icon));
    }

    @Override
    public void showInfoNotification(String title, String message) {
        PluginUtil.showInfoNotification(title, message);
    }

    @Override
    public void showWarningNotification(String title, String message) {
        PluginUtil.showWarnNotification(title, message);
    }

    @Override
    public void showErrorNotification(String title, String message) {
        PluginUtil.showErrorNotification(title, message);
    }

    private static <T> T runFromDispatchThread(Supplier<T> supplier) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return supplier.get();
        }
        final RunnableFuture<T> runnableFuture = new FutureTask<>(() -> supplier.get());
        AzureTaskManager.getInstance().runLater(runnableFuture);
        try {
            return runnableFuture.get();
        } catch (final InterruptedException | ExecutionException e) {
            return null;
        }
    }
}
