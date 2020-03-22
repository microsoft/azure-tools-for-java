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
package com.microsoft.azure.docker.ops;

import com.fasterxml.jackson.databind.*;
import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.model.*;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.*;

import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.DEBUG;
import static com.microsoft.azure.docker.ops.utils.AzureDockerVMSetupScriptsForUbuntu.DEFAULT_DOCKER_IMAGES_DIRECTORY;

public class AzureDockerImageOps {
  public static final String CMD_SUCCESS = "exit-status: 0";

  public static void delete(DockerImage dockerImage, Session session) {
    if (dockerImage == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerImage and login session cannot be null");
    }

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      // delete any containers first
      if (dockerImage.containers != null) {
        for (DockerContainer dockerContainer : dockerImage.containers.values()) {
          AzureDockerContainerOps.delete(dockerContainer, session);
        }
      }

      if (DEBUG) System.out.format("Start executing docker rmi %s\n", dockerImage.name);
      String cmdOut1 = AzureDockerSSHOps.executeCommand("docker rmi " + dockerImage.name, session, true, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (!cmdOut1.contains(CMD_SUCCESS)) {
        String title = "Docker Image Error";
        String msg = String.format("Docker image %s failed to delete: %s", dockerImage.name, cmdOut1);
        DefaultLoader.getUIHelper().showError(msg, title);
      }
      if (DEBUG) System.out.format("Done executing docker rmi %s\n", dockerImage.name);

      if (DEBUG) System.out.format("Start executing rm -f -r %s/%s\n", DEFAULT_DOCKER_IMAGES_DIRECTORY, dockerImage.name);
      cmdOut1 = AzureDockerSSHOps.executeCommand(String.format("rm -f -r %s/%s ", DEFAULT_DOCKER_IMAGES_DIRECTORY, dockerImage.name), session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing rm -f -r %s/%s\n", DEFAULT_DOCKER_IMAGES_DIRECTORY, dockerImage.name);
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerImage create(DockerImage dockerImage, Session session) {
    if (dockerImage == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerImage and login session cannot be null");
    }

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String dockerImageDir = DEFAULT_DOCKER_IMAGES_DIRECTORY + "/" + dockerImage.name;
      String cmd1 = String.format("docker build -t %s -f %s/Dockerfile %s/ \n", dockerImage.name, dockerImageDir, dockerImageDir);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true, true);
      if (!cmdOut1.contains(CMD_SUCCESS)) {
        String title = "Docker Image Error";
        String msg = String.format("Docker image %s failed to create: %s", dockerImage.name, cmdOut1);
        DefaultLoader.getUIHelper().showError(msg, title);
        return null;
      }
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

      // docker images ubuntu --format "{ \"id\": \"{{.ID}}\", \"name\": \"{{.Repository}} }\""
      // Getting the image ID
      String cmd2 = String.format("docker images %s --format {{.ID}} \n", dockerImage.name);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd2);
      String cmdOut2 = AzureDockerSSHOps.executeCommand(cmd2, session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd2);
      dockerImage.id = cmdOut2.trim();

      dockerImage.remotePath = dockerImageDir;
      dockerImage.isPluginImage = true;

      return dockerImage;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static AzureDockerImageInstance create(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    DockerImage dockerImage = AzureDockerImageOps.create(new DockerImage(dockerImageInstance), session);
    // add the image to the Docker dockerHost list of Docker images
    if (dockerImageInstance.host.dockerImages == null) {
      dockerImageInstance.host.dockerImages = new HashMap<>();
    }
    if (dockerImage != null && dockerImage.name != null) {
      dockerImageInstance.host.dockerImages.put(dockerImage.name, dockerImage);

      dockerImageInstance.id = dockerImage.id;
      dockerImageInstance.remotePath = dockerImage.remotePath;
    }

    return dockerImageInstance;
  }

  public static DockerImage get(DockerImage dockerImage, Session session) {
    if (dockerImage == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerImage and login session cannot be null");
    }

    try {
      if (!session.isConnected()) session.connect();

      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String getDetails(DockerImage dockerImage, Session session) {
    if (dockerImage == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerImage and login session cannot be null");
    }

    try {
      if (!session.isConnected()) session.connect();

      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static Map<String, DockerImage> getImages(DockerHost dockerHost) {
    if (dockerHost == null || (dockerHost.session == null && dockerHost.certVault == null)) {
      throw new AzureDockerException("Unexpected param values: dockerHost and login session cannot be null");
    }

    if (dockerHost.session == null) dockerHost.session = AzureDockerSSHOps.createLoginInstance(dockerHost);

    try {
      if (!dockerHost.session.isConnected()) dockerHost.session.connect();

      Map<String, DockerImage> dockerImageMap = new HashMap<>();

      AzureDockerVMOps.waitForDockerDaemonStartup(dockerHost.session);

      ObjectMapper mapper = new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(SerializationFeature.INDENT_OUTPUT, true);

      // list all the Docker images on the Docker host
      String cmd1 = String.format("docker images -a --format \"{ \\\"name\\\" : \\\"{{.Repository}}\\\", \\\"tag\\\" : \\\"{{.Tag}}\\\", \\\"id\\\" : \\\"{{.ID}}\\\", \\\"size\\\" : \\\"{{.Size}}\\\" }\"");
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, dockerHost.session, false);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);
      String jsonAllImages = cmdOut1;

      // list all the Azure Plugin images
      cmd1 = String.format("cd %s/ && ls -1 -d */ && cd ~", DEFAULT_DOCKER_IMAGES_DIRECTORY);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, dockerHost.session, false);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);
      String pluginImages = cmdOut1;

      Scanner lines = new Scanner(jsonAllImages);
      while (lines.hasNextLine()) {
        String currentLine = lines.nextLine();
        try {
          DockerImageRawInfo rawImage = mapper.readValue(currentLine, DockerImageRawInfo.class);
          if (rawImage.name != null && !rawImage.name.equals("<none>")) {
            DockerImage dockerImage = new DockerImage();
            dockerImage.name = rawImage.name;
            dockerImage.tag = (rawImage.tag != null && rawImage.tag.toLowerCase().equals("latest")) ? "" : rawImage.tag;
            dockerImage.id = rawImage.id;
            dockerImage.size = rawImage.size;
            dockerImage.dockerHostApiUrl = dockerHost.apiUrl;
            dockerImage.containers = new HashMap<>();

            dockerImageMap.put(dockerImage.name + (dockerImage.tag.isEmpty() ? "" : ":" + dockerImage.tag), dockerImage);
          }
        } catch (Exception ignored){}
      }
      lines.close();

      lines = new Scanner(pluginImages);
      while (lines.hasNextLine()) {
        String currentLine = lines.nextLine();
        try {
          DockerImage dockerImage = dockerImageMap.get(currentLine.split("/")[0]);
          if (dockerImage != null) {
            // found an image that was created by the Azure Plugin
            dockerImage.remotePath = DEFAULT_DOCKER_IMAGES_DIRECTORY + "/" + dockerImage.name;
            dockerImage.isPluginImage = true;
            if (DEBUG) System.out.println("Start downloading the Dockerfile content");
            dockerImage.dockerfile = AzureDockerSSHOps.download(dockerHost.session, "Dockerfile", dockerImage.remotePath.substring(2), true);
            if (DEBUG) System.out.println("Done downloading the Dockerfile content");
            // list the .WAR or .JAR artifact
            cmd1 = String.format("cd %s/ && ls -1 *.?ar && cd ~", dockerImage.remotePath);
            if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
            cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, dockerHost.session, false);
            if (DEBUG) System.out.println(cmdOut1);
            if (DEBUG) System.out.format("Done executing: %s\n", cmd1);
            dockerImage.artifactFile = cmdOut1.trim();
          }
        } catch (Exception ignored){}
      }
      lines.close();

      return dockerImageMap;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String getDockerImageMapKey(DockerImage dockerImage) {
    if (dockerImage != null && dockerImage.name != null) {
      return dockerImage.name + (dockerImage.tag == null || dockerImage.tag.isEmpty() ? "" : ":" + dockerImage.tag);
    } else {
      return "";
    }
  }

  public static class DockerImageRawInfo {
    public String name;
    public String tag;
    public String id;
    public String size;
  }



}
