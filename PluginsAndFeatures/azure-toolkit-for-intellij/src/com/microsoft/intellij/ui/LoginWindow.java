/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2020 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.jcef.JBCefBrowser;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class LoginWindow extends AzureDialogWrapper {
    public final String redirectUri;
    public final String requestUri;
    private String res = null;

    private final JBCefBrowser myJBCefBrowser;

    private void setResult(String res) {
        this.res = res;
    }

    public String getResult() {
        return res;
    }

    public LoginWindow(String requestUri, String redirectUri) {
        super(null, false, IdeModalityType.IDE);
        this.redirectUri = redirectUri;
        this.requestUri = requestUri;
        setModal(true);
        setTitle("Azure Login Dialog");

        myJBCefBrowser = new JBCefBrowser(requestUri);

        myJBCefBrowser.getJBCefClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                if (url.startsWith(redirectUri)) {
                    setResult(url);
                    closeDlg();
                }
            }
        }, myJBCefBrowser.getCefBrowser());

        JComponent browser = myJBCefBrowser.getComponent();
        browser.setPreferredSize(new Dimension(500, 750));
        browser.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                myJBCefBrowser.getCefBrowser().loadURL(requestUri);
            }
        });

        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return myJBCefBrowser.getComponent();
    }

    private void closeDlg() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean closeResult = myJBCefBrowser.getCefBrowser().doClose();
                if (!closeResult) {
                    throw new IllegalStateException("Unable to properly close the browser");
                }
            }
        }, ModalityState.stateForComponent(myJBCefBrowser.getComponent()));
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected JComponent createSouthPanel() {
        return null;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "LoginWindow";
    }
}
