/**
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018-2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.forms.ErrorMessageForm;
import com.microsoft.intellij.forms.OpenSSLFinderForm;
import com.microsoft.intellij.helpers.containerregistry.ContainerRegistryPropertyView;
import com.microsoft.intellij.helpers.containerregistry.ContainerRegistryPropertyViewProvider;
import com.microsoft.intellij.helpers.functionapp.FunctionAppPropertyViewProvider;
import com.microsoft.intellij.helpers.rediscache.RedisCacheExplorerProvider;
import com.microsoft.intellij.helpers.rediscache.RedisCachePropertyView;
import com.microsoft.intellij.helpers.rediscache.RedisCachePropertyViewProvider;
import com.microsoft.intellij.helpers.storage.*;
import com.microsoft.intellij.helpers.webapp.DeploymentSlotPropertyViewProvider;
import com.microsoft.intellij.helpers.webapp.WebAppPropertyViewProvider;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import com.microsoft.tooling.msservices.model.storage.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.FunctionAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import javax.swing.*;
import java.awt.Desktop;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class UIHelperImpl implements UIHelper {
    public static Key<StorageAccount> STORAGE_KEY = new Key<StorageAccount>("storageAccount");
    public static Key<ClientStorageAccount> CLIENT_STORAGE_KEY = new Key<ClientStorageAccount>("clientStorageAccount");
    public static final Key<String> SUBSCRIPTION_ID = new Key<>("subscriptionId");
    public static final Key<String> RESOURCE_ID = new Key<>("resourceId");
    public static final Key<String> WEBAPP_ID = new Key<>("webAppId");
    public static final Key<String> SLOT_NAME = new Key<>("slotName");
	
    private Map<Class<? extends StorageServiceTreeItem>, Key<? extends StorageServiceTreeItem>> name2Key = ImmutableMap.of(
            BlobContainer.class, BlobExplorerFileEditorProvider.CONTAINER_KEY,
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
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                String headerMessage = getHeaderMessage(message, ex, appendEx, suggestDetail);

                String details = getDetails(ex);

                ErrorMessageForm em = new ErrorMessageForm(title);
                em.showErrorMessageForm(headerMessage, details);
                em.show();
            }
        });
    }

    @Override
    public void showError(@NotNull final String message, @NotNull final String title) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public boolean showConfirmation(@NotNull String message, @NotNull String title, @NotNull String[] options, String defaultOption) {
        int optionDialog = JOptionPane.showOptionDialog(null,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                defaultOption);
        return (optionDialog == JOptionPane.YES_OPTION);
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
        FileSaverDescriptor fileDescriptor = new FileSaverDescriptor(title, "");
        final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileDescriptor, (Project) null);
        final VirtualFileWrapper save = dialog.save(LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home")), "");

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
        LightVirtualFile itemVirtualFile = new LightVirtualFile(item.getName() + itemType);
        itemVirtualFile.putUserData((Key<T>) name2Key.get(item.getClass()), item);
        itemVirtualFile.putUserData(STORAGE_KEY, storageAccount);

        itemVirtualFile.setFileType(getFileType(itemName, iconName));

        openItem(projectObject, itemVirtualFile);
    }

    @Override
    public <T extends StorageServiceTreeItem> void openItem(Object projectObject, ClientStorageAccount clientStorageAccount, T item, String itemType, String itemName, String iconName) {
        LightVirtualFile itemVirtualFile = new LightVirtualFile(item.getName() + itemType);
        itemVirtualFile.putUserData((Key<T>) name2Key.get(item.getClass()), item);
        itemVirtualFile.putUserData(CLIENT_STORAGE_KEY, clientStorageAccount);

        itemVirtualFile.setFileType(getFileType(itemName, iconName));

        openItem(projectObject, itemVirtualFile);
    }

    @Override
    public void openItem(@NotNull final Object projectObject, @NotNull final Object itemVirtualFile) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                FileEditorManager.getInstance((Project) projectObject).openFile((VirtualFile) itemVirtualFile, true, true);
            }
        });
    }

    @org.jetbrains.annotations.NotNull
    private FileType getFileType(@NotNull final String itemName, @Nullable final String iconName) {
        return new FileType() {
            @NotNull
            @Override
            public String getName() {
                return itemName;
            }

            @NotNull
            @Override
            public String getDescription() {
                return itemName;
            }

            @NotNull
            @Override
            public String getDefaultExtension() {
                return "";
            }

            @Nullable
            @Override
            public Icon getIcon() {
                return UIHelperImpl.loadIcon(iconName);
            }

            @Override
            public boolean isBinary() {
                return true;
            }

            @Override
            public boolean isReadOnly() {
                return false;
            }

            @Override
            public String getCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] bytes) {
                return "UTF8";
            }
        };
    }

    @Override
    public void refreshQueue(@NotNull final Object projectObject, @NotNull final StorageAccount storageAccount,
                             @NotNull final Queue queue) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount.name(), queue);
                if (file != null) {
                    final QueueFileEditor queueFileEditor = (QueueFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            queueFileEditor.fillGrid();
                        }
                    });
                }
            }
        });
    }


    @Override
    public void refreshBlobs(@NotNull final Object projectObject, @NotNull final String accountName, @NotNull final BlobContainer container) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile file = (VirtualFile) getOpenedFile(projectObject, accountName, container);
                if (file != null) {
                    final BlobExplorerFileEditor containerFileEditor = (BlobExplorerFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            containerFileEditor.fillGrid();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void refreshTable(@NotNull final Object projectObject, @NotNull final StorageAccount storageAccount,
                             @NotNull final Table table) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile file = (VirtualFile) getOpenedFile(projectObject, storageAccount.name(), table);
                if (file != null) {
                    final TableFileEditor tableFileEditor = (TableFileEditor) FileEditorManager.getInstance((Project) projectObject).getEditors(file)[0];
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            tableFileEditor.fillGrid();
                        }
                    });
                }
            }
        });
    }

    @NotNull
    @Override
    public String promptForOpenSSLPath() {
        OpenSSLFinderForm openSSLFinderForm = new OpenSSLFinderForm(null);
        openSSLFinderForm.setModal(true);
        openSSLFinderForm.show();

        return DefaultLoader.getIdeHelper().getPropertyWithDefault("MSOpenSSLPath", "");
    }

    @Override
    public void openRedisPropertyView(@NotNull RedisCacheNode node) {
        String redisName = node.getName() != null ? node.getName() : RedisCacheNode.TYPE;
        String sid = node.getSubscriptionId();
        String resId = node.getResourceId();
        if (isSubscriptionIdAndResourceIdEmpty(sid, resId)) {
            return;
        }
        Project project = (Project) node.getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, RedisCachePropertyViewProvider.TYPE, resId);
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(redisName, RedisCachePropertyViewProvider.TYPE,
                    RedisCacheNode.REDISCACHE_ICON_PATH, sid, resId);
        }
        FileEditor[] editors = fileEditorManager.openFile(itemVirtualFile, true, true);
        for (FileEditor editor: editors) {
            if (editor.getName().equals(RedisCachePropertyView.ID) &&
                    editor instanceof RedisCachePropertyView) {
                ((RedisCachePropertyView) editor).onReadProperty(sid, resId);
            }
        }
    }

    @Override
    public void openRedisExplorer(RedisCacheNode redisCacheNode) {
        String redisName = redisCacheNode.getName() != null ? redisCacheNode.getName() : RedisCacheNode.TYPE;
        String sid = redisCacheNode.getSubscriptionId();
        String resId = redisCacheNode.getResourceId();
        if (isSubscriptionIdAndResourceIdEmpty(sid, resId)) {
            return;
        }
        Project project = (Project) redisCacheNode.getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, RedisCacheExplorerProvider.TYPE, resId);
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(redisName, RedisCacheExplorerProvider.TYPE,
                    RedisCacheNode.REDISCACHE_ICON_PATH, sid, resId);
        }
        fileEditorManager.openFile( itemVirtualFile, true, true);
    }

    @Override
    public void openInBrowser(String link) {
        try {
            Desktop.getDesktop().browse(URI.create(link));
        } catch (Throwable e) {
            throw new RuntimeException(UNABLE_TO_OPEN_BROWSER, e);
        }
    }

    @Override
    public void openContainerRegistryPropertyView(@NotNull ContainerRegistryNode node) {
        String registryName = node.getName() != null ? node.getName() : RedisCacheNode.TYPE;
        String sid = node.getSubscriptionId();
        String resId = node.getResourceId();
        if (isSubscriptionIdAndResourceIdEmpty(sid, resId)) {
            return;
        }
        Project project = (Project) node.getProject();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager,
                ContainerRegistryPropertyViewProvider.TYPE, resId);
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(registryName, ContainerRegistryPropertyViewProvider.TYPE,
                    ContainerRegistryNode.ICON_PATH, sid, resId);
        }
        FileEditor[] editors = fileEditorManager.openFile( itemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/);
        for (FileEditor editor: editors) {
            if (editor.getName().equals(ContainerRegistryPropertyView.ID) &&
                    editor instanceof ContainerRegistryPropertyView) {
                ((ContainerRegistryPropertyView) editor).onReadProperty(sid, resId);
            }
        }
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

    @Override
    public void openWebAppPropertyView(@NotNull final WebAppNode node) {
        final String sid = node.getSubscriptionId();
        final String webAppId = node.getWebAppId();
        final FileEditorManager fileEditorManager = getFileEditorManager(sid, webAppId, (Project) node.getProject());
        if (fileEditorManager == null) {
            return;
        }
        final String type = WebAppPropertyViewProvider.TYPE;
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, type, webAppId);
        if (itemVirtualFile == null) {
            final String iconPath = node.getParent() == null ? node.getIconPath()
                : node.getParent().getIconPath();
            itemVirtualFile = createVirtualFile(node.getWebAppName(), type, iconPath, sid, webAppId);
        }
        fileEditorManager.openFile(itemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/);
    }

    @Override
    public void openDeploymentSlotPropertyView(@NotNull DeploymentSlotNode node) {
        final String sid = node.getSubscriptionId();
        final String resourceId = node.getId();
        final FileEditorManager fileEditorManager = getFileEditorManager(sid, resourceId, (Project) node.getProject());
        if (fileEditorManager == null) {
            return;
        }
        final String type = DeploymentSlotPropertyViewProvider.TYPE;

        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, type, resourceId);
        if (itemVirtualFile == null) {
            final String iconPath = node.getParent() == null ? node.getIconPath()
                : node.getParent().getIconPath();
            final Map<Key, String> userData = new HashMap<>();
            userData.put(SUBSCRIPTION_ID, sid);
            userData.put(RESOURCE_ID, resourceId);
            userData.put(WEBAPP_ID, node.getWebAppId());
            userData.put(SLOT_NAME, node.getName());
            itemVirtualFile = createVirtualFile(node.getWebAppName() + "-" + node.getName(),
                type, iconPath, userData);
        }
        fileEditorManager.openFile(itemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/);
    }

    @Override
    public void openFunctionAppProperties(FunctionAppNode node) {
        final String subscriptionId = node.getSubscriptionId();
        final String functionAppId = node.getFunctionAppId();
        final FileEditorManager fileEditorManager = getFileEditorManager(subscriptionId, functionAppId, (Project) node.getProject());
        if (fileEditorManager == null) {
            return;
        }
        final String type = FunctionAppPropertyViewProvider.TYPE;
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, type, functionAppId);
        if (itemVirtualFile == null) {
            final String iconPath = node.getParent() == null ? node.getIconPath()
                    : node.getParent().getIconPath();
            itemVirtualFile = createVirtualFile(node.getFunctionAppName(), type, iconPath, subscriptionId, functionAppId);
        }
        fileEditorManager.openFile(itemVirtualFile, true, true);
    }

    @Nullable
    @Override
    public <T extends StorageServiceTreeItem> Object getOpenedFile(@NotNull Object projectObject,
                                                                   @NotNull String accountName,
                                                                   @NotNull T item) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) projectObject);

        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            T editedItem = editedFile.getUserData((Key<T>) name2Key.get(item.getClass()));
            StorageAccount editedStorageAccount = editedFile.getUserData(STORAGE_KEY);
            ClientStorageAccount editedClientStorageAccount = editedFile.getUserData(CLIENT_STORAGE_KEY);
            if (((editedStorageAccount != null && editedStorageAccount.name().equals(accountName)) ||
                    (editedClientStorageAccount != null && editedClientStorageAccount.getName().equals(accountName)))
                    && editedItem != null
                    && editedItem.getName().equals(item.getName())) {
                return editedFile;
            }
        }

        return null;
    }

    @Override
    public boolean isDarkTheme() {
        return UIUtil.isUnderDarcula();
    }

    @NotNull
    private static String getHeaderMessage(@NotNull String message, @Nullable Throwable ex,
                                           boolean appendEx, boolean suggestDetail) {
        String headerMessage = message.trim();

        if (ex != null && appendEx) {
            String exMessage = (ex.getLocalizedMessage() == null || ex.getLocalizedMessage().isEmpty()) ? ex.getMessage() : ex.getLocalizedMessage();
            String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + exMessage;
        }

        if (suggestDetail) {
            String separator = headerMessage.matches("^.*\\d$||^.*\\w$") ? ". " : " ";
            headerMessage = headerMessage + separator + "Click on '" + ErrorMessageForm.advancedInfoText + "' for detailed information on the cause of the error.";
        }

        return headerMessage;
    }

    @NotNull
    private static String getDetails(@Nullable Throwable ex) {
        String details = "";

        if (ex != null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            details = sw.toString();

            if (ex instanceof AzureCmdException) {
                String errorLog = ((AzureCmdException) ex).getErrorLog();
                if (errorLog != null && !errorLog.isEmpty()) {
                    details = errorLog;
                }
            }
        }

        return details;
    }

    @NotNull
    public static Icon loadIcon(@Nullable String name) {
        return IconLoader.getIcon("icons/" + name);
    }

    private LightVirtualFile searchExistingFile(FileEditorManager fileEditorManager, String fileType, String resourceId) {
        LightVirtualFile virtualFile = null;
        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            String fileResourceId = editedFile.getUserData(RESOURCE_ID);
            if (fileResourceId != null && fileResourceId.equals(resourceId) &&
                    editedFile.getFileType().getName().equals(fileType)) {
                virtualFile = (LightVirtualFile) editedFile;
                break;
            }
        }
        return virtualFile;
    }

    private LightVirtualFile createVirtualFile(String name, String type, String icon, Map<Key, String> userData) {
        LightVirtualFile itemVirtualFile = new LightVirtualFile(name);
        itemVirtualFile.setFileType(getFileType(type, icon));
        for(final Map.Entry<Key, String> data : userData.entrySet()) {
            itemVirtualFile.putUserData(data.getKey(), data.getValue());
        }
        return itemVirtualFile;
    }

    private LightVirtualFile createVirtualFile(String name, String type, String icon, String sid, String resId) {
        LightVirtualFile itemVirtualFile = new LightVirtualFile(name);
        itemVirtualFile.setFileType(getFileType(type, icon));
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
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}