/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.google.gson.Gson;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.core.mvp.model.container.ContainerExplorerMvpModel;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.model.container.pojo.Catalog;
import com.microsoft.azuretools.core.mvp.model.container.pojo.Tag;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.containerregistry.ContainerRegistryProperty;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class ContainerRegistryPropertyViewPresenter<V extends ContainerRegistryPropertyMvpView>
        extends MvpPresenter<V> {

    private static final String CANNOT_GET_SUBSCRIPTION_ID = "Cannot get Subscription ID.";
    private static final String CANNOT_GET_REGISTRY_ID = "Cannot get Container Registry's ID.";
    private static final String CANNOT_GET_REGISTRY_PROPERTY = "Cannot get Container Registry's property.";
    private static final String CANNOT_GET_REPOS = "Cannot get repositories.";
    private static final String CANNOT_GET_TAGS = "Cannot get tags.";

    private static final String BODY = "body";
    private static final String PAGE_SIZE = "30";
    private static final String KEY_LAST = "last";
    private static final String KEY_PAGE_SIZE = "n";
    private static final String HEADER_LINK = "link";
    private static final String FAKE_URL = "http://a";
    private final Stack<String> repoStack = new Stack<>();
    private final Stack<String> tagStack = new Stack<>();
    private String currentRepo;
    private String currentTag;
    private String nextRepo;
    private String nextTag;

    /**
     * Constructor.
     */
    public ContainerRegistryPropertyViewPresenter() {
        resetRepoStack();
        resetTagStack();
    }

    /**
     * Called by view when the view needs to load the property of an ACR.
     */
    public void onGetRegistryProperty(String sid, String id) {
        if (isSubscriptionIdAndResourceIdInValid(sid, id)) {
            return;
        }
        Observable.fromCallable(() -> ContainerRegistryMvpModel.getInstance().getContainerRegistry(sid, id))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(registry -> AzureTaskManager.getInstance().runLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    if (registry == null) {
                        getMvpView().onError(CANNOT_GET_REGISTRY_PROPERTY);
                        return;
                    }
                    ContainerRegistryProperty property = getProperty(registry, sid);
                    getMvpView().showProperty(property);
                }), e -> errorHandler(CANNOT_GET_REGISTRY_PROPERTY, (Exception) e));
    }

    /**
     * Called when switching admin user enabled status.
     */
    public void onEnableAdminUser(String sid, String id, boolean b) {
        if (isSubscriptionIdAndResourceIdInValid(sid, id)) {
            return;
        }
        Observable.fromCallable(() -> ContainerRegistryMvpModel.getInstance().setAdminUserEnabled(sid, id, b))
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(registry -> AzureTaskManager.getInstance().runLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    if (registry == null) {
                        getMvpView().onError(CANNOT_GET_REGISTRY_PROPERTY);
                        return;
                    }
                    ContainerRegistryProperty property = getProperty(registry, sid);
                    getMvpView().showProperty(property);
                }), e -> errorHandler(CANNOT_GET_REGISTRY_PROPERTY, (Exception) e));
    }

    /**
     * Called when refreshing repositories in ACR Explorer
     */
    public void onRefreshRepositories(String sid, String id, boolean isNextPage) {
        resetRepoStack();
        onListRepositories(sid, id, isNextPage);
    }

    /**
     * Called when listing repositories of ACR.
     */
    public void onListRepositories(String sid, String id, boolean isNextPage) {
        if (isSubscriptionIdAndResourceIdInValid(sid, id)) {
            return;
        }
        resetTagStack();
        Observable.fromCallable(() -> {
            ContainerRegistry registry = ContainerRegistryMvpModel.getInstance().getContainerRegistry(sid, id);
            PrivateRegistryImageSetting setting = ContainerRegistryMvpModel.getInstance()
                    .createImageSettingWithRegistry(registry);
            Map<String, String> query = buildQueryMap(isNextPage, repoStack, nextRepo);
            Map<String, String> responseMap = ContainerExplorerMvpModel.getInstance().listRepositories(registry
                    .getLoginServerUrl(), setting.getUsername(), setting.getPassword(), query);
            updatePaginationInfo(isNextPage, Type.REPO, responseMap.get(HEADER_LINK));
            Gson gson = new Gson();
            Catalog catalog = gson.fromJson(responseMap.get(BODY), Catalog.class);
            return catalog.getRepositories();
        })
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(repos -> AzureTaskManager.getInstance().runLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().listRepo(repos);
                }), e -> errorHandler(CANNOT_GET_REPOS, (Exception) e));
    }

    /**
     * Called when listing image tags for the given repository.
     */
    public void onListTags(String sid, String id, String repo, boolean isNextPage) {
        if (isSubscriptionIdAndResourceIdInValid(sid, id)) {
            return;
        }
        resetTagStack();
        Observable.fromCallable(() -> {
            ContainerRegistry registry = ContainerRegistryMvpModel.getInstance().getContainerRegistry(sid, id);
            PrivateRegistryImageSetting setting = ContainerRegistryMvpModel.getInstance()
                    .createImageSettingWithRegistry(registry);
            Map<String, String> query = buildQueryMap(isNextPage, tagStack, nextTag);
            Map<String, String> responseMap = ContainerExplorerMvpModel.getInstance().listTags(registry
                    .getLoginServerUrl(), setting.getUsername(), setting.getPassword(), repo, query);
            updatePaginationInfo(isNextPage, Type.TAG, responseMap.get(HEADER_LINK));
            Gson gson = new Gson();
            Tag tag = gson.fromJson(responseMap.get(BODY), Tag.class);
            return tag.getTags();
        })
                .subscribeOn(getSchedulerProvider().io())
                .subscribe(tags -> AzureTaskManager.getInstance().runLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().listTag(tags);
                }), e -> errorHandler(CANNOT_GET_TAGS, (Exception) e));
    }

    public boolean hasNextRepoPage() {
        return nextRepo != null;
    }

    public boolean hasNextTagPage() {
        return nextTag != null;
    }

    public boolean hasPreviousRepoPage() {
        return repoStack.size() != 0;
    }

    public boolean hasPreviousTagPage() {
        return tagStack.size() != 0;
    }

    private void resetRepoStack() {
        repoStack.clear();
        currentRepo = null;
        nextRepo = "";
    }

    private void resetTagStack() {
        tagStack.clear();
        currentTag = null;
        nextTag = "";
    }

    private ContainerRegistryProperty getProperty(ContainerRegistry registry, String sid) {
        String userName = "";
        String password = "";
        String password2 = "";
        if (registry.isAdminUserEnabled()) {
            userName = registry.getUserName();
            password = registry.getPrimaryCredential();
            password2 = registry.getSecondaryCredential();
        }
        final String region = Optional.ofNullable(registry.getRegion()).map(Region::getLabel).orElse(null);
        return new ContainerRegistryProperty(registry.getId(), registry.getName(), registry.getType(),
                registry.getResourceGroupName(), region, sid, registry.getLoginServerUrl(),
                registry.isAdminUserEnabled(), userName, password, password2);
    }

    private boolean isSubscriptionIdAndResourceIdInValid(String sid, String id) {
        if (StringUtils.isEmpty(sid)) {
            getMvpView().onError(CANNOT_GET_SUBSCRIPTION_ID);
            return true;
        }
        if (StringUtils.isEmpty(id)) {
            getMvpView().onError(CANNOT_GET_REGISTRY_ID);
            return true;
        }
        return false;
    }

    private Map<String, String> buildQueryMap(boolean isNextPage, @Nonnull Stack<String> stack, @Nullable String next) {
        Map<String, String> query = new HashMap<>();
        query.put(KEY_PAGE_SIZE, PAGE_SIZE);
        if (isNextPage) {
            if (next != null) {
                query.put(KEY_LAST, next);
            }
        } else {
            if (stack.size() > 0) {
                query.put(KEY_LAST, stack.peek());
            }
        }
        return query;
    }

    private void updatePaginationInfo(boolean isNextPage, @Nonnull Type type, @Nullable String linkHeader) {
        if (isNextPage) {
            switch (type) {
                case REPO:
                    if (this.currentRepo != null) {
                        repoStack.push(this.currentRepo);
                    }
                    if (this.nextRepo != null) {
                        this.currentRepo = this.nextRepo;
                    }
                    this.nextRepo = linkHeader == null ? null : parseLinkHeader(linkHeader);
                    break;
                case TAG:
                    if (this.currentTag != null) {
                        tagStack.push(this.currentTag);
                    }
                    if (this.nextTag != null) {
                        this.currentTag = this.nextTag;
                    }
                    this.nextTag = linkHeader == null ? null : parseLinkHeader(linkHeader);
                    break;
                default:
                    break;
            }
        } else {
            switch (type) {
                case REPO:
                    if (repoStack.size() > 0) {
                        this.nextRepo = this.currentRepo;
                        this.currentRepo = repoStack.pop();
                    }
                    break;
                case TAG:
                    if (tagStack.size() > 0) {
                        this.nextTag = this.currentTag;
                        this.currentTag = tagStack.pop();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Nullable
    private String parseLinkHeader(@Nonnull String header) {
        int start = header.indexOf("<") + 1;
        int end = header.lastIndexOf(">");
        if (start <= 0 || end < 0 || end >= header.length() || start >= end) {
            return null;
        }
        HttpUrl url = HttpUrl.parse(FAKE_URL + header.substring(start, end));
        if (url == null) {
            return null;
        }
        return url.queryParameter(KEY_LAST);
    }

    private void errorHandler(String msg, Exception e) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(msg, e);
        });
    }

    private enum Type {
        REPO,
        TAG
    }
}
