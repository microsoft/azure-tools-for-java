package com.microsoft.intellij.helpers.webapp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotPropertyViewPresenter;

public class DeploymentSlotPropertyView extends WebAppBasePropertyView {

    /**
     * Initialize the Web App Property View and return it.
     */
    public static WebAppBasePropertyView create(@NotNull Project project, @NotNull String sid,
                                                @NotNull String resId, @NotNull String name) {
        DeploymentSlotPropertyView view = new DeploymentSlotPropertyView(project, sid, resId, name);
        view.onLoadWebAppProperty(sid, resId, name);
        return view;
    }

    private DeploymentSlotPropertyView(@NotNull final Project project, @NotNull final String sid,
                                       @NotNull final String webAppId, @NotNull final String name) {
        super(project, sid, webAppId);
        presenter = new DeploymentSlotPropertyViewPresenter();
        presenter.onAttachView(this);
    }

    @Override
    public void onLoadWebAppProperty(@NotNull String sid, @NotNull String webAppId, @Nullable String name) {
        this.presenter.onLoadWebAppProperty(sid, webAppId, name);
    }
}
