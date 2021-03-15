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
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkPackageEntity;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class AzureSdkPackageGroupPanel {
    @Getter
    private JPanel contentPanel;
    private EditorTextField viewer;
    private JPanel pkgsPnl;
    private ActionToolbarImpl toolbar;
    private ButtonGroup pkgsGroup;
    private final List<AzureSdkPackageDetailPanel> pkgPnls = new ArrayList<>();

    public void setData(@Nonnull final List<? extends AzureSdkPackageEntity> packages) {
        this.clear();

        if (packages.size() > 0) {
            for (final AzureSdkPackageEntity pkg : packages) {
                final AzureSdkPackageDetailPanel pkgPnl = new AzureSdkPackageDetailPanel(pkg);
                pkgPnl.attachToGroup(pkgsGroup);
                pkgPnl.setOnPackageOrVersionSelected(this::onPackageOrVersionSelected);
                final JPanel contentPanel = pkgPnl.getContentPanel();
                final Dimension maximum = contentPanel.getMaximumSize();
                final Dimension preferred = contentPanel.getPreferredSize();
                contentPanel.setMaximumSize(new Dimension(maximum.width, preferred.height));
                this.pkgsPnl.add(contentPanel);
                this.pkgPnls.add(pkgPnl);
            }
            this.pkgPnls.get(0).setSelected(true);
        }
    }

    private void clear() {
        this.viewer.setText("");
        this.pkgPnls.forEach(p -> p.detachFromGroup(this.pkgsGroup));
        this.pkgPnls.clear();
        this.pkgsPnl.removeAll();
    }

    private void onPackageOrVersionSelected(AzureSdkPackageEntity pkg, String version) {
        this.viewer.getDocument().setText(pkg.generateMavenDependencySnippet(version));
    }

    private EditorTextField initCodeViewer() {
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

    private ActionToolbarImpl initCodeViewerToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction(ActionsBundle.message("action.$Copy.text"), ActionsBundle.message("action.$Copy.description"), AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent e) {
                CopyPasteManager.getInstance().setContents(new StringSelection(viewer.getText()));
            }
        });
        return new ActionToolbarImpl(ActionPlaces.TOOLBAR, group, false);
    }

    private JPanel initPackagesPanel() {
        final JPanel panel = new JPanel();
        final BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);
        return panel;
    }

    private void createUIComponents() {
        this.pkgsPnl = this.initPackagesPanel();
        this.viewer = this.initCodeViewer();
        this.toolbar = this.initCodeViewerToolbar();
        this.toolbar.setForceMinimumSize(true);
        this.toolbar.setTargetComponent(this.viewer);
    }
}
