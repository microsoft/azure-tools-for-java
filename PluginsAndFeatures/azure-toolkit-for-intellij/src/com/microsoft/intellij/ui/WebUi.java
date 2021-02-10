/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

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
