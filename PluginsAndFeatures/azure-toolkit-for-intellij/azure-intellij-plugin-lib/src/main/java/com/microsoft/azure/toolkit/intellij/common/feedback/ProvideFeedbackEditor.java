/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;

public class ProvideFeedbackEditor extends BaseEditor implements DumbAware {
    private final JBCefJSQuery myJSQueryOpenInBrowser;
    private JPanel pnlRoot;

    public ProvideFeedbackEditor(final Project project, VirtualFile virtualFile) {
        super(virtualFile);
        final JBCefBrowser jbCefBrowser = new JBCefBrowser("https://www.surveymonkey.com/r/PNB5NBL?mode=simple");
        final CefBrowser browser = jbCefBrowser.getCefBrowser();
        pnlRoot.add(jbCefBrowser.getComponent(), new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 3, 3, null, null, null, 0));
        // Create a JS query instance
        this.myJSQueryOpenInBrowser = JBCefJSQuery.create((JBCefBrowserBase) jbCefBrowser);
        myJSQueryOpenInBrowser.addHandler((e) -> {
            AzureTaskManager.getInstance().runLater(() -> {
                final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                fileEditorManager.closeFile(virtualFile);
            });
            return new JBCefJSQuery.Response("ok!");
        });

        jbCefBrowser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                if (!browser.getURL().contains("https://www.surveymonkey.com/r/PNB5NBL")) {
                    return;
                }
                final String js = "window.JavaPanelBridge = {\n" +
                    "    closeTab: function (e) {\n" +
                    "       " + myJSQueryOpenInBrowser.inject("e") +
                    "    }\n" +
                    "};\n" +
                    "var azToolkitCloseBtn = document.querySelector('.thank-you-page-container.survey-submit-actions a.survey-page-button.btn');\n" +
                    "if (azToolkitCloseBtn) {\n" +
                    "    azToolkitCloseBtn.innerHTML='Close';" +
                    "    azToolkitCloseBtn.addEventListener('click', () => {\n" +
                    "        window.JavaPanelBridge.closeTab();\n" +
                    "    });\n" +
                    "}";
                browser.executeJavaScript(js, browser.getURL(), 0);
            }
        }, browser);
    }

    @Override
    public JComponent getComponent() {
        return pnlRoot;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @Nonnull String getName() {
        return "Provide Feedback";
    }

    @Override
    public void dispose() {
        Disposer.dispose(myJSQueryOpenInBrowser);
    }
}
