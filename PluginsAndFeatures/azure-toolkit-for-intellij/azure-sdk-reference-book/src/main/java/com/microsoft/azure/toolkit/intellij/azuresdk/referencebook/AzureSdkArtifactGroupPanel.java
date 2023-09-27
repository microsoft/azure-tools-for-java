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
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity.DependencyType;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.intellij.util.GradleUtils;
import com.microsoft.intellij.util.MavenUtils;
import icons.GradleIcons;
import icons.OpenapiIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity.DependencyType.GRADLE;
import static com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity.DependencyType.MAVEN;

public class AzureSdkArtifactGroupPanel {
    @Getter
    private JPanel contentPanel;
    private EditorTextField viewer;
    private JPanel artifactsPnl;
    private ActionToolbarImpl toolbar;
    private AzureSdkProjectDependencyPanel pnlAddDependencies;
    private AzureSdkArtifactExamplePanel pnlExample;
    private JPanel tabDependency;
    private JPanel tabExamples;
    private HyperlinkLabel linkRequestExamples;
    private JTabbedPane tabbedPane;
    private ButtonGroup artifactsGroup;
    private final List<AzureSdkArtifactDetailPanel> artifactPnls = new ArrayList<>();
    private AzureSdkArtifactEntity pkg;
    private String version;
    private static DependencyType type = MAVEN;

    private final Project project;

    public AzureSdkArtifactGroupPanel(@Nullable Project project) {
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        if (!MavenUtils.isMavenProject(project) && !GradleUtils.isGradleProject(project)) {
            pnlAddDependencies.setVisible(false);
        }
    }

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

    @AzureOperation(name = "user/sdk.select_artifact.artifact", params = "pkg.getArtifactId()")
    private void onPackageOrVersionSelected(AzureSdkArtifactEntity pkg, String version) {
        OperationContext.action().setTelemetryProperty("artifact", pkg.getArtifactId());
        this.pkg = pkg;
        this.version = version;
        this.viewer.setText(pkg.getDependencySnippet(type, version));
        this.pnlAddDependencies.setPkg(pkg);
        this.pnlAddDependencies.setVersion(version);
        this.pnlAddDependencies.onSelectModule();
        this.pnlExample.setArtifact(pkg);
    }

    private void onDependencyTypeSelected(DependencyType type) {
        AzureSdkArtifactGroupPanel.type = type;
        final FileType fileType = FileTypeManagerEx.getInstance().getFileTypeByExtension(type.getFileExt());
        this.viewer.setNewDocumentAndFileType(fileType, new DocumentImpl(pkg.getDependencySnippet(type, version)));
    }

    private EditorTextField buildCodeViewer() {
        final Project project = ProjectManager.getInstance().getDefaultProject();
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
            @AzureOperation(name = "user/sdk.copy_dependency_configuration")
            public void actionPerformed(@NotNull final AnActionEvent e) {
                OperationContext.action().setTelemetryProperty("feature", pkg.getArtifactId());
                OperationContext.action().setTelemetryProperty("tools", AzureSdkArtifactGroupPanel.type.getName());
                CopyPasteManager.getInstance().setContents(new StringSelection(viewer.getText()));
            }
        });
        group.add(new DependencyTypeSelector(this::onDependencyTypeSelected, AzureSdkArtifactGroupPanel.type));
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
        this.pnlAddDependencies = new AzureSdkProjectDependencyPanel(project);
    }

    /**
     * referred com.intellij.application.options.schemes.AbstractSchemesPanel.ShowSchemesActionsListAction
     *
     * @see com.intellij.application.options.schemes.AbstractSchemesPanel
     */
    private static class DependencyTypeSelector extends DefaultActionGroup {
        private final Consumer<? super DependencyType> onTypeSelected;
        private DependencyType selectedType;

        private DependencyTypeSelector(Consumer<? super DependencyType> onTypeSelected, DependencyType type) {
            super();
            setPopup(true);
            this.onTypeSelected = onTypeSelected;
            this.selectedType = type;
            final AnAction maven = createAction(MAVEN.getName(), OpenapiIcons.RepositoryLibraryLogo, () -> this.setSelectedType(MAVEN));
            final AnAction gradle = createAction(GRADLE.getName(), GradleIcons.Gradle, () -> this.setSelectedType(GRADLE));
            this.addAll(maven, gradle);
        }

        private void setSelectedType(DependencyType type) {
            this.selectedType = type;
            this.onTypeSelected.accept(type);
        }

        @Override
        public void update(@NotNull final AnActionEvent e) {
            final Icon icon = GRADLE == selectedType ? GradleIcons.Gradle : OpenapiIcons.RepositoryLibraryLogo;
            e.getPresentation().setIcon(icon);
        }

        private AnAction createAction(final String name, final Icon icon, final Runnable onSelected) {
            return new AnAction(name, null, icon) {
                @Override
                public void actionPerformed(@NotNull final AnActionEvent e) {
                    onSelected.run();
                }
            };
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    public void $$$setupUI$$$() {
    }
}
