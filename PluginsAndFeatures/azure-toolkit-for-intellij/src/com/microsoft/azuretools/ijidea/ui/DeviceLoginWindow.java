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
import com.intellij.openapi.vcs.impl.CancellableRunnable;
import com.microsoft.aad.adal4j.AdalErrorCode;
import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;

public class DeviceLoginWindow extends AzureDialogWrapper {
    private static final String TITLE = "Azure Device Login";
    private JPanel jPanel;
    private JEditorPane editorPanel;
    private AuthenticationResult authenticationResult = null;
    private final DeviceCode deviceCode;
    private final CancellableRunnable runnable;
    private final Future<?> future;

    public AuthenticationResult getAuthenticationResult() {
        return authenticationResult;
    }

    public DeviceLoginWindow(final AuthenticationContext ctx, final DeviceCode deviceCode,
                             final AuthenticationCallback<AuthenticationResult> callback) {
        super(null, false, IdeModalityType.PROJECT);
        super.setOKButtonText("Copy&&Open");
        this.deviceCode = deviceCode;
        setModal(true);
        setTitle(TITLE);
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

        runnable = new CancellableRunnable() {
            private AtomicBoolean cancel = new AtomicBoolean();
            final long interval = deviceCode.getInterval();
            long remaining = deviceCode.getExpiresIn();

            @Override
            public void cancel() {
                cancel.set(true);
            }

            @Override
            public void run() {
                while (!cancel.get() && remaining > 0) {
                    try {
                        remaining -= interval;
                        authenticationResult = ctx.acquireTokenByDeviceCode(deviceCode, callback).get();
                        break;
                    } catch (ExecutionException | InterruptedException e) {
                        if (e instanceof InterruptedException || e.getCause() instanceof AuthenticationException &&
                                ((AuthenticationException) e.getCause()).getErrorCode() == AdalErrorCode.AUTHORIZATION_PENDING) {
                        } else {
                            e.printStackTrace();
                            break;
                        }
                    }
                    try {
                        Thread.sleep(interval * 1000);
                    } catch (InterruptedException e1) {
                        // swallow the exception
                    }
                }
                closeDialog();
            }
        };
        final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r) {
                    @Override
                    public void interrupt() {
                        super.interrupt();
                        runnable.cancel();
                    }
                };
            }
        });
        future = executor.submit(runnable);
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
    public void doCancelAction() {
        future.cancel(true);
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
