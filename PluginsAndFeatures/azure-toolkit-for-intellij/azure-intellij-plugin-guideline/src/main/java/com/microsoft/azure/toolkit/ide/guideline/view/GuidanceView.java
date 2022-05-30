package com.microsoft.azure.toolkit.ide.guideline.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.config.ProcessConfig;

import javax.annotation.Nonnull;
import javax.swing.*;

public class GuidanceView extends SimpleToolWindowPanel {
    private Project project;
    private JPanel pnlRoot;
    private ProcessView pnlProcess;
    private WelcomeView pnlWelcome;

    public GuidanceView(final Project project) {
        super(true);
        this.project = project;
        $$$setupUI$$$();
        this.setContent(pnlRoot);
        showWelcomePage();
    }

    public void showWelcomePage() {
        pnlProcess.setVisible(false);
        pnlWelcome.setVisible(true);
    }

    public void showProcess(@Nonnull Process process) {
        pnlWelcome.setVisible(false);
        pnlProcess.setVisible(true);
        pnlProcess.showProcess(process);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.pnlProcess = new ProcessView(project);
        this.pnlWelcome = new WelcomeView(project);
    }
}
