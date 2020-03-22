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
package com.microsoft.azuretools.container;

public class Constant {
    public static final int TIMEOUT_STOP_CONTAINER = 5;
    public static final String CONSOLE_NAME = "AzureToolsConsole";
    public static final String DOCKERFILE_FOLDER = ".";
    public static final String DOCKERFILE_NAME = "Dockerfile";
    public static final String TOMCAT_SERVICE_PORT = "8080";
    public static final String DEFAULT_IMAGE_NAME_WITH_TAG = "localimage:latest";
    public static final String MESSAGE_INSTRUCTION = "(Set the DOCKER_HOST environment variable to connect elsewhere."
            + "Set the DOCKER_CERT_PATH variable to connect TLS-enabled daemon.)";
    public static final String MESSAGE_INSTRUCTION_DEPRECATED = "Please make sure following environment variables are"
            + " correctly set:\nDOCKER_HOST (default value: localhost:2375)\nDOCKER_CERT_PATH ";
    public static final String MESSAGE_DOCKERFILE_CREATED = "Docker file created at: %s";
    public static final String MESSAGE_CONFIRM_STOP_CONTAINER = "Running container detected. We will stop and remove "
            + "it.\n Would you like to continue?";
    public static final String MESSAGE_DOCKER_CONNECTING = "Connecting to docker daemon ... ";
    public static final String ERROR_CREATING_DOCKERFILE = "Error occurred in generating Dockerfile, "
            + "with exception:\n%s";
    public static final String ERROR_RUNNING_DOCKER = "Error occurred in Docker Run, with exception:\n%s";
    public static final String DOCKERFILE_CONTENT_TOMCAT = "FROM tomcat:8.5-jre8\r\n"
            + "RUN rm -fr /usr/local/tomcat/webapps/ROOT\r\n" + "COPY %s /usr/local/tomcat/webapps/ROOT.war\r\n";
    public static final String DOCKERFILE_CONTENT_SPRING = "FROM azul/zulu-openjdk-alpine:8\r\n" + "VOLUME /tmp\r\n"
            + "EXPOSE 8080\r\n" + "COPY %s app.jar\r\n"
            + "ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar /app.jar";
    public static final String ERROR_NO_SELECTED_PROJECT = "Can't detect an active project";
    public static final String MESSAGE_EXPORTING_PROJECT = "Packaging project into artifact: %s";
    public static final String MESSAGE_BUILDING_IMAGE = "Building Image ...";
    public static final String MESSAGE_PACKAGING_PROJECT = "Packaging project ...";
    public static final String MESSAGE_IMAGE_INFO = "Image name: %s";
    public static final String MESSAGE_CREATING_CONTAINER = "Creating container ...";
    public static final String MESSAGE_CONTAINER_INFO = "Container Id: %s";
    public static final String MESSAGE_STARTING_CONTAINER = "Starting container ...";
    public static final String MESSAGE_CONTAINER_STARTED = "Container is running now!\nURL: \nhttp://%s";
    public static final String ERROR_STARTING_CONTAINER = "Fail to start Container #id=%s";
    public static final String MESSAGE_ADD_DOCKER_SUPPORT_OK = "Successfully added docker support!";
    public static final String MESSAGE_ADDING_DOCKER_SUPPORT = "Adding docker support ...";
    public static final String MESSAGE_DOCKER_HOST_INFO = "Default docker host: %s";
    public static final String MESSAGE_EXECUTE_DOCKER_RUN = "Executing Docker Run...";
    public static final String ERROR_BUILDING_IMAGE = "Fail to build image. Please verify docker host connection "
            + "and Dockerfile.\n\nDockerHost: \t%s\nDockerfile: \t%s\n\nError Message:\n%s";
    public static final String DOCKERFILE_ARTIFACT_PLACEHOLDER = "<artifact>";
}
