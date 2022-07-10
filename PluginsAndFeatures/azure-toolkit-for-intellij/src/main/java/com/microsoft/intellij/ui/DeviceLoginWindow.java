/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.azure.identity.DeviceCodeInfo;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

public class DeviceLoginWindow extends AzureDialogWrapper {
    private static final String TITLE = "Azure Device Login";
    private JPanel panel;
    private JEditorPane editorPanel;
    private DeviceCodeInfo deviceCode;
    private Runnable onCancel;

    public DeviceLoginWindow(Project project) {
        super(project, false, IdeModalityType.PROJECT);
        super.setOKButtonText("Copy&&Open");
        setModal(true);
        setTitle(TITLE);
        editorPanel.setBackground(panel.getBackground());
        editorPanel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.open(e.getURL().toString());
            }
        });
        editorPanel.setFocusable(false);
        // Apply JLabel's font and color to JEditorPane
        final Font font = UIManager.getFont("Label.font");
        final Color foregroundColor = UIManager.getColor("Label.foreground");
        editorPanel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        if (font != null && foregroundColor != null) {
            editorPanel.setFont(font);
            editorPanel.setForeground(foregroundColor);
        }

        init();
    }

    public void show(@Nonnull final DeviceCodeInfo deviceCode) {
        this.deviceCode = deviceCode;
        final String url = deviceCode.getVerificationUrl();
        final String message = "<p>"
            + deviceCode.getMessage().replace(url, String.format("<a href=\"%s\">%s</a>", url, url))
            + "</p><p>Waiting for signing in with the code ...</p>";
        editorPanel.setText(message);
        this.show();
    }

    @Override
    protected void doOKAction() {
        final StringSelection selection = new StringSelection(deviceCode.getUserCode());
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        BrowserUtil.open(deviceCode.getVerificationUrl());
    }

    @Override
    public void doCancelAction() {
        Optional.ofNullable(onCancel).ifPresent(Runnable::run);
        super.doCancelAction();
    }

    public void setDoOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void close() {
        this.close(0, true);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }
}
