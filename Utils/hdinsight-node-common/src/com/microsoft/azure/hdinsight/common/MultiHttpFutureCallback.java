/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.google.common.util.concurrent.FutureCallback;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.util.List;

public abstract class MultiHttpFutureCallback implements FutureCallback<List<String>> {
    private final HttpExchange httpExchange;

    public MultiHttpFutureCallback(@NotNull HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
    }

    @Override
    public void onFailure(Throwable t) {
        dealWithFailure(t,httpExchange);
    }

    private static void dealWithFailure(@NotNull Throwable throwable,@NotNull final HttpExchange httpExchange) {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        try {
            String str = throwable.getMessage();
            httpExchange.sendResponseHeaders(200, str.length());
            OutputStream stream = httpExchange.getResponseBody();
            stream.write(str.getBytes());
            stream.close();
        }catch (Exception e) {
            //LOGGER.error("Get job history error", e);
        }
    }
}
