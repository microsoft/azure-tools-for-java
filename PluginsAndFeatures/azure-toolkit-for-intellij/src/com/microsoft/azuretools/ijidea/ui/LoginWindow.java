/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2020 JetBrains s.r.o.
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.ijidea.ui;

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
