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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.azuretools.container.utils.DockerUtil;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

public class DockerRuntime {
    private static final DockerRuntime INSTANCE = new DockerRuntime();
    private static final String CONTAINER_ID_KEY = "ContainerId";
    private static final String DOCKER_HOST_RUN_SETTING_KEY = "DockerHostRunSetting";
    // project basePath as key
    private Map<String, Map<String, Object>> containerSettingMap = new ConcurrentHashMap<>();

    private DockerRuntime() {
    }

    public static DockerRuntime getInstance() {
        return INSTANCE;
    }

    /**
     * getRunningContainerId.
     *
     * @param key
     *            basePath of project as key
     */
    public synchronized String getRunningContainerId(String key) {
        if (containerSettingMap.containsKey(key)) {
            return (String) containerSettingMap.get(key).get(CONTAINER_ID_KEY);
        } else {
            return null;
        }
    }

    /**
     * setRunningContainerId.
     */
    public synchronized void setRunningContainerId(String key, String runningContainerId, DockerHostRunSetting model)
            throws DockerException, InterruptedException, DockerCertificateException {
        cleanRuningContainer(key);
        HashMap<String, Object> value = new HashMap<>();
        value.put(CONTAINER_ID_KEY, runningContainerId);
        value.put(DOCKER_HOST_RUN_SETTING_KEY, model);
        containerSettingMap.put(key, value);
    }

    /**
     * clean running container.
     */
    public synchronized void cleanRuningContainer(String key)
            throws DockerCertificateException, DockerException, InterruptedException {
        if (containerSettingMap.containsKey(key)) {
            String runningContainerId = (String) containerSettingMap.get(key).get(CONTAINER_ID_KEY);
            DockerHostRunSetting dataModel = (DockerHostRunSetting) containerSettingMap.get(key)
                    .get(DOCKER_HOST_RUN_SETTING_KEY);
            DockerClient docker = DockerUtil.getDockerClient(dataModel.getDockerHost(), dataModel.isTlsEnabled(),
                    dataModel.getDockerCertPath());
            docker.stopContainer(runningContainerId, Constant.TIMEOUT_STOP_CONTAINER);
            docker.removeContainer(runningContainerId);
        }
        containerSettingMap.remove(key);
    }

    /**
     * cleanAllRuningContainer.
     */
    public void cleanAllRuningContainer() {
        for (String key : containerSettingMap.keySet()) {
            try {
                cleanRuningContainer(key);
            } catch (DockerCertificateException | DockerException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        containerSettingMap.clear();
    }
}
