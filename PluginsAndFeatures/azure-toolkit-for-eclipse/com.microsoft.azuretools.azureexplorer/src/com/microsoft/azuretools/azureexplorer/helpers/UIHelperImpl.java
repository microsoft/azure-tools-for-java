/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.azureexplorer.helpers;

import com.microsoft.azuretools.azureexplorer.editors.webapp.DeploymentSlotEditor;
import com.microsoft.azuretools.azureexplorer.editors.webapp.DeploymentSlotPropertyEditorInput;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;

import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.azureexplorer.editors.StorageEditorInput;
import com.microsoft.azuretools.azureexplorer.editors.container.ContainerRegistryExplorerEditor;
import com.microsoft.azuretools.azureexplorer.editors.container.ContainerRegistryExplorerEditorInput;
import com.microsoft.azuretools.azureexplorer.editors.rediscache.RedisExplorerEditor;
import com.microsoft.azuretools.azureexplorer.editors.rediscache.RedisExplorerEditorInput;
import com.microsoft.azuretools.azureexplorer.editors.webapp.WebAppPropertyEditor;
import com.microsoft.azuretools.azureexplorer.editors.webapp.WebAppPropertyEditorInput;
import com.microsoft.azuretools.azureexplorer.forms.OpenSSLFinderForm;
import com.microsoft.azuretools.azureexplorer.views.RedisPropertyView;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.model.storage.StorageServiceTreeItem;
import com.microsoft.tooling.msservices.model.storage.Table;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

public class UIHelperImpl implements UIHelper {
    private Map<Class<? extends StorageServiceTreeItem>, String> type2Editor = ImmutableMap.of(BlobContainer.class, "com.microsoft.azuretools.azureexplorer.editors.BlobExplorerFileEditor",
            Queue.class, "com.microsoft.azuretools.azureexplorer.editors.QueueFileEditor",
            Table.class, "com.microsoft.azuretools.azureexplorer.editors.TableFileEditor");

    private static final String UNABLE_TO_OPEN_BROWSER = "Unable to open external web browser";
    private static final String UNABLE_TO_GET_PROPERTY = "Error opening view page";
    private static final String UNABLE_TO_OPEN_EXPLORER = "Unable to open explorer";

    @Override
    public void showException(final String message,
                              final Throwable ex,
                              final String title,
                              final boolean appendEx,
                              final boolean suggestDetail) {
        if (Display.getCurrent() == null) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    PluginUtil.displayErrorDialogAndLog(null, title, message, ex);
                }
            });
        } else {
            PluginUtil.displayErrorDialogAndLog(null, title, message, ex);
        }
    }

    @Override
    public void showError(final String message, final String title) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                PluginUtil.displayErrorDialog(null, title, message);
            }
        });
    }

    @Override
    public boolean showConfirmation(@NotNull String message, @NotNull String title, @NotNull String[] options, String defaultOption) {
        boolean choice = MessageDialog.openConfirm(PluginUtil.getParentShell(),
                title,
                message);

        return choice;
    }

    @Override
    public void logError(String message, Throwable ex) {
        Activator.getDefault().log(message, ex);
    }

    @Override
    public File showFileChooser(String title) {
        FileDialog dialog = new FileDialog(new Shell(), SWT.SAVE);
        dialog.setOverwrite(true);
//        IProject selProject = PluginUtil.getSelectedProject();
//        if (selProject != null) {
//            String path = selProject.getLocation().toPortableString();
//            dialog.setFilterPath(path);
//        }
        dialog.setText(title);
        String fileName = dialog.open();
        if (fileName == null || fileName.isEmpty()) {
            return null;
        } else {
            return new File(fileName);
        }
    }

    @Override
    public File showFileSaver(String s, String s1) {
        // todo
        return null;
    }

    @Override
    public <T extends StorageServiceTreeItem> void openItem(Object projectObject, final ClientStorageAccount clientStorageAccount, final T item, String itemType, String itemName, String iconName) {
//        Display.getDefault().syncExec(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    BlobExplorerView view = (BlobExplorerView) PlatformUI
//                            .getWorkbench().getActiveWorkbenchWindow()
//                            .getActivePage().showView("com.microsoft.azureexplorer.views.BlobExplorerView");
//                    view.init(storageAccount, (BlobContainer) blobContainer);
//                } catch (PartInitException e) {
//                    Activator.getDefault().log("Error opening container", e);
//                }
//            }
//        });
        IWorkbench workbench=PlatformUI.getWorkbench();
        IEditorDescriptor editorDescriptor=workbench.getEditorRegistry().findEditor(type2Editor.get(item.getClass()));
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IEditorPart newEditor = page.openEditor(new StorageEditorInput(clientStorageAccount.getName(), clientStorageAccount.getConnectionString(), item), editorDescriptor.getId());
        } catch (PartInitException e) {
            Activator.getDefault().log("Error opening " + item.getName(), e);
        }
    }

    @Override
    public <T extends StorageServiceTreeItem> void openItem(Object projectObject, final StorageAccount storageAccount, final T item, String itemType, String itemName, String iconName) {
//        Display.getDefault().syncExec(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    BlobExplorerView view = (BlobExplorerView) PlatformUI
//                            .getWorkbench().getActiveWorkbenchWindow()
//                            .getActivePage().showView("com.microsoft.azureexplorer.views.BlobExplorerView");
//                    view.init(storageAccount, (BlobContainer) blobContainer);
//                } catch (PartInitException e) {
//                    Activator.getDefault().log("Error opening container", e);
//                }
//            }
//        });
        IWorkbench workbench=PlatformUI.getWorkbench();
        IEditorDescriptor editorDescriptor=workbench.getEditorRegistry().findEditor(type2Editor.get(item.getClass()));
        try {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            IEditorPart newEditor = page.openEditor(new StorageEditorInput(storageAccount.name(), StorageClientSDKManager.getConnectionString(storageAccount), item), editorDescriptor.getId());
        } catch (PartInitException e) {
            Activator.getDefault().log("Error opening " + item.getName(), e);
        }
    }

    @Override
    public void openItem(Object projectObject, Object itemVirtualFile) {
    }

    @Override
    public void refreshQueue(Object projectObject, final StorageAccount storageAccount, final Queue queue) {
        IWorkbench workbench=PlatformUI.getWorkbench();
        final IEditorDescriptor editorDescriptor=workbench.getEditorRegistry()
                .findEditor("com.microsoft.azuretools.azureexplorer.editors.QueueFileEditor");
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO
//                try {
//                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//                    QueueFileEditor newEditor = (QueueFileEditor) page.openEditor(new StorageEditorInput(storageAccount, queue), editorDescriptor.getId());
//                    newEditor.fillGrid();
//                } catch (PartInitException e) {
//                    Activator.getDefault().log("Error opening container", e);
//                }
            }
        });
    }

    @Override
    public void refreshBlobs(Object projectObject, final String accountName, final BlobContainer container) {
        IWorkbench workbench=PlatformUI.getWorkbench();
        final IEditorDescriptor editorDescriptor=workbench.getEditorRegistry()
                .findEditor("com.microsoft.azuretools.azureexplorer.editors.BlobExplorerFileEditor");
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                //TODO
//                try {
//                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//                    BlobExplorerFileEditor newEditor = (BlobExplorerFileEditor) page.openEditor(new StorageEditorInput(storageAccount, container), editorDescriptor.getId());
//                    newEditor.fillGrid();
//                } catch (PartInitException e) {
//                    Activator.getDefault().log("Error opening container", e);
//                }
            }
        });
    }

    @Override
    public void refreshTable(Object projectObject, final StorageAccount storageAccount, final Table table) {
        IWorkbench workbench=PlatformUI.getWorkbench();
        final IEditorDescriptor editorDescriptor=workbench.getEditorRegistry()
                .findEditor("com.microsoft.azuretools.azureexplorer.editors.TableFileEditor");
        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO
                /*try {
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    TableFileEditor newEditor = (TableFileEditor) page.openEditor(new StorageEditorInput(storageAccount, table), editorDescriptor.getId());
                    newEditor.fillGrid();
                } catch (PartInitException e) {
                    Activator.getDefault().log("Error opening container", e);
                }*/
            }
        });
    }

    @Override
    public String promptForOpenSSLPath() {
        OpenSSLFinderForm openSSLFinderForm = new OpenSSLFinderForm(PluginUtil.getParentShell());
        openSSLFinderForm.open();

        return DefaultLoader.getIdeHelper().getProperty("MSOpenSSLPath", "");
    }

    @Override
    public <T extends StorageServiceTreeItem> Object getOpenedFile(Object projectObject, String accountName, T blobContainer) {
        return null;
    }

    @Override
    public boolean isDarkTheme() {
        return false;
    }

    @Override
    public void openRedisPropertyView(RedisCacheNode node) {
        EventUtil.executeWithLog(TelemetryConstants.REDIS, TelemetryConstants.REDIS_READPROP, (operation) -> {
            String sid = node.getSubscriptionId();
            String resId = node.getResourceId();
            if (sid == null || resId == null) {
                return;
            }
            openView(RedisPropertyView.ID, sid, resId);
        });
    }

    @Override
    public void openRedisExplorer(@NotNull RedisCacheNode node) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        RedisExplorerEditorInput input = new RedisExplorerEditorInput(node.getSubscriptionId(),
                node.getResourceId(), node.getName());
        IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(RedisExplorerEditor.ID);
        openEditor(EditorType.REDIS_EXPLORER, input, descriptor);
    }

    @Override
    public void openDeploymentPropertyView(DeploymentNode deploymentNode) {
        // TODO Auto-generated method stub
    }

    @Override
    public void openResourceTemplateView(DeploymentNode deploymentNode, String s) {
        // TODO Auto-generated method stub
    }

    @Override
    public void openContainerRegistryPropertyView(@NotNull ContainerRegistryNode node) {
        String sid = node.getSubscriptionId();
        String resId = node.getResourceId();
        if (Utils.isEmptyString(sid) || Utils.isEmptyString(resId)) {
            return;
        }
        IWorkbench workbench = PlatformUI.getWorkbench();
        ContainerRegistryExplorerEditorInput input = new ContainerRegistryExplorerEditorInput(sid, resId, node.getName());
        IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(ContainerRegistryExplorerEditor.ID);
        openEditor(EditorType.CONTAINER_EXPLORER, input, descriptor);
    }

    @Override
    public void openInBrowser(String link) {
        try {
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(link));
        } catch (Exception e) {
            showException(UNABLE_TO_OPEN_BROWSER, e, UNABLE_TO_OPEN_BROWSER, false, false);
        }
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void openEditor(EditorType type, IEditorInput input, IEditorDescriptor descriptor) {
        try {
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
            if (activeWorkbenchWindow == null) {
                return;
            }
            IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
            if (page == null) {
                return;
            }
            switch (type) {
                case REDIS_EXPLORER:
                case CONTAINER_EXPLORER:
                case WEBAPP_EXPLORER:
                    page.openEditor(input, descriptor.getId());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            showException(UNABLE_TO_OPEN_EXPLORER, e, UNABLE_TO_OPEN_EXPLORER, false, false);
        }
    }

    private void openView(String viewId, String sid, String resId) {
        try {
            IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (activeWorkbenchWindow == null) {
                return;
            }
            IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
            if (page == null) {
                return;
            }
            switch (viewId) {
                case RedisPropertyView.ID:
                    final RedisPropertyView redisPropertyView = (RedisPropertyView) page.showView(RedisPropertyView.ID,
                            resId, IWorkbenchPage.VIEW_ACTIVATE);
                    redisPropertyView.onReadProperty(sid, resId);
                    break;
                default:
                    break;
            }
        } catch (PartInitException e) {
            showException(UNABLE_TO_GET_PROPERTY, e, UNABLE_TO_GET_PROPERTY, false, false);
        }
    }

    @Override
    public void openWebAppPropertyView(WebAppNode node) {
        if (Utils.isEmptyString(node.getId())) {
            return;
        }
        IWorkbench workbench = PlatformUI.getWorkbench();
        WebAppPropertyEditorInput input = new WebAppPropertyEditorInput(node.getSubscriptionId(), node.getId(), node.getName());
        IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(WebAppPropertyEditor.ID);
        openEditor(EditorType.WEBAPP_EXPLORER, input, descriptor);
    }

    @Override
    public void openDeploymentSlotPropertyView(final DeploymentSlotNode node) {
        if (Utils.isEmptyString(node.getId())) {
            return;
        }
        IWorkbench workbench = PlatformUI.getWorkbench();
        DeploymentSlotPropertyEditorInput input = new DeploymentSlotPropertyEditorInput(node.getId(),
            node.getSubscriptionId(), node.getWebAppId(), node.getName());
        IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(DeploymentSlotEditor.ID);
        openEditor(EditorType.WEBAPP_EXPLORER, input, descriptor);
    }

    @Override
    public void showInfo(Node node, String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public void showError(Node node, String s) {
        // TODO Auto-generated method stub
    }

}
