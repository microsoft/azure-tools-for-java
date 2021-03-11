/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

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
import com.intellij.ui.components.JBScrollPane;
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
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AzureSdkPackageGroupPanel {
    @Getter
    private JPanel contentPanel;
    private JPanel codePanel;
    private JBTable packagesTable;
    private JBScrollPane tableContainer;
    private EditorEx editor;
    private ListTableModel<AzureSdkPackageEntity> packagesTableModel;

    public AzureSdkPackageGroupPanel() {
        this.$$$setupUI$$$();
        initCodeSnippetPanel();
    }

    public void setData(@Nonnull final List<? extends AzureSdkPackageEntity> packages) {
        this.editor.getDocument().setText("");
        this.packagesTableModel.setItems(new ArrayList<>(packages));
        if (packages.size() > 0) {
            this.packagesTable.setVisible(true);
            this.packagesTable.setRowSelectionInterval(0, 0);
            this.packagesTable.getSelectionModel().setSelectionInterval(0, 0);
            this.onPackageSelected(packages.get(0));
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
        settings.setAnimatedScrolling(false);
        settings.setRefrainFromScrolling(false);
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        this.codePanel.add(this.editor.getComponent(), BorderLayout.CENTER);
    }

    private void initPackagesTable() {
        final PackageTableColumn nameColumn = new PackageTableColumn(PackageTableColumn.NAME);
        final PackageTableColumn versionColumn = new PackageTableColumn(PackageTableColumn.VERSION);
        final PackageTableColumn linksColumn = new PackageTableColumn(PackageTableColumn.LINKS);
        this.packagesTableModel = new ListTableModel<>(nameColumn, versionColumn, linksColumn);
        this.packagesTable = new TableView<>(this.packagesTableModel);
        final TableColumnModel columnModel = this.packagesTable.getColumnModel();
        this.packagesTable.setTableHeader(new JTableHeader(columnModel));
        ((DefaultTableCellRenderer) this.packagesTable.getTableHeader().getDefaultRenderer())
            .setHorizontalAlignment(JLabel.LEADING);
        columnModel.getColumn(0).setPreferredWidth(80);
        columnModel.getColumn(1).setPreferredWidth(80);
        this.packagesTable.setRowSelectionAllowed(true);
        this.packagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.packagesTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.packagesTable.setVisibleRowCount(3);
        this.packagesTable.setBorder(BorderFactory.createEmptyBorder());
        this.packagesTable.getSelectionModel().addListSelectionListener((e) -> {
            final int row = this.packagesTable.getSelectedRow();
            if (row >= 0) {
                final AzureSdkPackageEntity pkg = this.packagesTableModel.getRowValue(row);
                this.onPackageSelected(pkg);
            }
        });
    }

    private static class PackageTableColumn extends ColumnInfo<AzureSdkPackageEntity, Object> {
        private static final String NAME = "Name";
        private static final String VERSION = "Version";
        private static final String LINKS = "Links";

        public PackageTableColumn(String name) {
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
                    return "Javadoc";
            }
            return o.getArtifact();
        }

        @Override
        public TableCellRenderer getRenderer(AzureSdkPackageEntity entity) {
            if (NAME.equals(getName())) {
                return null;
            }
            return new PackageTableCellRenderer(getName());
        }
    }

    public static class PackageTableCellRenderer extends JPanel implements TableCellRenderer {

        private PackageTableCellRenderer(String name) {
            super();
            this.setLayout(new BorderLayout());
            this.setName(name);
        }

        public JComponent getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final AzureSdkVersionTag tag = new AzureSdkVersionTag();
            this.add(tag.getTagPanel(), BorderLayout.WEST);
            if (PackageTableColumn.LINKS.equals(getName())) {
                tag.setName("Javadoc");
                tag.setValue("3.12.0-FAKE");
            }
            if (PackageTableColumn.VERSION.equals(getName())) {
                tag.setName("Maven");
                tag.setValue(value);
            }
            return this;
        }
    }

    private void createUIComponents() {
        initPackagesTable();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    public void $$$setupUI$$$() {
    }
}
