package com.microsoft.intellij.runner.webapp.webappconfig;

import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;

public class IntelliJWebAppSettingModel extends WebAppSettingModel {

    public static final String OLD_UI = "oldui";
    public static final String NEW_UI = "newui";

    private String uiVersion = NEW_UI;
    private boolean openBrowserAfterDeployment = true;
    private boolean compileBeforeDeploy = true;
    private boolean slotPanelVisiable = false;

    public String getUiVersion() {
        return uiVersion;
    }

    public void setUiVersion(String uiVersion) {
        this.uiVersion = uiVersion;
    }

    public boolean isOpenBrowserAfterDeployment() {
        return openBrowserAfterDeployment;
    }

    public void setOpenBrowserAfterDeployment(boolean openBrowserAfterDeployment) {
        this.openBrowserAfterDeployment = openBrowserAfterDeployment;
    }

    public boolean isCompileBeforeDeploy() {
        return compileBeforeDeploy;
    }

    public void setCompileBeforeDeploy(boolean compileBeforeDeploy) {
        this.compileBeforeDeploy = compileBeforeDeploy;
    }

    public boolean isSlotPanelVisiable() {
        return slotPanelVisiable;
    }

    public void setSlotPanelVisiable(boolean slotPanelVisiable) {
        this.slotPanelVisiable = slotPanelVisiable;
    }
}