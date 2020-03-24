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
