/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.BrowserHyperlinkListener;
import com.microsoft.intellij.helpers.base.BaseEditor;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.nimbusds.jose.util.IOUtils;
import org.intellij.plugins.markdown.ui.preview.MarkdownUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.IOException;

public class WhatsNewView extends BaseEditor {
    public static final String FAILED_TO_LOAD_WHATS_NEW_DOCUMENT = "Failed to load whats new document";
    private JPanel contentPanel;
    private JEditorPane pane;

    public WhatsNewView(Project project, VirtualFile virtualFile) {
        pane.setBackground(contentPanel.getBackground());
        pane.setEditorKit(new HTMLEditorKit());
        pane.setMargin(new Insets(0, 50, 0, 50));
        pane.addHyperlinkListener(new BrowserHyperlinkListener());
        try {
            final String content = IOUtils.readInputStreamToString(virtualFile.getInputStream());
            final String markDownHTML = MarkdownUtil.INSTANCE.generateMarkdownHtml(virtualFile, content, project);
            pane.setText(markDownHTML);
            final Color foreground = EditorColorsManager.getInstance().getGlobalScheme().getDefaultForeground();
            final StyleSheet styleSheet = ((HTMLDocument) this.pane.getDocument()).getStyleSheet();
            styleSheet.addRule("h1 { margin: 12px; font-size: 16px; }");
            styleSheet.addRule("h2 { margin: 10px; font-size: 14px; }");
            styleSheet.addRule("code { font-size: 12px;}");
            styleSheet.addRule(String.format("body { color: %s; font-size: 12px; font-family: arial; }",
                                             UIUtils.convertRGB2Hex(foreground)));
        } catch (IOException e) {
            final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            fileEditorManager.closeFile(virtualFile);
            ApplicationManager.getApplication().invokeLater(() -> PluginUtil.displayErrorDialog(
                    FAILED_TO_LOAD_WHATS_NEW_DOCUMENT, e.getMessage()));
        }
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return contentPanel;
    }

    @NotNull
    @Override
    public String getName() {
        return "Azure Toolkit for Java";
    }

    @Override
    public void dispose() {

    }
}
