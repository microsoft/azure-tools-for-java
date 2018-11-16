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
import com.microsoft.aad.adal4j.AdalErrorCode;
import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;
import java.awt.Desktop;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
    private AuthenticationResult authenticationResult = null;
    private Future<?> authExecutor;
    private static final String TITLE = "Azure Device Login";

    public AuthenticationResult getAuthenticationResult() {
        return authenticationResult;
    }

    public DeviceLoginWindow(final AuthenticationContext ctx, final DeviceCode deviceCode,
                             final AuthenticationCallback<AuthenticationResult> callBack) {
        super(null, false, IdeModalityType.PROJECT);
        setModal(true);
        setTitle(TITLE);
        editorPanel.setText(createHtmlFormatMessage(deviceCode.getMessage()));
        editorPanel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException e1) {
                    // swallow exceptions
                }
            }
        });
        authExecutor = ApplicationManager.getApplication()
            .executeOnPooledThread(() -> pullAuthenticationResult(ctx, deviceCode, callBack));
        init();
    }

    private void pullAuthenticationResult(final AuthenticationContext ctx, final DeviceCode deviceCode,
                                          final AuthenticationCallback<AuthenticationResult> callback) {
        final long interval = deviceCode.getInterval();
        long remaining = deviceCode.getExpiresIn();
        while (remaining > 0 && authenticationResult == null) {
            try {
                remaining -= interval;
                Thread.sleep(interval * 1000);
                authenticationResult = ctx.acquireTokenByDeviceCode(deviceCode, callback).get();
            } catch (ExecutionException | InterruptedException e) {
                if (e.getCause() instanceof AuthenticationException &&
                    ((AuthenticationException) e.getCause()).getErrorCode() == AdalErrorCode.AUTHORIZATION_PENDING) {
                    // swallow the pending exception
                } else {
                    e.printStackTrace();
                    break;
                }
            }
        }
        closeDialog();
    }

    private String createHtmlFormatMessage(final String message) {
        final String pattern = "(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        final Matcher matcher = Pattern.compile(pattern).matcher(message);
        if (matcher.find()) {
            final String deviceLoginUrl = matcher.group(0);
            return "<p>"
                + message.replace(deviceLoginUrl, String.format("<a href=\"%s\">%s</a>", deviceLoginUrl,
                deviceLoginUrl))
                + "</p><p>Waiting for signing in with the code ...</p>";
        }
        return message;
    }

    @Override
    public void doCancelAction() {
        authExecutor.cancel(true);
        super.doCancelAction();
    }

    private void closeDialog() {
        ApplicationManager.getApplication().invokeLater(() -> {
            final Window w = getWindow();
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
