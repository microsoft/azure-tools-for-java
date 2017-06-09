/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.container;

public class Constant {
    public final static int TIMEOUT_STOP_CONTAINER = 5;
    public final static String DOCKER_CONTEXT_FOLDER="/dockerContext/";
    public final static String DOCKERFILE_NAME="Dockerfile";
    public final static String TOMCAT_SERVICE_PORT = "8080";
    public final static String IMAGE_PREFIX = "local/tomcat";
    public final static String MESSAGE_INSTRUCTION = "(Set the DOCKER_HOST environment variable to connect elsewhere. "
            + "Set the DOCKER_CERT_PATH variable to connect TLS-enabled daemon.)";
    public final static String MESSAGE_INSTRUCTION_DEPRECATED = "Please make sure following environment variables are correctly set:\nDOCKER_HOST (default value: localhost:2375)\nDOCKER_CERT_PATH ";
    public final static String MESSAGE_DOCKERFILE_CREATED = "Dockerfile Successfully Created.";
    public final static String MESSAGE_CONFIRM_STOP_CONTAINER = "Running container detected. We will stop and remove it.\n Continue?";
    public final static String ERROR_CREATING_DOCKERFILE = "Error occurred in generating Dockerfile";
    public final static String ERROR_RUNNING_DOCKER = "Error occurred in Docker Run";
    public final static String DOCKERFILE_CONTENT_TOMCAT = "FROM tomcat:8.5-jre8\r\nCOPY %s.war /usr/local/tomcat/webapps/\r\n";
}
