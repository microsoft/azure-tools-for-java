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

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;
import java.awt.Desktop;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

public class DeviceLoginWindow extends AzureDialogWrapper {
    private JPanel jPanel;
    private JEditorPane editorPanel;
    private boolean isCancelled;

    public boolean getIsCancelled() {
        return this.isCancelled;
    }

    public DeviceLoginWindow(String message, final String title) {
        super(null, true, IdeModalityType.PROJECT);
        setModal(true);
        setTitle(title);
        editorPanel.setText(createHtmlFormatMessage(message));
        editorPanel.addHyperlinkListener(e -> {
            if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException e1) {
                    // todo
                } catch (URISyntaxException e1) {
                    // todo
                }
            }
        });
        init();
    }

    private String createHtmlFormatMessage(final String message) {
        final Matcher matcher = Pattern.compile("http\\S+").matcher(message);
        if (matcher.find()) {
            final String deviceLoginUrl = matcher.group(0);
            return "<p>"
                + message.replace(deviceLoginUrl, String.format("<a href=\"%s\">%s</a>", deviceLoginUrl, deviceLoginUrl))
                + "</p>";
        }
        return message;
    }

    @Override
    public void doCancelAction() {
        close();
        isCancelled = true;
        super.doCancelAction();
    }

    public void close() {
        ApplicationManager.getApplication().invokeLater(() -> {
            Window w = getWindow();
            w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
        }, ModalityState.stateForComponent(jPanel));
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{this.getCancelAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jPanel;
    }
}
