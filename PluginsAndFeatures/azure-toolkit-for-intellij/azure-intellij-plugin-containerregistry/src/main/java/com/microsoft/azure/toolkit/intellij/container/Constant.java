/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.container;

public class Constant {
    public static final String DOCKERFILE_NAME = "Dockerfile";
    public static final String TOMCAT_SERVICE_PORT = "80";
    public static final String MESSAGE_INSTRUCTION = "(Set the DOCKER_HOST environment variable to connect elsewhere."
        + "Set the DOCKER_CERT_PATH variable to connect TLS-enabled daemon.)";
    public static final String MESSAGE_DOCKERFILE_CREATED = "Docker file created at: %s";
    public static final String DOCKERFILE_CONTENT_TOMCAT = " FROM mcr.microsoft.com/java/tomcat:8-zulu-alpine-tomcat-9\n" +
            "RUN rm -fr /usr/local/tomcat/webapps/ROOT\n" +
            "COPY %s /usr/local/tomcat/webapps/ROOT.war";
    public static final String DOCKERFILE_CONTENT_SPRING = "FROM azul/zulu-openjdk-alpine:11\n" +
            "VOLUME /tmp\n" +
            "EXPOSE 8080\n" +
            "COPY %s app.jar\n" +
            "ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar /app.jar";
    public static final String ERROR_NO_SELECTED_PROJECT = "Can't detect an active project";
    public static final String MESSAGE_CONTAINER_STARTED = "Container is running now!\n" +
            "URL: http://%s/";
    public static final String MESSAGE_ADD_DOCKER_SUPPORT_OK = "Successfully added docker support!";
    public static final String DOCKERFILE_ARTIFACT_PLACEHOLDER = "<artifact>";
}
