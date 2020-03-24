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

package com.microsoft.azure.docker.ops.utils;

import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.AzureDockerSubscription;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AzureDockerValidationUtils {

  public static boolean validateDockerImageName(String name) {
    return (name != null && name.length() > 0 && name.length() < 64 && name.matches("^[a-z0-9][a-z0-9_-]*[a-z0-9_-]$"));
  }

  public static String getDockerImageNameTip() {
    return "Docker image name should be less than 64 characters and it can only include lower case letters, numbers, underscores and hyphens.";
  }

  public static boolean validateDockerContainerName(String name) {
    return (name != null && name.length() > 0 && name.length() < 64 && name.matches("^[a-z0-9][a-z0-9_-]*[a-z0-9_-]$"));
  }

  public static String getDockerContainerNameTip() {
    return "Docker container name should be less than 64 characters and it can only include lower case letters, numbers, underscores and dashes.";
  }

  public static boolean validateDockerArtifactPath(String name) {
    return name != null && name.length() > 0 &&
        Files.exists(Paths.get(name)) &&
        Files.isRegularFile(Paths.get(name)) &&
        Files.isReadable(Paths.get(name)) &&
        (new File(name).getName()).toLowerCase().matches("[a-z_0-9-.]+[jw]ar");
  }

  public static String getDockerArtifactPathTip() {
    return "Full path to the artifact to be deployed; alphanumeric, periods and hyphens characters only, white spaces and special characters are not supported";
  }

  public static Map<String, String> validateDockerPortSettings(String portSettings) {
    if (portSettings == null || portSettings.length() < 4 || portSettings.length() > 255) {
      return null;
    }
    Map<String, String> portMapping = new HashMap<>();

    for (String item : portSettings.trim().toLowerCase().split("[\\s]+")) {
      Pattern DOCKER_PORT_SETTINGS = Pattern.compile("^(\\d+):(\\d+)[/]?(tcp|udp)?"); // i.e. 18080:8080/tcp
      Matcher matcher = DOCKER_PORT_SETTINGS.matcher(item);
      if (!matcher.matches()) {
        return null;
      }
      int hostPort = Integer.parseInt(matcher.group(1));
      System.out.format("\t%d - %d\n", 1, hostPort);
      if (hostPort < 1 || hostPort > 65535) {
        return null;
      }
      int containerPort = Integer.parseInt(matcher.group(2));
      if (containerPort < 1 || containerPort > 65535) {
        return null;
      }
      portMapping.put(matcher.group(1), matcher.group(2));
    }
    return portMapping;
  }

  public static String getDockerPortSettingsTip() {
    return "Port settings for the container to be deployed (i.e. 18080:80/tcp)";
  }

  public static boolean validateDockerfilePath(String name) {
    return Files.exists(Paths.get(name)) &&
        Files.isRegularFile(Paths.get(name)) &&
        Files.isReadable(Paths.get(name));
  }

  public static String getDockerfilePathTip() {
    return "Full path to the Dockerfile to be used during the Docker image creation";
  }


  public static boolean validateDockerHostName(String name) {
    return (name != null && name.length() > 0 && name.length() < 26 && name.matches("^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$"));
  }

  public static String getDockerHostNameTip() {
    return "Virtual machine name should be less than 64 characters and it can only include alphanumeric characters and dashes.";
  }

  public static boolean validateDockerHostResourceGroupName(String name) {
    return (name != null && name.length() > 0 && name.length() <= 90 && name.matches("^[A-Za-z0-9][A-Za-z0-9-_().]*[A-Za-z0-9]$"));
  }

  public static String getDockerHostResourceGroupNameTip() {
    return "Resource group name should be less than 90 characters and it can only include alphanumeric characters, periods, underscores, hyphens and paranthesis and can not end in a period.";
  }

  public static boolean validateDockerHostUserName(String name) {
    return (name != null && name.length() > 0 && name.length() <= 64 && name.matches("^[A-Za-z0-9][A-Za-z0-9-_.]*[A-Za-z0-9-_]$"));
  }

  public static String getDockerHostUserNameTip() {
    return "User name should be 1 to 64 characters and it can only include alphanumeric characters, periods, underscores and can not end in a period.";
  }

  public static boolean validateDockerHostPassword(String pwd) {
    if (pwd == null || pwd.length() < 12 || pwd.length() > 72) {
      return false;
    }

    int specialChars = 0;
    if (pwd.matches(".*[A-Z].*")) {
      specialChars ++;
    }
    if (pwd.matches(".*[a-z].*")) {
      specialChars ++;
    }
    if (pwd.matches(".*[0-9].*")) {
      specialChars ++;
    }
    if (pwd.matches(".*[^A-Za-z0-9].*")) {
      specialChars ++;
    }

    return specialChars >= 3;
  }

  public static String getDockerHostPasswordTip() {
    return "Password should be 12 to 72 characters and must have 3 of the following: 1 lower case character, 1 upper case character, 1 number and 1 special character";
  }

  public static boolean validateDockerHostStorageName(String name) {
    return (name != null && name.length() >= 3 && name.length() <= 24 && name.matches("^[a-z][a-z0-9]*[a-z0-9]$"));
  }

  public static boolean validateDockerHostStorageName(String name, AzureDockerSubscription subscription) {
    return (name != null && name.length() >= 3 && name.length() <= 24 && name.matches("^[a-z][a-z0-9]*[a-z0-9]$") &&
        (subscription == null || subscription.azureClient == null || AzureDockerUtils.checkStorageNameAvailability(subscription.azureClient, name)));
  }

  public static String getDockerHostStorageNameTip() {
    return "Storage account name should be 3 to 24 lowercase alphanumeric characters and it should be unique across all Azure domain (DNS like)";
  }

  public static boolean validateDockerVnetName(String name) {
    return (name != null && name.length() > 0 && name.length() < 26 && name.matches("^[A-Za-z0-9][A-Za-z0-9-_.]*[A-Za-z0-9_]$"));
  }

  public static String getDockerVnetNameTip() {
    return "Virtual network name must be between 2 and 26 chars, begin with a letter or number, end with a letter, number or underscore and may contain only letters, numbers, underscores, periods or hyphens.";
  }

  public static boolean validateDockerVnetAddrSpace(String name) {
    if (name == null || name.length() < 2 || name.length() > 64) {
      return false;
    }

    Pattern CDIR_PREFIX_REGEX = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)/(\\d+)$");
    Matcher matcher = CDIR_PREFIX_REGEX.matcher(name);
    if (!matcher.matches() || Integer.parseInt(matcher.group(5)) > 31 || Integer.parseInt(matcher.group(5)) < 1) {
      return false;
    }
    for (int i = 1; i < 5; i++) {
      if (Integer.parseInt(matcher.group(i)) > 255) {
        return false;
      }
    }
    return true;
  }

  public static String getDockerVnetAddrspaceTip() {
    return "Virtual network address space name should be in CDIR notation.";
  }

  public static boolean validateDockerHostSshDirectory(String directoryPath) {
    return (directoryPath != null && directoryPath.length() >= 0 &&
        Files.isDirectory(Paths.get(directoryPath)) &&
        Files.isRegularFile(Paths.get(directoryPath, "id_rsa")) &&
        Files.isReadable(Paths.get(directoryPath, "id_rsa")) &&
        Files.isRegularFile(Paths.get(directoryPath, "id_rsa.pub")) &&
        Files.isReadable(Paths.get(directoryPath, "id_rsa.pub")));
  }

  public static String getDockerHostSshDirectoryTip() {
    return "Path to the directory containing SSH key files id_rsa and id_rsa.pub";
  }

  public static boolean validateDockerHostTlsDirectory(String directoryPath) {
    return (directoryPath != null && directoryPath.length() >= 0 &&
        Files.isDirectory(Paths.get(directoryPath)) &&
        Files.isRegularFile(Paths.get(directoryPath, "ca.pem")) &&
        Files.isReadable(Paths.get(directoryPath, "ca.pem")) &&
        Files.isRegularFile(Paths.get(directoryPath, "ca-key.pem")) &&
        Files.isReadable(Paths.get(directoryPath, "ca-key.pem")) &&
        Files.isRegularFile(Paths.get(directoryPath, "cert.pem")) &&
        Files.isReadable(Paths.get(directoryPath, "cert.pem")) &&
        Files.isRegularFile(Paths.get(directoryPath, "key.pem")) &&
        Files.isReadable(Paths.get(directoryPath, "key.pem")) &&
        Files.isRegularFile(Paths.get(directoryPath, "server.pem")) &&
        Files.isReadable(Paths.get(directoryPath, "server.pem")) &&
        Files.isRegularFile(Paths.get(directoryPath, "server-key.pem")) &&
        Files.isReadable(Paths.get(directoryPath, "server-key.pem")));
  }

  public static String getDockerHostTlsDirectoryTip() {
    return "Path to the directory containing TLS certificate files: ca.pem, ca-key.pem, cert.pem, key.pem, server.pem and server-key.pem";
  }

  public static boolean validateDockerHostPort(String name) {
    return (name != null && name.length() >= 4 && name.length() <= 5 && name.matches("[0-9]+") && Integer.parseInt(name) > 1024 && Integer.parseInt(name) <= 65535);
  }

  public static String getDockerHostPortTip() {
    return "Port settings should be an number between 1025 and 65535";
  }

  public static boolean validateDockerHostKeyvaultName(String name, AzureDockerHostsManager dockerManager, boolean checkUrl) {
    return (name != null && name.length() >= 3 && name.length() <= 24 && name.matches("^[A-Za-z][A-Za-z0-9-]*[A-Za-z0-9]$") && (!checkUrl || AzureDockerUtils.checkKeyvaultNameAvailability(name)));
  }

  public static String getDockerHostKeyvaultNameTip() {
    return "User name should be 3 to 24 lowercase alphanumeric characters and it should be unique across all Azure domain (DNS like)";
  }

}
