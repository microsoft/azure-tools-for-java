package com.microsoft.intellij.runner.webapp.webappconfig;

import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;

public class IntelliJWebAppSettingModel extends WebAppSettingModel {

    public enum UIVersion{
        OLD,
        NEW
    }

    private UIVersion uiVersion = UIVersion.NEW;
    private boolean forceBuild = true;
    private boolean openBrowserAfterDeployment = true;
    private boolean slotPanelVisible = false;

    public UIVersion getUiVersion() {
        return uiVersion;
    }

    public void setUiVersion(UIVersion uiVersion) {
        this.uiVersion = uiVersion;
    }

    public boolean isOpenBrowserAfterDeployment() {
        return openBrowserAfterDeployment;
    }

    public void setOpenBrowserAfterDeployment(boolean openBrowserAfterDeployment) {
        this.openBrowserAfterDeployment = openBrowserAfterDeployment;
    }

    public boolean isForceBuild() {
        return forceBuild;
    }

    public void setForceBuild(boolean forceBuild) {
        this.forceBuild = forceBuild;
    }

    public boolean isSlotPanelVisible() {
        return slotPanelVisible;
    }

    public void setSlotPanelVisible(boolean slotPanelVisible) {
        this.slotPanelVisible = slotPanelVisible;
    }
}