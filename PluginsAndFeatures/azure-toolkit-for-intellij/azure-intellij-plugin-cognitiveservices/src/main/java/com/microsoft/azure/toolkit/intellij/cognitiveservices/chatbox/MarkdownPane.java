/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.EditorTextField;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

@Getter
public class MarkdownPane {
    private JPanel contentPanel;
    private MarkdownText value;

    public MarkdownPane() {
    }

    public MarkdownPane(String markdown) {
        this.setValue(new MarkdownText(markdown));
    }

    public MarkdownPane(MarkdownText markdown) {
        this.setValue(markdown);
    }

    public void setValue(MarkdownText markdown) {
        this.value = markdown;
        this.contentPanel.removeAll();
        final List<MarkdownText.Part> parts = markdown.getParts();
        final GridLayoutManager newLayout = new GridLayoutManager(parts.size(), 1);
        this.contentPanel.setLayout(newLayout);
        final Container container = this.contentPanel.getParent();
        Optional.ofNullable(container).map(Component::getBackground).ifPresent(contentPanel::setBackground);
        for (int i = 0; i < parts.size(); i++) {
            final MarkdownText.Part part = parts.get(i);
            final GridConstraints constraints = new GridConstraints(i, 0, 1, 1,
                GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                null, null, null, 0);
            final String text = part.getText();
            if (part instanceof MarkdownText.CodePart codePart) {
                this.contentPanel.add(createEditor(codePart), constraints);
            } else {
                final String html = MarkdownUtil.INSTANCE.generateMarkdownHtml(new LightVirtualFile("dummy.md"), text, null);
                final JTextPane pane = new JTextPane();
                pane.setContentType("text/html");
                pane.setEditorKit(new UIUtil.JBWordWrapHtmlEditorKit());
                Messages.configureMessagePaneUi(pane, String.format("<html><body>%s</body></html", html));
                Optional.ofNullable(container).map(Component::getBackground).ifPresent(pane::setBackground);
                this.contentPanel.add(pane, constraints);
            }
        }
        this.contentPanel.revalidate();
        this.contentPanel.repaint();
    }

    private EditorTextField createEditor(MarkdownText.CodePart part) {
        final Project project = DataManager.getInstance().getDataContext(this.contentPanel).getData(CommonDataKeys.PROJECT);
        final DocumentImpl document = new DocumentImpl("", true);
        final FileType fileType = FileTypeManagerEx.getInstance().getFileTypeByExtension(part.getLanguage());
        final EditorTextField result = new EditorTextField(document, project, fileType, true, false);
        result.addSettingsProvider(editor -> { // add scrolling/line number features
            editor.setHorizontalScrollbarVisible(true);
            editor.setVerticalScrollbarVisible(true);
            editor.getSettings().setLineNumbersShown(true);
        });
        result.setText(part.getCode().trim());
        return result;
    }
}
