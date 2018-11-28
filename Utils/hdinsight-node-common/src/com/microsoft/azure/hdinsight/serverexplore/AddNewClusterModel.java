/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.serverexplore;

import com.microsoft.azure.hdinsight.sdk.cluster.SparkClusterType;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddNewClusterModel implements Cloneable {
    private String clusterNameLabelTitle;
    private String userNameLabelTitle;
    private String passwordLabelTitle;

    @NotNull
    private SparkClusterType sparkClusterType;
    private String clusterName;
    private String userName;
    private String password;
    @Nullable
    private URI livyEndpoint;
    @Nullable
    private URI yarnEndpoint;

    private String storageName;
    private String storageKey;

    @Nullable
    private String errorMessage;

    @Nullable
    private List<String> containers;
    private int selectedContainerIndex;      // -1 for non-selection

    public String getClusterNameLabelTitle() {
        return clusterNameLabelTitle;
    }

    public AddNewClusterModel setClusterNameLabelTitle(String clusterNameLabelTitle) {
        this.clusterNameLabelTitle = clusterNameLabelTitle;

        return this;
    }

    public String getUserNameLabelTitle() {
        return userNameLabelTitle;
    }

    public AddNewClusterModel setUserNameLabelTitle(String userNameLabelTitle) {
        this.userNameLabelTitle = userNameLabelTitle;

        return this;
    }

    public String getPasswordLabelTitle() {
        return passwordLabelTitle;
    }

    public AddNewClusterModel setPasswordLabelTitle(String passwordLabelTitle) {
        this.passwordLabelTitle = passwordLabelTitle;

        return this;
    }

    public SparkClusterType getSparkClusterType() {
        return sparkClusterType;
    }

    public AddNewClusterModel setSparkClusterType(@NotNull SparkClusterType sparkClusterType) {
        this.sparkClusterType = sparkClusterType;
        return this;
    }

    public String getClusterName() {
        return clusterName;
    }

    public AddNewClusterModel setClusterName(String clusterName) {
        this.clusterName = clusterName;

        return this;
    }

    public String getUserName() {
        return userName;
    }

    public AddNewClusterModel setUserName(String userName) {
        this.userName = userName;

        return this;
    }

    public String getPassword() {
        return password;
    }

    public AddNewClusterModel setPassword(String password) {
        this.password = password;

        return this;
    }

    public String getStorageName() {
        return storageName;
    }

    public AddNewClusterModel setStorageName(String storageName) {
        this.storageName = storageName;

        return this;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public AddNewClusterModel setStorageKey(String storageKey) {
        this.storageKey = storageKey;

        return this;
    }

    @Nullable
    public URI getLivyEndpoint() {
        return livyEndpoint;
    }

    public AddNewClusterModel setLivyEndpoint(@Nullable URI livyEndpoint) {
        this.livyEndpoint = livyEndpoint;
        return this;
    }

    @Nullable
    public URI getYarnEndpoint() {
        return yarnEndpoint;
    }

    public AddNewClusterModel setYarnEndpoint(@Nullable URI yarnEndpoint) {
        this.yarnEndpoint = yarnEndpoint;
        return this;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public AddNewClusterModel setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;

        return this;
    }

    @NotNull
    public List<String> getContainers() {
        return Optional.ofNullable(containers)
                .orElse(new ArrayList<>());
    }

    public AddNewClusterModel setContainers(@Nullable List<String> containers) {
        this.containers = containers;

        return this;
    }

    public int getSelectedContainerIndex() {
        return selectedContainerIndex;
    }

    public AddNewClusterModel setSelectedContainerIndex(int selectedContainerIndex) {
        this.selectedContainerIndex = selectedContainerIndex;

        return this;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // Here is a shadow clone, not deep clone
        return super.clone();
    }
}
