/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.remote.AuthType;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.config.unified.SshConfigManager;
import com.intellij.ssh.config.unified.SshConfigManager.ConfigsData;
import com.intellij.ssh.ui.unified.SshConfigConfigurable;
import com.intellij.ssh.ui.unified.SshUiData;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.lang3.reflect.MethodUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * open "SSH Configurations" dialog in Intellij Ultimate.
 * <p>NOTE: this class is implemented via reflection to avoid explicit dependency on Intellij Ultimate </o>
 */
public class AddSshConfigAction {

    public static void addSshConfig(VirtualMachine vm, @Nonnull Project project) {
        if (!vm.isSshEnabled()) {
            AzureMessager.getMessager().warning(String.format("SSH is not enabled on '%s'.", vm.getName()));
            return;
        }
        final SshConfigConfigurable configurable = new SshConfigConfigurable.Main(project);
        AzureTaskManager.getInstance().runLater(() -> openConfigDialog(vm, project, configurable));
    }

    @Nonnull
    public static SshConfig toSshConfig(VirtualMachine vm) {
        final String customName = String.format("Azure: %s", vm.getName());
        final SshConfig config = new SshConfig(true);
        config.setCustomName(customName);
        config.setId(UUID.nameUUIDFromBytes(customName.getBytes()).toString());
        config.setUsername(vm.getAdminUserName());
        config.setAuthType(vm.isPasswordAuthenticationDisabled() ? AuthType.KEY_PAIR : AuthType.PASSWORD);
        config.setHost(vm.getHostIp());
        config.setPort(vm.getSshPort());
        return config;
    }

    @Nonnull
    public static SshConfig getOrCreateSshConfig(VirtualMachine vm, @Nonnull Project project) {
        final String sshConfigName = String.format("Azure: %s", vm.getName());
        final SshConfigManager manager = SshConfigManager.getInstance(project);
        SshConfig result = manager.findConfigByName(sshConfigName);
        if (Objects.isNull(result)) {
            result = toSshConfig(vm);
            final SshUiData uiData = new SshUiData(result, true);
            final SshConfigManager.ConfigsData newConfigs = new SshConfigManager.ConfigsData(Collections.emptyList(), Collections.singletonList(uiData));
            final SshConfigManager.ConfigsData merged = manager.getLastSavedAndCurrentData().createMerged(newConfigs);
            manager.applyData(merged.getCurrentData(), null);
        }
        return result;
    }

    @AzureOperation(name = "user/vm.open_ssh_config_dialog", params = "vm.name()")
    private static void openConfigDialog(VirtualMachine vm, @Nonnull Project project, Configurable configurable) {
        ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
            final String name = String.format("Azure: %s", vm.getName());
            final SshUiData uiData = new SshUiData(toSshConfig(vm), true);
            final SshConfigManager manager = SshConfigManager.getInstance(project);
            final SshConfig existingConfigs = manager.findConfigByName(name);
            final ConfigsData newConfigs = new ConfigsData(Collections.emptyList(), Collections.singletonList(uiData));
            try {
                if (Objects.isNull(existingConfigs)) {
                    final ConfigsData savedAndCurrentData = manager.getLastSavedAndCurrentData();
                    final ConfigsData merged = savedAndCurrentData.createMerged(newConfigs);
                    MethodUtils.invokeMethod(configurable, true, "resetFromData", merged);
                }
                MethodUtils.invokeMethod(configurable, true, "select", uiData);
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                AzureMessager.getMessager().error(e);
            }
        });
    }

}
