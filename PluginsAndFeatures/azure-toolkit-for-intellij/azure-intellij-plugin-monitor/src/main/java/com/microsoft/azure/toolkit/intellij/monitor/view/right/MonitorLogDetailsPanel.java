/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.right;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.intellij.ide.DataManager;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.EditorTextField;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;

public class MonitorLogDetailsPanel {
    @Getter
    private JPanel contentPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JPanel cellDetailsPanel;
    private JLabel cellTitle;
    private EditorTextField cellContentViewer;

    public MonitorLogDetailsPanel() {
        $$$setupUI$$$();
        this.cellTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
    }

    public void setStatus(String statusText) {
        this.cellDetailsPanel.setVisible(false);
        this.statusPanel.setVisible(true);
        this.statusLabel.setText(statusText);
    }

    @AzureOperation(name = "user/monitor.select_table_cell")
    public void setViewText(String viewerTitle, String viewerText) {
        this.cellDetailsPanel.setVisible(true);
        this.statusPanel.setVisible(false);
        this.cellTitle.setText(viewerTitle);
        final String formattedString = getValidJsonString(viewerText);
        if (Objects.nonNull(formattedString)) {
            this.cellContentViewer.setFileType(JsonFileType.INSTANCE);
            this.cellContentViewer.setText(formattedString);
        } else {
            this.cellContentViewer.setFileType(PlainTextFileType.INSTANCE);
            this.cellContentViewer.setText(viewerText);
        }
    }

    private EditorTextField createEditorTextField() {
        final Project project = DataManager.getInstance().getDataContext(this.contentPanel).getData(CommonDataKeys.PROJECT);
        final DocumentImpl document = new DocumentImpl("", true);
        final EditorTextField result = new EditorTextField(document, project, PlainTextFileType.INSTANCE, true, false);
        result.addSettingsProvider(editor -> { // add scrolling/line number features/show gutter/soft wrap
            editor.setHorizontalScrollbarVisible(true);
            editor.setVerticalScrollbarVisible(true);
            editor.getSettings().setLineNumbersShown(true);
            editor.getSettings().setGutterIconsShown(true);
            editor.getSettings().setUseSoftWraps(true);
        });
        return result;
    }

    @Nullable
    public String getValidJsonString(String jsonString) {
        try {
            final ObjectMapper objectMapper = new JsonMapper();
            final Object jsonObj = objectMapper.readValue(jsonString, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObj);
        } catch (final Exception e) {
            return null;
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        this.cellContentViewer = createEditorTextField();
    }
}
