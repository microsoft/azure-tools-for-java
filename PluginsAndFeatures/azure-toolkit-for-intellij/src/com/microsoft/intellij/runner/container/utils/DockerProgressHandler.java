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

package com.microsoft.intellij.runner.container.utils;

import com.microsoft.intellij.runner.RunProcessHandler;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ProgressMessage;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DockerProgressHandler implements ProgressHandler {
    private final Map<String, String> layerMap = new ConcurrentHashMap<>();
    private final RunProcessHandler processHandler;

    public DockerProgressHandler(RunProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    @Override
    public void progress(ProgressMessage message) throws DockerException {
        if (message == null) {
            return;
        }
        if (message.error() != null) {
            throw new DockerException(message.error());
        }
        String id = message.id();
        if (id != null) {
            if (layerMap.containsKey(id) && layerMap.get(id).equals(message.toString())) {
                return; // ignore duplicate message
            }
            layerMap.put(id, message.toString());
        } else {
            layerMap.clear();
        }
        String out = Stream.of(id, message.status(), message.stream(), message.progress())
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining("\t"))
                .trim();
        processHandler.setText(out);
    }
}
