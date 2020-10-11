package com.microsoft.azure.appservice.webapp.component;

import com.microsoft.azure.appservice.Platform;
import com.microsoft.azure.appservice.component.form.AzureFormPanel;
import com.microsoft.azure.appservice.component.form.input.ComboBoxDeployment;
import com.microsoft.azure.appservice.component.form.input.ComboBoxPlatform;
import com.microsoft.azure.appservice.webapp.WebAppConfig;

import javax.swing.*;
import java.nio.file.Path;

public class WebAppConfigFormPanelBasic extends JPanel implements AzureFormPanel<WebAppConfig> {
    private JPanel contentPanel;

    protected JTextField textName;
    protected ComboBoxPlatform selectorPlatform;
    protected ComboBoxDeployment selectorApplication;

    public JPanel getContentPanel() {
        return contentPanel;
    }

    @Override
    public WebAppConfig getData() {
        final String name = this.textName.getText();
        final Platform platform = this.selectorPlatform.getValue();
        final Path path = this.selectorApplication.getValue();
        final WebAppConfig config = new WebAppConfig();
        config.setName(name);
        config.setApplication(path);
        config.setPlatform(platform);
        return config;
    }
}
