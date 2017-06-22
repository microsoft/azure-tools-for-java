/**
 * Copyright (c) Microsoft Corporation
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
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.forms.ErrorMessageForm;
import com.microsoft.intellij.forms.OpenSSLFinderForm;
import com.microsoft.intellij.helpers.rediscache.RedisCachePropertyView;
import com.microsoft.intellij.helpers.storage.*;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import com.microsoft.tooling.msservices.model.storage.*;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode;

import javax.swing.*;
import java.awt.Desktop;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Map;

public class UIHelperImpl implements UIHelper {
    public static Key<StorageAccount> STORAGE_KEY = new Key<StorageAccount>("storageAccount");
    public static Key<ClientStorageAccount> CLIENT_STORAGE_KEY = new Key<ClientStorageAccount>("clientStorageAccount");
    public static final Key<String> SUBSCRIPTION_ID = new Key<>("subscriptionId");
    public static final Key<String> RESOURCE_ID = new Key<>("resourceId");
    private Map<Class<? extends StorageServiceTreeItem>, Key<? extends StorageServiceTreeItem>> name2Key = ImmutableMap.of(BlobContainer.class, BlobExplorerFileEditorProvider.CONTAINER_KEY,
            Queue.class, QueueExplorerFileEditorProvider.QUEUE_KEY,
            Table.class, TableExplorerFileEditorProvider.TABLE_KEY);

    private static final String UNABLE_TO_OPEN_BROWSER = "Unable to open external web browser";


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
     */
    @Override
    public File showFileChooser(String title) {
        JFileChooser saveFile = new JFileChooser();
        saveFile.setDialogTitle(title);
        if (saveFile.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            return saveFile.getSelectedFile();
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
        }, ModalityState.any());
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
        if (sid == null || resId == null) {
            return;
        }
        Project project = (Project) node.getProject();
        LightVirtualFile itemVirtualFile = null;
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            return;
        }
        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            String fileSid = editedFile.getUserData(SUBSCRIPTION_ID);
            if (fileSid != null && fileSid.equals(sid) &&
                    editedFile.getName().equals(redisName) &&
                    editedFile.getFileType().getName().equals(RedisCacheNode.TYPE)) {
                itemVirtualFile = (LightVirtualFile) editedFile;
                break;
            }
        }
        if (itemVirtualFile == null) {
            itemVirtualFile = new LightVirtualFile(redisName);
            itemVirtualFile.setFileType(getFileType(RedisCacheNode.TYPE, RedisCacheNode.REDISCACHE_ICON_PATH));
            itemVirtualFile.putUserData(SUBSCRIPTION_ID, sid);
            itemVirtualFile.putUserData(RESOURCE_ID, resId);
        }
        FileEditor[] editors = fileEditorManager.openFile( itemVirtualFile, true, true);
        ApplicationManager.getApplication().invokeLater(() -> {
            for (FileEditor editor: editors) {
                if (editor.getName().equals(RedisCachePropertyView.ID) &&
                        editor instanceof RedisCachePropertyView) {
                    ((RedisCachePropertyView) editor).readProperty(sid, resId);
                }
            }
        }, ModalityState.any());
    }

    @Override
    public void openRedisExplorer(RedisCacheNode redisCacheNode) {
        //TODO: implement for intellij
    }

    @Override
    public void openInBrowser(String link) {
        try {
            Desktop.getDesktop().browse(URI.create(link));
        } catch (Exception e) {
            showException(UNABLE_TO_OPEN_BROWSER, e, UNABLE_TO_OPEN_BROWSER, false, false);
        }
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
    public static ImageIcon loadIcon(@Nullable String name) {
        java.net.URL url = UIHelperImpl.class.getResource("/icons/" + name);
        return new ImageIcon(url);
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}