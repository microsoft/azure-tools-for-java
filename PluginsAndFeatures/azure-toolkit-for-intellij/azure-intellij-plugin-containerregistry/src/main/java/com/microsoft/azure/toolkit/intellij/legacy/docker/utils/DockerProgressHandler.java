/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.utils;

import com.microsoft.intellij.RunProcessHandler;
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
