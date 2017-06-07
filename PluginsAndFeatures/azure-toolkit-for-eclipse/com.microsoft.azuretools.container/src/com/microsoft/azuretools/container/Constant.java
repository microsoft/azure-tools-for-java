package com.microsoft.azuretools.container;

public class Constant {
	public final static String DOCKER_CONTEXT_FOLDER="/dockerContext/";
	public final static String DOCKERFILE_NAME="Dockerfile";
	public final static String TOMCAT_SERVICE_PORT = "8080";
	public final static String IMAGE_PREFIX = "local/tomcat";
	public final static String MESSAGE_INSTRUCTION = "Please make sure following environment variables are correctly set:\nDOCKER_HOST (default value: localhost:2375)\nDOCKER_CERT_PATH ";
	public final static String MESSAGE_DOCKERFILE_CREATED = "Dockerfile Successfully Created.";
	public final static String ERROR_CREATING_DOCKERFILE = "Error occurred in generating Dockerfile";
	public final static String DOCKERFILE_CONTENT_TOMCAT = "FROM tomcat:8.5-jre8\r\nCOPY %s.war /usr/local/tomcat/webapps/\r\n";
}
