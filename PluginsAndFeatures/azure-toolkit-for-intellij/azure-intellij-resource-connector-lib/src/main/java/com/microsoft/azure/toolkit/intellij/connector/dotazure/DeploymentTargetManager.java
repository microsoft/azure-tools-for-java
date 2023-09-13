/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.dotazure;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule.TARGETS_FILE;

@Slf4j
public class DeploymentTargetManager {
    private static final String ELEMENT_NAME_APPS = "targetApps";
    private static final String ELEMENT_NAME_APP = "targetApp";
    public static final String ATTR_ID = "id";
    private final Set<String> targetAppIds = new LinkedHashSet<>();
    @Getter
    private final Profile profile;

    public DeploymentTargetManager(@Nonnull final Profile profile) {
        this.profile = profile;
        this.reload();
    }

    public void reload() {
        try {
            this.load();
        } catch (final Exception e) {
            final Throwable root = ExceptionUtils.getRootCause(e);
            if (!(root instanceof ProcessCanceledException)) {
                throw new AzureToolkitRuntimeException(e);
            }
        }
    }

    @AzureOperation("internal/connector.add_target_app")
    public synchronized void addTarget(String id) {
        targetAppIds.add(id);
    }

    @AzureOperation("internal/connector.remove_target_app")
    public synchronized void removeTarget(String id) {
        targetAppIds.remove(id);
    }

    public List<String> getTargets() {
        return this.targetAppIds.stream().collect(Collectors.toList());
    }

    @Nullable
    public VirtualFile getTargetsFile() {
        return this.profile.getProfileDir().findChild(TARGETS_FILE);
    }

    @ExceptionNotification
    @AzureOperation("boundary/connector.save_target_apps")
    synchronized void save() throws IOException {
        final Element appsEle = new Element(ELEMENT_NAME_APPS);
        this.targetAppIds.stream().map(id -> new Element(ELEMENT_NAME_APP).setAttribute("id", id)).forEach(appsEle::addContent);
        final VirtualFile appsFile = this.profile.getProfileDir().findOrCreateChildData(this, TARGETS_FILE);
        JDOMUtil.write(appsEle, appsFile.toNioPath());
    }

    @ExceptionNotification
    @AzureOperation(name = "boundary/connector.load_target_apps")
    synchronized void load() throws Exception {
        final VirtualFile appsFile = this.profile.getProfileDir().findChild(TARGETS_FILE);
        if (Objects.isNull(appsFile) || appsFile.contentsToByteArray().length < 1) {
            return;
        }
        this.targetAppIds.clear();
        final Element appsEle = JDOMUtil.load(appsFile.toNioPath());
        appsEle.getChildren().stream().map(e -> e.getAttributeValue(ATTR_ID)).forEach(this::addTarget);
    }
}
