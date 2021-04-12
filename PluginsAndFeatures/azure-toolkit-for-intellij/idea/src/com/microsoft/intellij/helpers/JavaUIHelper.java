package com.microsoft.intellij.helpers;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.toolkit.intellij.webapp.docker.ContainerRegistryPropertyView;
import com.microsoft.azure.toolkit.intellij.webapp.docker.ContainerRegistryPropertyViewProvider;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudAppNode;

import static com.microsoft.azure.toolkit.intellij.springcloud.properties.SpringCloudAppPropertiesEditorProvider.SPRING_CLOUD_APP_PROPERTY_TYPE;
import static com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper.getSubscriptionId;

public class JavaUIHelper extends UIHelperImpl {

    @Override
    public void openSpringCloudAppPropertyView(SpringCloudAppNode node) {
        Project project = (Project) node.getProject();
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            showError(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        final String id = node.getAppId();
        final String subscription = getSubscriptionId(id);
        final String appName = node.getAppName();
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager, SPRING_CLOUD_APP_PROPERTY_TYPE, id);
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(appName, subscription, id);
            itemVirtualFile.setFileType(new AzureFileType(SPRING_CLOUD_APP_PROPERTY_TYPE, AzureIconLoader.loadIcon(AzureIconSymbol.SpringCloud.MODULE)));
        }
        itemVirtualFile.putUserData(CLUSTER_ID, node.getClusterId());
        itemVirtualFile.putUserData(APP_ID, id);
        fileEditorManager.openFile(itemVirtualFile, true, true);
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
            itemVirtualFile = createVirtualFile(registryName, sid, resId);
            AzureFileType fileType = new AzureFileType(ContainerRegistryPropertyViewProvider.TYPE, AzureIconLoader.loadIcon(AzureIconSymbol.ContainerRegistry.MODULE));
            itemVirtualFile.setFileType(fileType);
        }
        FileEditor[] editors = fileEditorManager.openFile(itemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/);
        for (FileEditor editor: editors) {
            if (editor.getName().equals(ContainerRegistryPropertyView.ID) &&
                editor instanceof ContainerRegistryPropertyView) {
                ((ContainerRegistryPropertyView) editor).onReadProperty(sid, resId);
            }
        }
    }

    public void closeSpringCloudAppPropertyView(@NotNull Object projectObject, String appId) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) projectObject);
        LightVirtualFile file = searchExistingFile(fileEditorManager, SPRING_CLOUD_APP_PROPERTY_TYPE, appId);
        if (file != null) {
            AzureTaskManager.getInstance().runLater(() -> fileEditorManager.closeFile(file));
        }
    }
}
