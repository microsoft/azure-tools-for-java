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

public class AzureDockerVMSetupScriptsForUbuntu {
  public static final String INSTALL_DOCKER_FOR_UBUNTU_SERVER_16_04_LTS = "" +
      "echo Running: \"if [ ! -d ~/.azuredocker/tls ]; then mkdir -p ~/.azuredocker/tls ; fi\" &&" +
      "if [ ! -d ~/.azuredocker/tls ]; then mkdir -p ~/.azuredocker/tls ; fi &&" +
      "echo Running: sudo apt-get update &&" +
      "sudo apt-get update &&" +
      "echo Running: sudo apt-get install -y --no-install-recommends apt-transport-https ca-certificates curl software-properties-common &&" +
      "sudo apt-get install -y --no-install-recommends apt-transport-https ca-certificates curl software-properties-common &&" +
      "echo Running: curl -fsSL https://apt.dockerproject.org/gpg | sudo apt-key add - &&" +
      "curl -fsSL https://apt.dockerproject.org/gpg | sudo apt-key add - &&" +
      "echo Running: sudo add-apt-repository \"deb https://apt.dockerproject.org/repo/ ubuntu-$(lsb_release -cs) main\" &&" +
      "sudo add-apt-repository \"deb https://apt.dockerproject.org/repo/ ubuntu-xenial main\" &&" +
      "echo Running: sudo apt-get update &&" +
      "sudo apt-get update &&" +
      "echo Running: sudo apt-get -y install docker-engine &&" +
      "sudo apt-get -y install docker-engine &&" +
      "echo Running: sudo groupadd docker &&" +
      "sudo groupadd docker &&" +
      "echo Running: sudo usermod -aG docker $USER &&" +
      "sudo usermod -aG docker $USER \n";

  public static final String INSTALL_DOCKER_FOR_UBUNTU_SERVER_14_04_LTS = "" +
      "echo Running: \"if [ ! -d ~/.azuredocker/tls ]; then mkdir -p ~/.azuredocker/tls ; fi\" &&" +
      "if [ ! -d ~/.azuredocker/tls ]; then mkdir -p ~/.azuredocker/tls ; fi &&" +
      "echo Running: sudo apt-get update &&" +
      "sudo apt-get update &&" +
      "echo Running: sudo apt-get install -y --no-install-recommends apt-transport-https ca-certificates curl software-properties-common &&" +
      "sudo apt-get install -y --no-install-recommends apt-transport-https ca-certificates curl software-properties-common &&" +
      "echo Running: curl -fsSL https://apt.dockerproject.org/gpg | sudo apt-key add - &&" +
      "curl -fsSL https://apt.dockerproject.org/gpg | sudo apt-key add - &&" +
      "echo Running: sudo add-apt-repository \"deb https://apt.dockerproject.org/repo/ ubuntu-$(lsb_release -cs) main\" &&" +
      "sudo add-apt-repository \"deb https://apt.dockerproject.org/repo/ ubuntu-trusty main\" &&" +
      "echo Running: sudo apt-get update &&" +
      "sudo apt-get update &&" +
      "echo Running: sudo apt-get -y install docker-engine &&" +
      "sudo apt-get -y install docker-engine &&" +
      "echo Running: sudo groupadd docker &&" +
      "sudo groupadd docker &&" +
      "echo Running: sudo usermod -aG docker $USER &&" +
      "sudo usermod -aG docker $USER \n";

  public static final String DOCKER_API_PORT_TLS_DISABLED = "2375";
  public static final String DOCKER_API_PORT_TLS_ENABLED = "2376";

  /* Bash script that creates a default unsecured Docker configuration file; must be run on the Docker dockerHost after the VM is provisioned
   * Values to be replaced via String.replace()
   *  "$DOCKER_HOST_PORT_PARAM$" - TCP port to be opened for communicating with Docker API
   */
  public static final String CREATE_DEFAULT_DOCKER_OPTS_TLS_DISABLED = "" +
      "echo Running: sudo service docker stop &&" +
      "sudo service docker stop &&" +
      "echo Running: mkdir ~/.azuredocker &&" +
      "mkdir ~/.azuredocker &&" +
      "echo Running: sudo echo DOCKER_OPTS=\\\"--tls=false -H tcp://0.0.0.0:$DOCKER_API_PORT_PARAM$ -H unix:///var/run/docker.sock\\\" > ~/.azuredocker/docker.config &&" +
      "sudo echo DOCKER_OPTS=\\\"--tls=false -H tcp://0.0.0.0:$DOCKER_API_PORT_PARAM$ -H unix:///var/run/docker.sock\\\" > ~/.azuredocker/docker.config &&" +
      "echo Running: sudo cp -f ~/.azuredocker/docker.config /etc/default/docker &&" +
      "sudo cp -f ~/.azuredocker/docker.config /etc/default/docker &&" +
      "echo Running: sudo service docker start &&" +
      "sudo service docker start \n";

  /* Bash script that creates a default TLS secured Docker configuration file; must be run on the Docker dockerHost after the VM is provisioned
   * Values to be replaced via String.replace()
   *  "$DOCKER_API_PORT_PARAM$" - TCP port to be opened for communicating with Docker API
   */
  public static final String CREATE_DEFAULT_DOCKER_OPTS_TLS_ENABLED = "" +
      "echo Running: sudo service docker stop &&" +
      "sudo service docker stop &&" +
      "echo Running: mkdir ~/.azuredocker &&" +
      "mkdir ~/.azuredocker &&" +
      "echo Running: sudo echo DOCKER_OPTS=\\\"--tlsverify --tlscacert=/etc/docker/tls/ca.pem --tlscert=/etc/docker/tls/server.pem --tlskey=/etc/docker/tls/server-key.pem -H tcp://0.0.0.0:$DOCKER_API_PORT_PARAM$ -H unix:///var/run/docker.sock\\\" > ~/.azuredocker/docker.config &&" +
      "sudo echo DOCKER_OPTS=\\\"--tlsverify --tlscacert=/etc/docker/tls/ca.pem --tlscert=/etc/docker/tls/server.pem --tlskey=/etc/docker/tls/server-key.pem -H tcp://0.0.0.0:$DOCKER_API_PORT_PARAM$ -H unix:///var/run/docker.sock\\\" > ~/.azuredocker/docker.config &&" +
      "echo Running: sudo cp -f ~/.azuredocker/docker.config /etc/default/docker &&" +
      "sudo cp -f ~/.azuredocker/docker.config /etc/default/docker &&" +
      "echo Running: sudo service docker start &&" +
      "sudo service docker start \n";

  public static final String DOCKER_API_PORT_PARAM = "[$]DOCKER_API_PORT_PARAM[$]";

  /* Bash script that creates the TLS certs; must be run on the Docker dockerHost after the VM is provisioned
   * Values to be replaced via String.replace()
   *  "$CERT_CA_PWD_PARAM$" - some randomly generated password
   *  "$HOSTNAME$" - Docker dockerHost name
   *  "$FQDN_PARAM$" - fully qualified name of the Docker dockerHost
   *  "$DNS_PARAM$" - domain of the Docker dockerHost
   */
  public static final String CERT_CA_PWD_PARAM = "[$]CERT_CA_PWD_PARAM[$]";
  public static final String HOSTNAME = "[$]HOSTNAME[$]";
  public static final String FQDN_PARAM = "[$]FQDN_PARAM[$]";
  public static final String DOMAIN_PARAM = "[$]DOMAIN_PARAM[$]";
  public static final String CREATE_OPENSSL_TLS_CERTS_FOR_UBUNTU = "" +
      "echo Running: \"if [ ! -d ~/.azuredocker/tls ]; then rm -f -r ~/.azuredocker/tls ; fi\" &&" +
      "if [ ! -d ~/.azuredocker/tls ]; then rm -f -r ~/.azuredocker/tls ; fi &&" +
      "echo Running: mkdir -p ~/.azuredocker/tls &&" +
      "mkdir -p ~/.azuredocker/tls &&" +
      "echo Running: cd ~/.azuredocker/tls &&" +
      "cd ~/.azuredocker/tls &&" +
      // Generate CA certificate
      "echo Running: openssl genrsa -passout pass:$CERT_CA_PWD_PARAM$ -aes256 -out ca-key.pem 2048 &&" +
      "openssl genrsa -passout pass:$CERT_CA_PWD_PARAM$ -aes256 -out ca-key.pem 2048 &&" +
      // Generate Server certificates
      "echo Running: openssl req -passin pass:$CERT_CA_PWD_PARAM$ -subj '/CN=Docker Host CA/C=US' -new -x509 -days 365 -key ca-key.pem -sha256 -out ca.pem &&" +
      "openssl req -passin pass:$CERT_CA_PWD_PARAM$ -subj '/CN=Docker Host CA/C=US' -new -x509 -days 365 -key ca-key.pem -sha256 -out ca.pem &&" +
      "echo Running: openssl genrsa -out server-key.pem 2048 &&" +
      "openssl genrsa -out server-key.pem 2048 &&" +
//      "openssl req -passin pass:$CERT_CA_PWD_PARAM$ -subj '/CN=$HOSTNAME$' -sha256 -new -key server-key.pem -out server.csr \n" +
      "echo Running: openssl req -subj '/CN=$HOSTNAME$' -sha256 -new -key server-key.pem -out server.csr &&" +
      "openssl req -subj '/CN=$HOSTNAME$' -sha256 -new -key server-key.pem -out server.csr &&" +
      "echo Running: \"echo subjectAltName = DNS:$FQDN_PARAM$, DNS:*$DOMAIN_PARAM$, IP:127.0.0.1 > extfile.cnf \" &&" +
      "echo subjectAltName = DNS:$FQDN_PARAM$, DNS:*$DOMAIN_PARAM$, IP:127.0.0.1 > extfile.cnf &&" +
      "echo Running: openssl x509 -req -passin pass:$CERT_CA_PWD_PARAM$ -days 365 -sha256 -in server.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out server.pem -extfile extfile.cnf &&" +
      "openssl x509 -req -passin pass:$CERT_CA_PWD_PARAM$ -days 365 -sha256 -in server.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out server.pem -extfile extfile.cnf &&" +
      // Generate Client certificates
      "echo Running: openssl genrsa -passout pass:$CERT_CA_PWD_PARAM$ -out key.pem &&" +
      "openssl genrsa -passout pass:$CERT_CA_PWD_PARAM$ -out key.pem &&" +
      "echo Running: openssl req -passin pass:$CERT_CA_PWD_PARAM$ -subj '/CN=client' -new -key key.pem -out client.csr &&" +
      "openssl req -passin pass:$CERT_CA_PWD_PARAM$ -subj '/CN=client' -new -key key.pem -out client.csr &&" +
      "echo Running: \"echo extendedKeyUsage = clientAuth,serverAuth > extfile.cnf \" &&" +
      "echo extendedKeyUsage = clientAuth,serverAuth > extfile.cnf &&" +
      "echo Running: openssl x509 -req -passin pass:$CERT_CA_PWD_PARAM$ -days 365 -sha256 -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out cert.pem -extfile extfile.cnf &&" +
      "openssl x509 -req -passin pass:$CERT_CA_PWD_PARAM$ -days 365 -sha256 -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out cert.pem -extfile extfile.cnf &&" +
      "echo Running: cd ~ &&" +
      "cd ~ &&";

  /* Bash script that sets up the TLS certificates to be used in a secured Docker configuration file; must be run on the Docker dockerHost after the VM is provisioned
   */
  public static final String INSTALL_DOCKER_TLS_CERTS_FOR_UBUNTU = "" +
      "echo \"if [ ! -d /etc/docker/tls ]; then sudo mkdir -p /etc/docker/tls ; fi\" &&" +
      "if [ ! -d /etc/docker/tls ]; then sudo mkdir -p /etc/docker/tls ; fi &&" +
      "echo sudo cp -f ~/.azuredocker/tls/ca.pem /etc/docker/tls/ca.pem &&" +
      "sudo cp -f ~/.azuredocker/tls/ca.pem /etc/docker/tls/ca.pem &&" +
      "echo sudo cp -f ~/.azuredocker/tls/server.pem /etc/docker/tls/server.pem &&" +
      "sudo cp -f ~/.azuredocker/tls/server.pem /etc/docker/tls/server.pem &&" +
      "echo sudo cp -f ~/.azuredocker/tls/server-key.pem /etc/docker/tls/server-key.pem &&" +
      "sudo cp -f ~/.azuredocker/tls/server-key.pem /etc/docker/tls/server-key.pem &&" +
      "echo sudo chmod 644 /etc/docker/tls/* &&" +
      "sudo chmod 644 /etc/docker/tls/* \n";

  private static final String GET_DOCKERHOST_TLSCACERT_FOR_UBUNTU =
      "cat ~/.azuredocker/tls/ca.pem";
  private static final String GET_DOCKERHOST_TLSCERT_FOR_UBUNTU =
      "cat ~/.azuredocker/tls/cert.pem";
  private static final String GET_DOCKERHOST_TLSCLIENTKEY_FOR_UBUNTU =
      "cat ~/.azuredockertls/key.pem";
  private static final String GET_DOCKERHOST_TLSSERVERCERT_FOR_UBUNTU =
      "cat ~/.azuredocker/tls/server.pem";
  private static final String GET_DOCKERHOST_TLSSERVERKEY_FOR_UBUNTU =
      "cat ~/.azuredocker/tls/server-key.pem";


  public static final String DOCKER_IMAGE_NAME_PARAM = "[$]DOCKER_IMAGE_NAME_PARAM[$]";
  public static final String DOCKER_CONTAINER_NAME_PARAM = "[$]DOCKER_CONTAINER_NAME_PARAM[$]";
  public static final String DOCKER_ARTIFACT_FILENAME = "[$]DOCKER_ARTIFACT_FILENAME[$]";

  /* Bash script that creates the local directory which stores the artifact and Dockerfile
   * Values to be replaced via String.replace()
   *  "[$]DOCKER_IMAGE_NAME_PARAM[$]" - name of the Docker image to be created
   *  "[$]DOCKER_CONTAINER_NAME_PARAM[$]" - name of the Docker container to be created
   *  "[$]DOCKER_ARTIFACT_FILENAME[$]" - Docker dockerHost name
   */
  public static final String CREATE_DOCKER_IMAGE_DIRECTORY_FOR_UBUNTU = "" +
      "echo Running: \"if [ ! -d ~/.azuredocker/images/[$]DOCKER_IMAGE_NAME_PARAM[$] ]; then rm -f -r ~/.azuredocker/images/[$]DOCKER_IMAGE_NAME_PARAM[$] ; fi\" &&" +
      "if [ ! -d ~/.azuredocker/images/[$]DOCKER_IMAGE_NAME_PARAM[$] ]; then rm -f -r ~/.azuredocker/images/[$]DOCKER_IMAGE_NAME_PARAM[$] ; fi &&" +
      "echo Running: mkdir -p ~/.azuredocker/tls &&" +
      "mkdir -p ~/.azuredocker/images/[$]DOCKER_IMAGE_NAME_PARAM[$] \n";

  public static final String DEFAULT_DOCKER_IMAGES_DIRECTORY = "~/.azuredocker/images";

  public static final String UPDATE_CURRENT_DOCKER_USER = "" +
      "echo Running: sudo usermod -aG docker $USER &&" +
      "sudo usermod -aG docker $USER \n";

}
