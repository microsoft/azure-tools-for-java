/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class ReadStreamLineThread extends Thread {

    private InputStream inputStream;
    private Consumer<String> stringConsumer;
    private Consumer<IOException> errorHandler;

    public ReadStreamLineThread(InputStream inputStream, Consumer<String> lineConsumer) {
        this(inputStream, lineConsumer, null);
    }

    public ReadStreamLineThread(InputStream inputStream, Consumer<String> stringConsumer, Consumer<IOException> errorHandler) {
        this.inputStream = inputStream;
        this.stringConsumer = stringConsumer;
        this.errorHandler = errorHandler;
    }

    @Override
    public void run() {
        try (InputStreamReader isr = new InputStreamReader(inputStream);
             BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                stringConsumer.accept(line);
            }
        } catch (IOException e) {
            if (errorHandler != null) {
                errorHandler.accept(e);
            }
        }
    }
}
