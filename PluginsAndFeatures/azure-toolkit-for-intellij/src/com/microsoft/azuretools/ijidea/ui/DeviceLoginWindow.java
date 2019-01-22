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
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

public class DeviceLoginWindow extends AzureDialogWrapper {
    private static final String TITLE = "Azure Device Login";
    private JPanel jPanel;
    private JEditorPane editorPanel;
    private AuthenticationResult authenticationResult = null;
    private Future<?> authExecutor;
    private final DeviceCode deviceCode;

    public AuthenticationResult getAuthenticationResult() {
        return authenticationResult;
    }

    public DeviceLoginWindow(final AuthenticationContext ctx, final DeviceCode deviceCode,
                             final AuthenticationCallback<AuthenticationResult> callBack) {
        super(null, false, IdeModalityType.PROJECT);
        super.setOKButtonText("Copy&&Open");
        this.deviceCode = deviceCode;
        setModal(true);
        setTitle(TITLE);
        editorPanel.setBackground(jPanel.getBackground());
        editorPanel.setText(createHtmlFormatMessage());
        editorPanel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
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
        // Close adal logger for it will write useless error log
        // for issue #2368 https://github.com/Microsoft/azure-tools-for-java/issues/2368
        Logger authLogger = Logger.getLogger(AuthenticationContext.class);
        Level authLoggerLevel = authLogger.getLevel();
        authLogger.setLevel(Level.OFF);
        try {
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
        } finally {
            authLogger.setLevel(authLoggerLevel);
        }
        closeDialog();
    }

    private String createHtmlFormatMessage() {
        final String verificationUrl = deviceCode.getVerificationUrl();
        return "<p>"
            + deviceCode.getMessage().replace(verificationUrl, String.format("<a href=\"%s\">%s</a>", verificationUrl,
            verificationUrl))
            + "</p><p>Waiting for signing in with the code ...</p>";
    }

    @Override
    public void doCancelAction() {
        authExecutor.cancel(true);
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        final StringSelection selection = new StringSelection(deviceCode.getUserCode());
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        try {
            Desktop.getDesktop().browse(new URI(deviceCode.getVerificationUrl()));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void closeDialog() {
        ApplicationManager.getApplication().invokeLater(() -> {
            final Window w = getWindow();
            w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
        }, ModalityState.stateForComponent(jPanel));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jPanel;
    }
}
