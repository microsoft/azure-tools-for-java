/*
 * Copyright (c) Microsoft Corporation
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
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * Created by vlashch on 10/17/16.
 */
class LoginWindow extends AzureDialogWrapper {
    public final String redirectUri;
    public final String requestUri;
    private String res = null;

    private final JFXPanel fxPanel;

    private void setResult(String res) {
        this.res = res;
    }

    public String getResult() {
        return res;
    }

    public LoginWindow(String requestUri, String redirectUri) {
        super(null, false, IdeModalityType.IDE);

        this.redirectUri =  redirectUri;
        this.requestUri =  requestUri;

        fxPanel = new JFXPanel();
        setModal(true);
        setTitle("Azure Login Dialog");
        fxPanel.setPreferredSize(new Dimension(500, 750));
        Platform.setImplicitExit(false);
        Runnable fxWorker = new Runnable() {
            @Override
            public void run() {
                //Group root = new Group();
                final WebView browser = new WebView();
                final WebEngine webEngine = browser.getEngine();
                webEngine.locationProperty().addListener(new ChangeListener<String>(){
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {

                        if(newValue.startsWith(redirectUri)) {
                            setResult(newValue);
                            closeDlg();
                        }
                    }
                });

                Scene scene = new Scene(browser);
                fxPanel.setScene(scene);
                webEngine.load(requestUri);
            }
        };

        Platform.runLater(fxWorker);

        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return fxPanel;
    }

    private void closeDlg() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Window w = getWindow();
                w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
            }
        }, ModalityState.stateForComponent(fxPanel));
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
