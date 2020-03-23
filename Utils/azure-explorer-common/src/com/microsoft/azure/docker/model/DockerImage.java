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

package com.microsoft.azure.docker.model;

import java.util.List;
import java.util.Map;

public class DockerImage {
  public String name;
  public String id;
  public String size;            // size of the image
  public String artifactFile;    // .war or .jar output file representing the application to be deployed and run
  public String repository;
  public String tag;
  public boolean isPluginImage;  // true if the image was generated via the Azure Plugin
  public String artifactPath;    // .war or .jar output file path representing the application to be deployed and run
  public String ports;           // container᾿s port or a range of ports to the dockerHost to be published (i.e. "1234-1236:1234-1236/tcp")
  public String dockerfile;      // Dockerfile input from which the image will be created
  public String remotePath;      // Docker dockerHost path to Dockerfile and artifact
  public String imageBase;       // see FROM directive
  public String exposeCMD;       // see EXPOSE directive
  public List<String> addCMDs;   // see ADD directive
  public List<String> runCMDs;   // see RUN directive
  public List<String> copyCMDs;  // see COPY directive
  public List<String> envCMDs;   // see ENV directive
  public List<String> workCMDs;  // see WORK directive

  public Map<String, DockerContainer> containers; // map of all Docker containers for this image

  public String dockerHostApiUrl; // parent Docker dockerHost

  public DockerImage() {}

  public DockerImage(AzureDockerImageInstance dockerImageInstance) {
    this.name = dockerImageInstance.dockerImageName;
    this.artifactPath = dockerImageInstance.artifactPath;
    this.dockerfile = dockerImageInstance.dockerfileContent;
    this.ports = dockerImageInstance.dockerPortSettings;
    this.dockerHostApiUrl = dockerImageInstance.host.apiUrl;
  }

  public DockerImage(String name, String customContent, String ports, String artifactPath) {
    this.name = name;
    this.dockerfile = customContent;
    this.ports = ports;
    this.artifactPath = artifactPath;
  }
}
