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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jcraft.jsch.Session;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DockerHost {
  public String name;
  public AzureDockerVM hostVM;
  public DockerHostVMState state;
  public boolean hasPwdLogIn;
  public boolean hasSSHLogIn;
  public DockerHostOSType hostOSType;
  public AzureDockerCertVault certVault; // see using Azure Key Vault to store secrets
  public boolean isTLSSecured;
  public boolean hasKeyVault;
  public String apiUrl;
  public String port;
  public Map<String, DockerImage> dockerImages;
  public boolean isUpdating;
  public String sid;

  @JsonIgnore
  public Session session;

  public DockerHost() {}

  public DockerHost(DockerHost copyHost) {
    if (copyHost == null) {
      return;
    }

    this.name = copyHost.name;
    this.hostVM = copyHost.hostVM; // this is a safe assignment; hostVM's properties should not be updated
    this.state = copyHost.state;
    this.hasPwdLogIn = copyHost.hasPwdLogIn;
    this.hasSSHLogIn = copyHost.hasSSHLogIn;
    this.hostOSType = copyHost.hostOSType;
    this.certVault = (copyHost.certVault != null) ? new AzureDockerCertVault(copyHost.certVault) : null;
    this.isTLSSecured = copyHost.isTLSSecured;
    this.hasKeyVault = copyHost.hasKeyVault;
    this.apiUrl = copyHost.apiUrl;
    this.port = copyHost.port;
    this.dockerImages = copyHost.dockerImages;
    this.session = null;
    this.isUpdating = copyHost.isUpdating;
    this.sid = copyHost.sid;
  }

  public boolean equalsTo(DockerHost otherHost) {
    return Objects.equals(this.name, otherHost.name) &&
        this.hostVM == otherHost.hostVM &&
        this.state == otherHost.state &&
        this.hasPwdLogIn == otherHost.hasPwdLogIn &&
        this.hasSSHLogIn == otherHost.hasSSHLogIn &&
        this.isTLSSecured == otherHost.isTLSSecured &&
        this.hostOSType == otherHost.hostOSType &&
        this.certVault.equalsTo(otherHost.certVault) &&
        this.hasKeyVault == otherHost.hasKeyVault &&
        Objects.equals(this.apiUrl, otherHost.apiUrl) &&
        Objects.equals(this.port, otherHost.port);
  }

  public enum DockerHostOSType {
    UBUNTU_SERVER_16_04_LTS,
    UBUNTU_SERVER_14_04_LTS,
    UBUNTU_SNAPPY_CORE_15_04,
    COREOS_STABLE_LATEST,
    OPENLOGIC_CENTOS_7_2,
    LINUX_OTHER
  }

  public enum DockerHostVMState {
    RUNNING,
    DEALLOCATING,
    DEALLOCATED,
    STARTING,
    STOPPED,
    UNKNOWN,
    UPDATING,
    TO_BE_CREATED
  }
}
