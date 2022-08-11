package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;

import javax.annotation.Nonnull;

public interface ConnectUsingSshAction {
    public void connectBySsh(VirtualMachine vm, @Nonnull Project project);
}
