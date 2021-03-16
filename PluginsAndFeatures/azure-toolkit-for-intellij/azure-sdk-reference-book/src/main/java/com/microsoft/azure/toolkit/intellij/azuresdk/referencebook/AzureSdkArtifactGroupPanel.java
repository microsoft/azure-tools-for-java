/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.EditorTextField;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class AzureSdkArtifactGroupPanel {
    @Getter
    private JPanel contentPanel;
    private EditorTextField viewer;
    private JPanel artifactsPnl;
    private ActionToolbarImpl toolbar;
    private ButtonGroup artifactsGroup;
    private final List<AzureSdkArtifactDetailPanel> artifactPnls = new ArrayList<>();

    public void setData(@Nonnull final List<? extends AzureSdkArtifactEntity> artifacts) {
        this.clear();
        if (artifacts.size() > 0) {
            for (final AzureSdkArtifactEntity pkg : artifacts) {
                final AzureSdkArtifactDetailPanel artifactPnl = buildArtifactPanel(pkg);
                this.artifactsPnl.add(artifactPnl.getContentPanel());
                this.artifactPnls.add(artifactPnl);
            }
            this.artifactPnls.get(0).setSelected(true);
        }
    }

    private void clear() {
        this.viewer.setText("");
        this.artifactPnls.forEach(p -> p.detachFromGroup(this.artifactsGroup));
        this.artifactPnls.clear();
        this.artifactsPnl.removeAll();
    }

    private void onPackageOrVersionSelected(AzureSdkArtifactEntity pkg, String version) {
        this.viewer.getDocument().setText(pkg.generateMavenDependencySnippet(version));
    }

    private EditorTextField buildCodeViewer() {
        final Project project = ProjectManager.getInstance().getOpenProjects()[0];
        final DocumentImpl document = new DocumentImpl("", true);
        final EditorTextField viewer = new EditorTextField(document, project, XmlFileType.INSTANCE, true, false);
        viewer.addSettingsProvider(editor -> { // add scrolling/line number features
            editor.setHorizontalScrollbarVisible(true);
            editor.setVerticalScrollbarVisible(true);
            editor.getSettings().setLineNumbersShown(true);
        });
        return viewer;
    }

    private ActionToolbarImpl buildCodeViewerToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction(ActionsBundle.message("action.$Copy.text"), ActionsBundle.message("action.$Copy.description"), AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent e) {
                CopyPasteManager.getInstance().setContents(new StringSelection(viewer.getText()));
            }
        });
        return new ActionToolbarImpl(ActionPlaces.TOOLBAR, group, false);
    }

    private JPanel buildArtifactsPanel() {
        final JPanel panel = new JPanel();
        final BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);
        return panel;
    }

    private AzureSdkArtifactDetailPanel buildArtifactPanel(AzureSdkArtifactEntity artifact) {
        final AzureSdkArtifactDetailPanel artifactPnl = new AzureSdkArtifactDetailPanel(artifact);
        artifactPnl.attachToGroup(artifactsGroup);
        artifactPnl.setOnArtifactOrVersionSelected(this::onPackageOrVersionSelected);
        final JPanel contentPanel = artifactPnl.getContentPanel();
        final Dimension maximum = contentPanel.getMaximumSize();
        final Dimension preferred = contentPanel.getPreferredSize();
        contentPanel.setMaximumSize(new Dimension(maximum.width, preferred.height));
        return artifactPnl;
    }

    private void createUIComponents() {
        this.artifactsPnl = this.buildArtifactsPanel();
        this.viewer = this.buildCodeViewer();
        this.toolbar = this.buildCodeViewerToolbar();
        this.toolbar.setForceMinimumSize(true);
        this.toolbar.setTargetComponent(this.viewer);
    }
}
