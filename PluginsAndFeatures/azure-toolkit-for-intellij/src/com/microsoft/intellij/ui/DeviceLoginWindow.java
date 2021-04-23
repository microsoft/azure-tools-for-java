/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.azure.identity.DeviceCodeInfo;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowEvent;

public class DeviceLoginWindow extends AzureDialogWrapper {
    private static final String TITLE = "Azure Device Login";
    private JPanel panel;
    private JEditorPane editorPanel;
    private DeviceCodeInfo deviceCode;
    private IDeviceLoginUI deviceLoginUI;


    public DeviceLoginWindow(DeviceCodeInfo deviceCode, IDeviceLoginUI deviceLoginUI) {
        super(null, false, IdeModalityType.PROJECT);
        super.setOKButtonText("Copy&&Open");
        this.deviceCode = deviceCode;
        this.deviceLoginUI = deviceLoginUI;
        setModal(true);
        setTitle(TITLE);
        editorPanel.setBackground(panel.getBackground());
        editorPanel.setText(createHtmlFormatMessage());
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

    private String createHtmlFormatMessage() {
        final String verificationUrl = deviceCode.getVerificationUrl();
        return "<p>"
            + deviceCode.getMessage().replace(verificationUrl, String.format("<a href=\"%s\">%s</a>", verificationUrl,
            verificationUrl))
            + "</p><p>Waiting for signing in with the code ...</p>";
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
        deviceLoginUI.cancel();
        super.doCancelAction();
    }
    public void closeDialog() {
        ApplicationManager.getApplication().invokeLater(() -> {
            final Window w = getWindow();
            w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
        }, ModalityState.stateForComponent(panel));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }
}
