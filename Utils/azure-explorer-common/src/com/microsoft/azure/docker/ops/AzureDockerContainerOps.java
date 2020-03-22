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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.model.*;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.*;

import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.DEBUG;
import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.checkDockerContainerUrlAvailability;
import static com.microsoft.azure.docker.ops.utils.AzureDockerVMSetupScriptsForUbuntu.DEFAULT_DOCKER_IMAGES_DIRECTORY;

public class AzureDockerContainerOps {
  public static final String CMD_SUCCESS = "exit-status: 0";

  public static List<AzureDockerContainerInstance> list(AzureDockerContainerInstance dockerContainer) {
    try {
      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static List<AzureDockerContainerInstance> list(DockerImage dockerImage) {
    try {
      return null;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String getDetails(DockerContainer dockerContainer, Session session) {
    if (dockerContainer == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer and login session cannot be null");
    }

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String cmd1 = String.format("docker inspect %s \n", dockerContainer.name);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, false);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

      return cmdOut1;

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String getDetails(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String cmd1 = String.format("docker inspect %s \n", dockerContainer.dockerContainerName);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, false);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

      return cmdOut1;

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String getDetails(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);

    return getDetails(dockerContainerInstance, session);
  }

  public static String getDetails(AzureDockerContainerInstance dockerContainer) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || dockerContainer.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    return getDetails(dockerContainer, dockerContainer.dockerHost.session);
  }

  public static void start(DockerContainer dockerContainer, Session session) {
    if (dockerContainer == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer and login session cannot be null");
    }

    AzureDockerVMOps.waitForDockerDaemonStartup(session);

    try {
      if (!session.isConnected()) session.connect();

      String cmd1 = String.format("docker start %s \n", dockerContainer.name);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true, true);
      if (!cmdOut1.contains(CMD_SUCCESS)) {
        String title = "Docker Container Error";
        String msg = String.format("Docker container %s failed to start: %s", dockerContainer.name, cmdOut1);
        DefaultLoader.getUIHelper().showError(msg, title);
        throw new AzureDockerException(msg);
      }
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void start(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    AzureDockerVMOps.waitForDockerDaemonStartup(session);

    try {
      if (!session.isConnected()) session.connect();

      String cmd1 = String.format("docker start %s \n", dockerContainer.dockerContainerName);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true, true);
      if (!cmdOut1.contains(CMD_SUCCESS)) {
        String title = "Docker Container Error";
        String msg = String.format("Docker container %s failed to start: %s", dockerContainer.dockerContainerName, cmdOut1);
        DefaultLoader.getUIHelper().showError(msg, title);
        throw new AzureDockerException(msg);
      }
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void start(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);
    start(dockerContainerInstance, session);
  }

  public static void start(AzureDockerContainerInstance dockerContainer) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || dockerContainer.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    start(dockerContainer, dockerContainer.dockerHost.session);
  }

  public static void stop(DockerContainer dockerContainer, Session session) {
    if (dockerContainer == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer and login session cannot be null");
    }

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String cmd1 = String.format("docker stop %s \n", dockerContainer.name);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true, true);
      if (!cmdOut1.contains(CMD_SUCCESS)) {
        String title = "Docker Container Error";
        String msg = String.format("Docker Container %s Failed to Stop: %s", dockerContainer.name, cmdOut1);
        DefaultLoader.getUIHelper().showError(msg, title);
        throw new AzureDockerException(msg);
      }
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void stop(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String cmd1 = String.format("docker stop %s \n", dockerContainer.dockerContainerName);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true, true);
      if (!cmdOut1.contains(CMD_SUCCESS)) {
        String title = "Docker Container Error";
        String msg = String.format("Docker container %s failed to stop: %s", dockerContainer.dockerContainerName, cmdOut1);
        DefaultLoader.getUIHelper().showError(msg, title);
      }
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void stop(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);
    stop(dockerContainerInstance, session);
  }

  public static void stop(AzureDockerContainerInstance dockerContainer) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || dockerContainer.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    stop(dockerContainer, dockerContainer.dockerHost.session);
  }

  public static void delete(DockerContainer dockerContainer, Session session) {
    if (dockerContainer == null || session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer and login session cannot be null");
    }

    try {
      if (!session.isConnected()) session.connect();

      stop(dockerContainer, session);

      String cmd1 = String.format("docker rm %s \n", dockerContainer.name);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true, true);
      if (!cmdOut1.contains(CMD_SUCCESS)) {
        String title = "Docker Container Error";
        String msg = String.format("Docker container %s failed to delete: %s", dockerContainer.name, cmdOut1);
        DefaultLoader.getUIHelper().showError(msg, title);
        throw new AzureDockerException(msg);
      }
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void delete(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      stop(dockerContainer, session);

      String cmd1 = String.format("docker rm %s \n", dockerContainer.dockerContainerName);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true, true);
      if (!cmdOut1.contains(CMD_SUCCESS)) {
        String title = "Docker Container Error";
        String msg = String.format("Docker container %s failed to delete: %s", dockerContainer.dockerContainerName, cmdOut1);
        DefaultLoader.getUIHelper().showError(msg, title);
        throw new AzureDockerException(msg);
      }
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void delete(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);
    delete(dockerContainerInstance, session);
  }

  public static void delete(AzureDockerContainerInstance dockerContainer) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || dockerContainer.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    delete(dockerContainer, dockerContainer.dockerHost.session);
  }

  public static AzureDockerContainerInstance create(AzureDockerContainerInstance dockerContainer, Session session) {
    if (dockerContainer == null || dockerContainer.dockerHost == null || (session == null && dockerContainer.dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerContainer, Docker dockerHost and login session cannot be null");
    }

    if (session == null) session = dockerContainer.dockerHost.session;

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(session);

      String portSettings = "";
      for (String item : dockerContainer.dockerPortSettings.trim().split("[\\s]+")) {
        portSettings += " -p " + item;
      }

      String cmd1 = String.format("docker create %s --name %s %s \n", portSettings, dockerContainer.dockerContainerName, dockerContainer.dockerImageName);
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, session, true, true);
      if (!cmdOut1.contains(CMD_SUCCESS)) {
        String title = "Docker Container Error";
        String msg = String.format("Docker container %s failed to create: %s\nCheck if Docker host port are taken (port %s)", dockerContainer.dockerContainerName, cmdOut1, dockerContainer.dockerPortSettings);
        DefaultLoader.getUIHelper().showError(msg, title);
        throw new AzureDockerException(msg);
      }
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);

      return  dockerContainer;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static AzureDockerContainerInstance create(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    AzureDockerContainerInstance dockerContainerInstance = new AzureDockerContainerInstance(dockerImageInstance);
    dockerContainerInstance = create(dockerContainerInstance, session);

    return dockerContainerInstance;
  }

  public static AzureDockerContainerInstance create(AzureDockerContainerInstance dockerContainerInstance) {
    if (dockerContainerInstance == null || dockerContainerInstance.dockerHost == null || dockerContainerInstance.dockerHost.session == null) {
      throw new AzureDockerException("Unexpected param values; dockerImageInstance, Docker dockerHost and login session cannot be null");
    }

    return create(dockerContainerInstance, dockerContainerInstance.dockerHost.session);
  }

  public static Map<String, DockerContainer> getContainers(DockerHost dockerHost) {
    if (dockerHost == null || (dockerHost.session == null && dockerHost.certVault == null)) {
      throw new AzureDockerException("Unexpected param values: dockerHost and login session cannot be null");
    }

    Map<String, DockerContainer> dockerContainerMap = new HashMap<>();

    if (dockerHost.session == null) dockerHost.session = AzureDockerSSHOps.createLoginInstance(dockerHost);

    try {
      if (!dockerHost.session.isConnected()) dockerHost.session.connect();

      AzureDockerVMOps.waitForDockerDaemonStartup(dockerHost.session);

      ObjectMapper mapper = new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(SerializationFeature.INDENT_OUTPUT, true);

      // list all the Docker containers on the Docker host
      String cmd1 = "docker ps -a -s --no-trunc --format \"{\\\"name\\\" : \\\"{{.Names}}\\\", \\\"id\\\" : \\\"{{.ID}}\\\", \\\"status\\\" : \\\"{{.Status}}\\\", \\\"ports\\\" : \\\"{{.Ports}}\\\", \\\"image\\\" : \\\"{{.Image}}\\\", \\\"command\\\" : {{.Command}}, \\\"size\\\" : \\\"{{.Size}}\\\" }\"";
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, dockerHost.session, false);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);
      String jsonAllContainers = cmdOut1;

      // list only the running Docker containers on the Docker host
      cmd1 = "docker ps --format {{.Names}}";
      if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
      cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, dockerHost.session, false);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing: %s\n", cmd1);
      String runningContainers = cmdOut1;

      Scanner lines = new Scanner(jsonAllContainers);
      while (lines.hasNextLine()) {
        String currentLine = lines.nextLine();
        try {
          DockerContainer dockerContainer = mapper.readValue(currentLine, DockerContainer.class);
          dockerContainer.isRunning = false;
          // find the host port mappings
          String hostPort = "80";
          String containerPort = "8080";
          if (dockerContainer.ports == null || dockerContainer.ports.trim().isEmpty()) {
            // inspect the container and grab the host port
            try {
              cmd1 = String.format("docker inspect %s | grep -i HostPort", dockerContainer.name);
              if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
              cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, dockerHost.session, false);
              if (DEBUG) System.out.println(cmdOut1);
              if (DEBUG) System.out.format("Done executing: %s\n", cmd1);
              // returns "\"HostPort\": \"24320\""
              hostPort = cmdOut1.split("\"")[3];
              cmd1 = String.format("docker inspect %s | grep -i \"/tcp\"", dockerContainer.name);
              if (DEBUG) System.out.format("Start executing: %s\n", cmd1);
              cmdOut1 = AzureDockerSSHOps.executeCommand(cmd1, dockerHost.session, false);
              if (DEBUG) System.out.println(cmdOut1);
              if (DEBUG) System.out.format("Done executing: %s\n", cmd1);
              // returns "\"8080/tcp\": ["
              containerPort = cmdOut1.split("\"")[1];
            } catch (Exception eee) {}
          } else {
            // ports will be returned in the following format: "0.0.0.0:24320->8080/tcp"
            hostPort = dockerContainer.ports.split(":")[1].split("-")[0];
            containerPort = dockerContainer.ports.split(">")[1];
          }
          dockerContainer.ports = hostPort + ":" + containerPort;
          dockerContainer.url = String.format("http://%s:%s/",
              dockerHost.hostVM.dnsName,
              hostPort);
          dockerContainer.dockerHostApiUrl = dockerHost.apiUrl;
          dockerContainerMap.put(dockerContainer.name, dockerContainer);
        } catch (Exception ee){}
      }
      lines.close();

      lines = new Scanner(runningContainers);
      while (lines.hasNextLine()) {
        String currentLine = lines.nextLine();
        try {
          DockerContainer dockerContainer = dockerContainerMap.get(currentLine);
          if (dockerContainer != null) {
            dockerContainer.isRunning = true;
          }
        } catch (Exception ee){}
      }
      lines.close();

      return dockerContainerMap;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void setContainersAndImages(Map<String, DockerContainer> dockerContainerMap, Map<String, DockerImage> dockerImageMap) {
    for (DockerContainer dockerContainer : dockerContainerMap.values()) {
      try {
        DockerImage dockerImage = dockerImageMap.get(dockerContainer.image);
        if (dockerImage != null) {
          if (dockerImage.artifactFile != null && !dockerImage.artifactFile.isEmpty()) {
            // adjust the Url path to capture the artifact name
            String url = dockerImage.artifactFile.toLowerCase().matches(".*\\.war") ?
                dockerContainer.url + dockerImage.artifactFile.substring(0, dockerImage.artifactFile.lastIndexOf(".")) :
                dockerContainer.url;
            if (dockerContainer.isRunning && checkDockerContainerUrlAvailability(url)) {
              dockerContainer.url = url;
            }
          }

          dockerImage.containers.put(dockerContainer.name, dockerContainer);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

}
