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
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.adauth.IWebUi;

import java.net.URI;

/**
 * Created by vlashch on 10/17/16.
 */
class WebUi implements IWebUi {
    LoginWindow loginWindow;

    @Override
    //public Future<String> authenticateAsync(URI requestUri, URI redirectUri) {
    public String authenticate(URI requestUri, URI redirectUri) {
        System.out.println("==> requestUri: " + requestUri);
        final String requestUriStr = requestUri.toString();
        final String redirectUriStr = redirectUri.toString();

        if(ApplicationManager.getApplication().isDispatchThread()) {
            buildAndShow(requestUri.toString(), redirectUri.toString());
        } else {
            AzureTaskManager.getInstance().runAndWait(() -> buildAndShow(requestUriStr, redirectUriStr));
        }

//        final Callable<String> worker = new Callable<String>() {
//            @Override
//            public String call() {
//                return loginWindow.getResult();
//            }
//        };
//
//        // just to return future to comply interface
//        return Executors.newSingleThreadExecutor().submit(worker);
        return loginWindow.getResult();
    }

    private void buildAndShow(String requestUri, String redirectUri) {
        loginWindow = new LoginWindow(requestUri, redirectUri);
        loginWindow.show();
    }
}
