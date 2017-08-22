package com.microsoft.intellij.runner.container.dockerhost;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DockerHostRunModel {
    private String dockerHost;
    private String dockerCertPath;
    private boolean tlsEnabled;

    private String imageName;
    private String tagName;
    private String targetPath;
    private String targetName;

    public DockerHostRunModel() {
        try {
            dockerHost = DefaultDockerClient.fromEnv().uri().toString();
        } catch (DockerCertificateException e) {
            dockerHost = "";
        }
        dockerCertPath = "";
        tlsEnabled = false;
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());
        imageName = String.format("%s-%s", "localimage", date);
        tagName = "latest";
    }

    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    public String getDockerCertPath() {
        return dockerCertPath;
    }

    public void setDockerCertPath(String dockerCertPath) {
        this.dockerCertPath = dockerCertPath;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }
}
