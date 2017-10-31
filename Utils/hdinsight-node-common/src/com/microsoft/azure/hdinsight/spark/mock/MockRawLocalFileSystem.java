/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 */

package com.microsoft.azure.hdinsight.spark.mock;


import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class MockRawLocalFileSystem extends RawLocalFileSystem {
    @Override
    public URI getUri() {
        try {
            return new URI("mockfs:///");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public File pathToFile(Path path) {
        checkPath(path);

        @NotNull
        Path fakedPath;

        if (path.isAbsolute()) {
            URI originUri = path.toUri();

            fakedPath = Optional.ofNullable(originUri)
                    .map(URI::getScheme)
                    .filter(scheme -> scheme.equals("mockfs"))
                    .flatMap(scheme -> {
                        try {
                            Path newPath = new Path(new URI("file",
                                    originUri.getUserInfo(),
                                    originUri.getHost(),
                                    originUri.getPort(),
                                    originUri.getPath(),
                                    originUri.getQuery(),
                                    originUri.getFragment()));

                            return Optional.of(newPath);
                        } catch (URISyntaxException ignored) {
                            return Optional.empty();
                        }
                    })
                    .orElseGet(() -> {
                        List<String> components = Arrays.stream(originUri.getPath().split(Path.SEPARATOR))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .skip((Path.WINDOWS && Path.isWindowsAbsolutePath(originUri.getPath(), true)) ? 1 : 0)
                                .collect(Collectors.toList());

                        components.add(".");

                        return new Path(
                                new Path(System.getProperty("user.dir"), "../../"),
                                String.join(Path.SEPARATOR, components));

                    });
        } else {
            fakedPath = new Path(getWorkingDirectory(), path);
        }

        return new File(fakedPath.toUri().getPath());
    }
}