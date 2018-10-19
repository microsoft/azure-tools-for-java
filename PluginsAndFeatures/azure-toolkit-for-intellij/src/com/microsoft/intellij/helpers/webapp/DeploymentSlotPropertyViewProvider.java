package com.microsoft.intellij.helpers.webapp;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.helpers.UIHelperImpl;

public class DeploymentSlotPropertyViewProvider extends WebAppBasePropertyViewProvider {
    public static final String TYPE ="DEPLOYMENT_SLOT_PROPERTY";

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        final String sid = virtualFile.getUserData(UIHelperImpl.SUBSCRIPTION_ID);
        final String id = virtualFile.getUserData(UIHelperImpl.RESOURCE_ID);
        final String name = virtualFile.getName();
        return DeploymentSlotPropertyView.create(project, sid, id, name);
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
