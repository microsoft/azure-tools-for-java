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

package com.microsoft.azure.toolkit.lib.common.utils;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MavenDependencyUtils {
    public static List<String> getMavenCentralVersions(String groupId, String artifactId) throws IOException, DocumentException, AzureExecutionException {
        URLConnection conn = new URL(String.format("https://repo1.maven.org/maven2/%s/%s/maven-metadata.xml",
                                                   StringUtils.replace(groupId, ".", "/"), artifactId)).openConnection();
        List<String> res = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String xml = IOUtils.toString(reader);
            Document doc = DocumentHelper.parseText(xml);
            List<Node> nodes = doc.selectNodes("//metadata/versioning/versions/version");
            for (Node node : nodes) {
                String version = node.getText();
                if (StringUtils.isNotEmpty(version)) {
                    res.add(version);
                }
            }
        }
        if (res.isEmpty()) {
            throw new AzureExecutionException((String.format("Cannot get version from maven central for: %s:%s.", groupId, artifactId)));
        }
        return res;
    }
}
