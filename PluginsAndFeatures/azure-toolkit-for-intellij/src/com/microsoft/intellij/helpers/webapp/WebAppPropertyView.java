package com.microsoft.intellij.helpers.webapp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppPropertyViewPresenter;

public class WebAppPropertyView extends WebAppBasePropertyView {

    /**
     * Initialize the Web App Property View and return it.
     */
    public static WebAppBasePropertyView create(@NotNull final Project project, @NotNull final String sid,
                                                @NotNull final String webAppId) {
        WebAppPropertyView view = new WebAppPropertyView(project, sid, webAppId);
        view.onLoadWebAppProperty(sid, webAppId, null);
        return view;
    }

    private WebAppPropertyView(@NotNull final Project project, @NotNull final String sid,
                               @NotNull final String webAppId) {
        super(project, sid, webAppId);
        presenter = new WebAppPropertyViewPresenter();
        presenter.onAttachView(this);
    }

    @Override
    public void onLoadWebAppProperty(@NotNull final String sid, @NotNull final String webAppId, @Nullable final String name) {
        this.presenter.onLoadWebAppProperty(sid, webAppId, name);
    }
}
