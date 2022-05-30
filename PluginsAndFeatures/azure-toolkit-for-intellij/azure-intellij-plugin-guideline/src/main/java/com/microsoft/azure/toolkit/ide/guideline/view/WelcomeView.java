package com.microsoft.azure.toolkit.ide.guideline.view;

import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBFont;
import com.microsoft.azure.toolkit.ide.guideline.GuidanceConfigManager;
import com.microsoft.azure.toolkit.ide.guideline.GuidanceViewManager;
import com.microsoft.azure.toolkit.ide.guideline.config.ProcessConfig;
import com.microsoft.azure.toolkit.ide.guideline.view.components.ProcessPanel;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

public class WelcomeView {
    private JPanel pnlRoot;
    private JPanel pnlProcesses;
    private JLabel lblTitle;

    private final Project project;

    public WelcomeView(@Nonnull Project project) {
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.lblTitle.setFont(JBFont.h1());
        fillProcess();
    }

    private void fillProcess() {
        final List<ProcessConfig> processConfigs = GuidanceConfigManager.getInstance().loadProcessConfig();
        pnlProcesses.setLayout(new GridLayoutManager(processConfigs.size(), 1));
        for (int i = 0; i < processConfigs.size(); i++) {
            final ProcessConfig processConfig = processConfigs.get(i);
            final ProcessPanel processPanel = new ProcessPanel(processConfig);
            processPanel.setStartListener(e -> GuidanceViewManager.getInstance().showGuidance(project, processConfig));
            final GridConstraints gridConstraints = new GridConstraints(i, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
            pnlProcesses.add(processPanel, gridConstraints);
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    public void setVisible(boolean visible) {
        this.pnlRoot.setVisible(visible);
    }
}
