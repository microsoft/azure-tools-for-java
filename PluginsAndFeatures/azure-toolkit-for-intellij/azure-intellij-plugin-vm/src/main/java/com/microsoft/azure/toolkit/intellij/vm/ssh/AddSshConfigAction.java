/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.remote.AuthType;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.ConstructorUtils;
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
@Slf4j
public class AddSshConfigAction {

    public static void addSshConfig(VirtualMachine vm, @Nonnull Project project) {
        if (!vm.isSshEnabled()) {
            AzureMessager.getMessager().warning(String.format("ssh is not enabled on '%s'", vm.getName()));
        }
        try {
            final Class<?> clsConfigurable = Class.forName("com.intellij.ssh.ui.unified.SshConfigConfigurable$Main");
            final Configurable configurable = (Configurable) ConstructorUtils.invokeConstructor(clsConfigurable, project);
            AzureTaskManager.getInstance().runLater(() -> openConfigDialog(vm, project, configurable));
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            AzureMessager.getMessager().error(e);
        }
    }

    @AzureOperation(name = "vm.open_ssh_config_dialog", params = "vm.name()", type = AzureOperation.Type.TASK)
    private static void openConfigDialog(VirtualMachine vm, @Nonnull Project project, Configurable configurable) {
        ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
            try {
                final String name = String.format("Azure: %s", vm.name());
                final Class<?> clsSshUiData = Class.forName("com.intellij.ssh.ui.unified.SshUiData");
                final Class<?> clsSshConfigManager = Class.forName("com.intellij.ssh.config.unified.SshConfigManager");
                final Class<?> clsConfigsData = Class.forName("com.intellij.ssh.config.unified.SshConfigManager$ConfigsData");
                final Object uiData = ConstructorUtils.invokeConstructor(clsSshUiData, toSshConfig(vm, name), true);
                final Object manager = MethodUtils.invokeStaticMethod(clsSshConfigManager, "getInstance", project);
                final Object existingConfigs = MethodUtils.invokeMethod(manager, "findConfigByName", name);
                final Object newConfigs = ConstructorUtils.invokeConstructor(clsConfigsData, Collections.emptyList(), Collections.singletonList(uiData));
                if (Objects.isNull(existingConfigs)) {
                    final Object savedAndCurrentData = MethodUtils.invokeMethod(manager, "getLastSavedAndCurrentData");
                    final Object merged = MethodUtils.invokeMethod(savedAndCurrentData, "createMerged", newConfigs);
                    MethodUtils.invokeMethod(configurable, true, "resetFromData", merged);
                }
                MethodUtils.invokeMethod(configurable, true, "select", uiData);
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | InstantiationException e) {
                AzureMessager.getMessager().error(e);
            }
        });
    }

    @Nonnull
    private static Object toSshConfig(VirtualMachine vm, String name)
        throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        final Class<?> clsSshConfig = Class.forName("com.intellij.ssh.config.unified.SshConfig");
        final Object config = ConstructorUtils.invokeConstructor(clsSshConfig, true);
        MethodUtils.invokeMethod(config, "setCustomName", name);
        MethodUtils.invokeMethod(config, "setId", UUID.nameUUIDFromBytes(name.getBytes()).toString());
        MethodUtils.invokeMethod(config, "setUsername", vm.getAdminUserName());
        MethodUtils.invokeMethod(config, "setAuthType", vm.isPasswordAuthenticationDisabled() ? AuthType.KEY_PAIR : AuthType.PASSWORD);
        MethodUtils.invokeMethod(config, "setHost", vm.getHostIp());
        return config;
    }
}
