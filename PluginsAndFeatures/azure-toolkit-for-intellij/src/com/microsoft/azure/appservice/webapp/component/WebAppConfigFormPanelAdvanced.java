package com.microsoft.azure.appservice.webapp.component;

import com.intellij.ui.components.JBRadioButton;
import com.microsoft.azure.appservice.component.form.AzureFormPanel;
import com.microsoft.azure.appservice.component.form.input.*;
import com.microsoft.azure.appservice.webapp.WebAppConfig;
import com.microsoft.azure.management.resources.Subscription;

import javax.swing.*;
import java.awt.event.ItemEvent;

public class WebAppConfigFormPanelAdvanced extends JPanel implements AzureFormPanel<WebAppConfig> {
    private JPanel contentPanel;

    protected ComboBoxSubscription selectorSubscription;

    protected JTextField textName;
    protected ComboBoxPlatform selectorRuntime;
    protected JBRadioButton radioOsLinux;
    protected JBRadioButton radioOsWindows;
    protected ComboBoxRegion selectorRegion;

    protected JLabel textSku;
    protected ComboBoxDeployment selectorApplication;
    protected ComboBoxResourceGroup selectorGroup;
    protected ComboBoxServicePlan selectorServicePlan;

    public WebAppConfigFormPanelAdvanced() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.bind();
    }

    private void bind() {
        this.selectorSubscription.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final Subscription subscription = (Subscription) e.getItem();
                this.selectorGroup.refreshWith(subscription);
                this.selectorServicePlan.refreshWith(subscription);
                this.selectorRegion.refreshWith(subscription);
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.selectorGroup.clear();
                this.selectorServicePlan.clear();
                this.selectorRegion.clear();
            }
        });
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    @Override
    public WebAppConfig getData() {
        return null;
    }
}
