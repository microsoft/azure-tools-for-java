package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.component.RoundCornerPanel;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class AzureSdkVersionTag {
    private JPanel panel1;
    @Getter
    private RoundCornerPanel tagPanel;
    private RoundCornerPanel valPanel;
    private JLabel valLabel;
    private JLabel nameLabel;

    private void createUIComponents() {
        final JBColor blue = new JBColor(new Color(88, 157, 246), new Color(75, 121, 255));
        final Color white = Color.WHITE;
        this.tagPanel = new RoundCornerPanel(new GridLayoutManager(1, 1, JBUI.insets(4, 4, 4, 4), -1, -1), 8, blue);
        this.valPanel = new RoundCornerPanel(new GridLayoutManager(1, 1, JBUI.insets(2, 2, 2, 2), -1, -1), 5, white);
        this.valPanel.setBackground(blue);
    }

    public void setName(String name) {
        this.nameLabel.setText(name);
    }

    public void setValue(Object value) {
        this.valLabel.setText(value + "");
    }
}
