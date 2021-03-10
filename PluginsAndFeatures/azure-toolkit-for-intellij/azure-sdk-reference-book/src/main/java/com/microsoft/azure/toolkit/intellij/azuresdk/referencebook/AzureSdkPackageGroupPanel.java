/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.XHtmlFileType;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkPackageEntity;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

public class AzureSdkPackageGroupPanel {
    @Getter
    private JPanel contentPanel;
    private JPanel codePanel;
    private JBTable packagesTable;
    private EditorEx editor;
    private ListTableModel<AzureSdkPackageEntity> packagesTableModel;

    public AzureSdkPackageGroupPanel() {
        this.$$$setupUI$$$();
        initCodeSnippetPanel();
    }

    public void setData(@Nonnull final List<AzureSdkPackageEntity> packages) {
        this.editor.getDocument().setText("");
        this.packagesTableModel.setItems(packages);
        if (packages.size() > 0) {
            this.packagesTable.setVisible(true);
            this.packagesTable.setRowSelectionInterval(0, 0);
            this.packagesTable.getSelectionModel().setSelectionInterval(0, 0);
            this.onPackageSelected(packages.get(0));
            this.packagesTableModel.addRow(AzureSdkPackageEntity.builder().artifact(null).build());
        } else {
            this.packagesTable.setVisible(false);
        }
    }

    private void onPackageSelected(AzureSdkPackageEntity pkg) {
        this.editor.getDocument().setText(pkg.generateMavenDependencySnippet());
    }

    private void initCodeSnippetPanel() {
        final DocumentImpl document = new DocumentImpl("", true);
        this.editor = (EditorEx) EditorFactory.getInstance().createViewer(document);
        final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        final Project project = ProjectManager.getInstance().getOpenProjects()[0];
        final EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, XHtmlFileType.INSTANCE);
        this.editor.setHighlighter(highlighter);
        this.editor.getGutterComponentEx().setForceShowRightFreePaintersArea(true);
        this.editor.getFoldingModel().setFoldingEnabled(false);
        final EditorSettings settings = this.editor.getSettings();
        this.editor.getDocument().setText("<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>Title</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "\n" +
            "</body>\n" +
            "</html>");
        settings.setAnimatedScrolling(false);
        settings.setRefrainFromScrolling(false);
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        this.codePanel.add(this.editor.getComponent(), BorderLayout.CENTER);
    }

    private void createUIComponents() {
        final PackageColumn nameColumn = new PackageColumn(PackageColumn.NAME);
        final PackageColumn versionColumn = new PackageColumn(PackageColumn.VERSION);
        final PackageColumn linksColumn = new PackageColumn(PackageColumn.LINKS);
        this.packagesTableModel = new ListTableModel<>(nameColumn, versionColumn, linksColumn);
        this.packagesTable = new TableView<>(this.packagesTableModel);
        this.packagesTable.setTableHeader(new JTableHeader(this.packagesTable.getColumnModel()));
        ((DefaultTableCellRenderer) this.packagesTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.LEADING);
        this.packagesTable.setRowSelectionAllowed(true);
        this.packagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.packagesTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.packagesTable.setVisibleRowCount(3);
        this.packagesTable.getSelectionModel().addListSelectionListener((e) -> {
            final int row = this.packagesTable.getSelectedRow();
            if (row >= 0 && row + 1 != this.packagesTable.getRowCount()) {
                final AzureSdkPackageEntity pkg = this.packagesTableModel.getRowValue(row);
                this.onPackageSelected(pkg);
            } else {
                this.packagesTable.clearSelection();
            }
        });
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    public void $$$setupUI$$$() {
    }

    private static class PackageColumn extends ColumnInfo<AzureSdkPackageEntity, String> {
        private static final String NAME = "Name";
        private static final String VERSION = "Version";
        private static final String LINKS = "Links";

        public PackageColumn(String name) {
            super(name);
        }

        @Nullable
        @Override
        public String valueOf(AzureSdkPackageEntity o) {
            if (o.getArtifact() == null) {
                return "";
            }
            switch (getName()) {
                case NAME:
                    return o.getArtifact();
                case VERSION:
                    return StringUtils.firstNonBlank(o.getVersionGA(), "3.12.0-FAKE");
                case LINKS:
                    return "Maven";
            }
            return o.getArtifact();
        }

        @Override
        public @Nullable Icon getIcon() {
            if (getName().equals(VERSION)) {
                return AllIcons.Providers.Microsoft;
            }
            return null;
        }

        @Override
        public TableCellRenderer getCustomizedRenderer(AzureSdkPackageEntity entity, TableCellRenderer renderer) {
            return super.getCustomizedRenderer(entity, renderer);
        }
    }
}
