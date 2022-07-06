/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefClient;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBFont;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;

public class ProvideFeedbackEditor extends BaseEditor implements DumbAware {
    private static final String TARGET = "target";
    private static final String SURVEY = "survey";
    private static final String SUBMIT_SURVEY = "submit-survey";
    private static final String FEATURE_REQUEST = "feature request";
    private static final String ISSUE = "issue";
    private static final String QUALTRICS_SURVEY = "qualtrics survey";
    private static final String SURVEY_THANKS = "survey thanks";
    private final JBCefJSQuery myJSQueryOpenInBrowser;
    private final JBCefBrowser jbCefBrowser;
    private JPanel pnlRoot;
    private JLabel loadingLabel;
    private JPanel pnlLoading;
    private JPanel pnlBrowser;
    private Project project;

    public ProvideFeedbackEditor(final Project project, VirtualFile virtualFile) {
        super(virtualFile);
        this.project = project;
        this.loadingLabel.setIcon(IconUtil.scale(IntelliJAzureIcons.getIcon(AzureIcons.Common.REFRESH_ICON), loadingLabel, 1.5f));
        this.loadingLabel.setFont(JBFont.h2());
        this.jbCefBrowser = new JBCefBrowser("https://www.surveymonkey.com/r/PNB5NBL?mode=simple");
        final CefBrowser browser = jbCefBrowser.getCefBrowser();
        final JBCefClient client = jbCefBrowser.getJBCefClient();
        pnlBrowser.add(jbCefBrowser.getComponent(), new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 3, 3, null, null, null, 0));
        browser.createImmediately();
        // Create a JS query instance
        this.myJSQueryOpenInBrowser = JBCefJSQuery.create((JBCefBrowserBase) jbCefBrowser);
        myJSQueryOpenInBrowser.addHandler((e) -> {
            AzureTaskManager.getInstance().runLater(() -> closeEditor());
            return new JBCefJSQuery.Response("ok!");
        });
        client.addRequestHandler(openLinkWithLocalBrowser(), browser);
        client.addLoadHandler(modifySubmitButtonInSurveyCollectionPage(), browser);
        pnlBrowser.setVisible(false);
    }

    @AzureOperation(name = "common.complete_feedback", type = AzureOperation.Type.ACTION)
    private void closeEditor() {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.closeFile(virtualFile);
    }

    @Nonnull
    private CefLoadHandlerAdapter modifySubmitButtonInSurveyCollectionPage() {
        return new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                ProvideFeedbackEditor.this.pnlLoading.setVisible(false);
                ProvideFeedbackEditor.this.pnlBrowser.setVisible(true);
                if (!browser.getURL().contains("www.surveymonkey.com/r/PNB5NBL")) {
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
        };
    }

    @Nonnull
    private CefRequestHandlerAdapter openLinkWithLocalBrowser() {
        return new CefRequestHandlerAdapter() {
            @Override
            @AzureOperation(name = "common.feedback_browse", type = AzureOperation.Type.ACTION)
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                final String target = getBrowserTarget(request);
                OperationContext.action().setTelemetryProperty(TARGET, target);
                if (StringUtils.equalsAny(target, SURVEY, SUBMIT_SURVEY)) {
                    return false;
                }
                if (!StringUtils.equals(target, SURVEY_THANKS)) {
                    BrowserUtil.browse(request.getURL());
                }
                return true;
            }
        };
    }

    private String getBrowserTarget(CefRequest request) {
        final String url = request.getURL();
        if (StringUtils.containsIgnoreCase(url, "www.surveymonkey.com/r/PNB5NBL")) {
            return StringUtils.equalsIgnoreCase(request.getMethod(), "GET") ? SURVEY : SUBMIT_SURVEY;
        } else if (StringUtils.containsIgnoreCase(url, "github.com/Microsoft/azure-tools-for-java/issues/new")) {
            return StringUtils.containsIgnoreCase(url, "feature request") ? FEATURE_REQUEST : ISSUE;
        } else if (StringUtils.containsIgnoreCase(url, "microsoft.qualtrics.com/jfe/form/SV_b17fG5QQlMhs2up")) {
            return QUALTRICS_SURVEY;
        } else if (StringUtils.containsIgnoreCase(url, "survey-thanks")) {
            return SURVEY_THANKS;
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Override
    public JComponent getComponent() {
        return pnlRoot;
    }

    @Override
    @Nonnull
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getName() {
        return "Provide Feedback";
    }

    @Override
    public void dispose() {
        Disposer.dispose(myJSQueryOpenInBrowser);
        Disposer.dispose(jbCefBrowser);
    }
}
