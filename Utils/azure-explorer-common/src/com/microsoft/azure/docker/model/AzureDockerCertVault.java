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

import java.util.Objects;

public class AzureDockerCertVault {
  public String name;                   // Azure Key Vault's name
  public String uri;                    // Azure Key Vault's uri
  public String id;                     // Azure Key Vault's id
  public String hostName;               // name of the dockerHost (Docker dockerHost) to store the credentials for
  public String resourceGroupName;      // Azure resource group where to create the vault
  public String region;                 // Azure location where to create the vault
  public String userId;                 // current user id or client id as logged in via Azure Auth
  public String sid;                    // subscription id to which the resource belongs to
  public String vmUsername;             // username to be used fro VM (Docker dockerHost) login
  public String vmPwd;                  // password to be used for VM (Docker dockerHost) login
  public String sshKey;                 // see "id_rsa"
  public String sshPubKey;              // see "id_rsa.pub"
  public String tlsCACert;              // see "ca.pem"
  public String tlsCAKey;               // see "ca-key.pem"
  public String tlsClientCert;          // see "cert.pem"
  public String tlsClientKey;           // see "key.pem"
  public String tlsServerCert;          // see "server.pem"
  public String tlsServerKey;           // see "server-key.pem"

  // TODO: remove this member!!!
  public String servicePrincipalId;     // service principal's client id if user logged in with service principal credentials

  public AzureDockerCertVault() {}

  public AzureDockerCertVault(AzureDockerCertVault otherVault) {
    this.name = otherVault.name;
    this.uri = otherVault.uri;
    this.id = otherVault.id;
    this.hostName = otherVault.hostName;
    this.resourceGroupName = otherVault.resourceGroupName;
    this.region = otherVault.region;
    this.sid = otherVault.sid;
    this.servicePrincipalId = otherVault.servicePrincipalId;
    this.userId = otherVault.userId;
    this.vmUsername = otherVault.vmUsername;
    this.vmPwd = otherVault.vmPwd;
    this.sshKey = otherVault.sshKey;
    this.sshPubKey = otherVault.sshPubKey;
    this.tlsCACert = otherVault.tlsCACert;
    this.tlsCAKey = otherVault.tlsCAKey;
    this.tlsClientCert = otherVault.tlsClientCert;
    this.tlsClientKey = otherVault.tlsClientKey;
    this.tlsServerCert = otherVault.tlsServerCert;
    this.tlsServerKey = otherVault.tlsServerKey;
  }

  public boolean equalsTo(AzureDockerCertVault otherVault) {
    return Objects.equals(this.name, otherVault.name) &&
        Objects.equals(this.id, otherVault.id) &&
        Objects.equals(this.hostName, otherVault.hostName) &&
        Objects.equals(this.resourceGroupName, otherVault.resourceGroupName) &&
        Objects.equals(this.region, otherVault.region) &&
        Objects.equals(this.userId, otherVault.userId) &&
        Objects.equals(this.vmUsername, otherVault.vmUsername) &&
        Objects.equals(this.vmPwd, otherVault.vmPwd) &&
        Objects.equals(this.sshKey, otherVault.sshKey) &&
        Objects.equals(this.sshPubKey, otherVault.sshPubKey) &&
        Objects.equals(this.tlsCACert, otherVault.tlsCACert) &&
        Objects.equals(this.tlsCAKey, otherVault.tlsCAKey) &&
        Objects.equals(this.tlsClientKey, otherVault.tlsClientKey) &&
        Objects.equals(this.tlsServerCert, otherVault.tlsServerCert) &&
        Objects.equals(this.tlsServerKey, otherVault.tlsServerKey);
  }

  public String toString() { return this.name; }
}
