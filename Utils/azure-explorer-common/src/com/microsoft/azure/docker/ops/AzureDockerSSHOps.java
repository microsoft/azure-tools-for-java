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

import com.jcraft.jsch.*;
import com.microsoft.azure.docker.model.AzureDockerException;
import com.microsoft.azure.docker.model.DockerHost;

import java.io.*;
import java.net.URI;

public class AzureDockerSSHOps {
  public static Session createLoginInstance(DockerHost dockerHost) {
    if (dockerHost != null && dockerHost.certVault != null &&
        dockerHost.certVault.vmUsername != null && !dockerHost.certVault.vmUsername.isEmpty() &&
        ((dockerHost.certVault.vmPwd != null  && !dockerHost.certVault.vmPwd.isEmpty()) ||
         (dockerHost.certVault.sshPubKey != null && !dockerHost.certVault.sshPubKey.isEmpty()))) {
      try {
        JSch jsch = new JSch();
        jsch.setKnownHosts(System.getProperty("user.home")+"/.ssh/known_hosts");
        if (dockerHost.certVault.sshKey != null && !dockerHost.certVault.sshKey.isEmpty()) {
          jsch.addIdentity(dockerHost.certVault.hostName, dockerHost.certVault.sshKey.getBytes(), dockerHost.certVault.sshPubKey.getBytes(), (byte[]) null);
        }

        Session session = jsch.getSession(dockerHost.certVault.vmUsername, dockerHost.hostVM.dnsName);

        if (dockerHost.certVault.vmPwd != null && !dockerHost.certVault.vmPwd.isEmpty()) {
          session.setPassword(dockerHost.certVault.vmPwd);
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
        session.connect();

        return session;
      } catch (Exception e) {
        throw new AzureDockerException("Create Log In Instance: " + e.getMessage(), e);
      }
    } else {
      throw new AzureDockerException("Unexpected param values; dockerHost cannot be null");
    }
  }

  public static String executeCommand(String command, Session session, Boolean getExitStatus) {
    return executeCommand(command, session, getExitStatus, false);
  }

  public static String executeCommand(String command, Session session, Boolean getExitStatus, Boolean withErr) {
    String result = "";
    String resultErr = "";
    try {
      Channel channel = session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);
      InputStream commandOutput = channel.getInputStream();
      InputStream commandErr = ((ChannelExec) channel).getErrStream();
      channel.connect();
      byte[] tmp  = new byte[4096];
      while(true){
        while(commandOutput.available()>0){
          int i=commandOutput.read(tmp, 0, 4096);
          if(i<0)break;
          result += new String(tmp, 0, i);
        }
        while(commandErr.available()>0){
          int i=commandErr.read(tmp, 0, 4096);
          if(i<0)break;
          resultErr += new String(tmp, 0, i);
        }
        if(channel.isClosed()){
          if(commandOutput.available()>0) continue;
          if (getExitStatus) {
            result += "exit-status: " + channel.getExitStatus();
            if (withErr) {
              result += "\n With error:\n" + resultErr;
            }
          }
          break;
        }
        try{Thread.sleep(100);}catch(Exception ee){}
      }
      channel.disconnect();

      return result;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static String download(Session session, String fileName, String fromPath, boolean isUserHomeBased) {
    try {
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BufferedOutputStream buff = new BufferedOutputStream(outputStream);
      String absolutePath = isUserHomeBased ? channel.getHome() + "/" + fromPath : fromPath;
      channel.cd(absolutePath);
      channel.get(fileName, buff);

      channel.disconnect();

      return outputStream.toString();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void download(Session session, String fileName, String fromPath, String toPath) {
    try {
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      File toFile = new File(toPath, fileName);
      OutputStream outputStream = new FileOutputStream(toFile);
      BufferedOutputStream buff = new BufferedOutputStream(outputStream);
      channel.cd(fromPath);
      channel.get(fileName, buff);

      channel.disconnect();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void upload(Session session, InputStream from, String fileName, String toPath, boolean isUserHomeBased, String filePerm) {
    try {
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      String absolutePath = isUserHomeBased ? channel.getHome() + "/" + toPath : toPath;

      String path = "";
      for (String dir : absolutePath.split("/")) {
        path = path + "/" + dir;
        try {
          channel.mkdir(path);
        } catch (Exception ee) {
        }
      }
      channel.cd(absolutePath);
      channel.put(from, fileName);
      if (filePerm != null) {
        channel.chmod(Integer.parseInt(filePerm), absolutePath + "/" + fileName);
      }

      channel.disconnect();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void upload(Session session, String fileName, String fromPath, String toPath, boolean isUserHomeBased, String filePerm) {
    try {
      FileInputStream inputStream = new FileInputStream(fromPath + File.separator + fileName);
      ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
      String absolutePath = isUserHomeBased ? channel.getHome() + "/" + toPath : toPath;

      String path = "";
      for (String dir : absolutePath.split("/")) {
        path = path + "/" + dir;
        try {
          channel.mkdir(path);
        } catch (Exception ee) {
        }
      }
      channel.cd(toPath);
      channel.put(inputStream, fileName);

      channel.disconnect();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }
}
