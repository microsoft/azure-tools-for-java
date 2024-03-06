package com.microsoft.azure.toolkit.intellij.vm.runtarget;

import com.intellij.execution.Platform;
import com.intellij.execution.target.*;
import com.intellij.ide.wizard.AbstractWizardStepEx;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.ui.unified.SshUiData;
import com.jetbrains.plugins.remotesdk.RemoteSdkBundle;
import com.jetbrains.plugins.remotesdk.target.ssh.target.SshRemoteEnvironmentRequest;
import com.jetbrains.plugins.remotesdk.target.ssh.target.SshTargetConfigurable;
import com.jetbrains.plugins.remotesdk.target.ssh.target.SshTargetEnvironmentConfiguration;
import com.jetbrains.plugins.remotesdk.target.ssh.target.SshTargetType;
import com.jetbrains.plugins.remotesdk.target.ssh.target.wizard.*;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
public class AzureVmTargetType extends TargetEnvironmentType<AzureVmTargetEnvironmentConfiguration> implements BrowsableTargetEnvironmentType {
    public static final String TYPE_ID = "Microsoft.Compute/virtualMachines";
    public static final String DISPLAY_NAME = "Azure Virtual Machine";
    @Nls
    @Nonnull
    private final String displayName = DISPLAY_NAME;
    @Nonnull
    private final Icon icon = IntelliJAzureIcons.getIcon(AzureIcons.VirtualMachine.MODULE);
    private final SshTargetType originType;

    public AzureVmTargetType() {
        super(TYPE_ID);
        this.originType = new SshTargetType();
    }

    @Nullable
    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/vm.open_create_run_target_dialog")
    @SuppressWarnings("KotlinInternalInJava")
    public List<AbstractWizardStepEx> createStepsForNewWizard(@Nonnull Project project, @Nonnull AzureVmTargetEnvironmentConfiguration config, @Nullable LanguageRuntimeType<?> runtimeType) {
        final boolean isCustomToolConfiguration = runtimeType instanceof CustomToolLanguageRuntimeType;
        final ConnectionData data = new ConnectionData(false, null, "", 22, "", "", true, "", "", true, ConnectionData.OpenSshAgentConnectionState.NOT_STARTED);
        final SshTargetWizardModel model = new SshTargetWizardModel(project, config, data);
        model.setLanguageType(runtimeType);
        model.setCustomToolConfiguration(isCustomToolConfiguration);
        final ArrayList<AbstractWizardStepEx> steps = new ArrayList<>(4);
        steps.add(new AzureVmTargetConnectionStep(model, steps));
        steps.add(new SshTargetAuthStep(model));
        steps.add(new SshTargetIntrospectionStep(model));
        steps.add(isCustomToolConfiguration ? new SshConfigureCustomToolStep(model) : new SshTargetLanguageStep(model));
        return steps;
    }

    @Nonnull
    @Override
    public PersistentStateComponent<?> createSerializer(@Nonnull AzureVmTargetEnvironmentConfiguration config) {
        return config;
    }

    @Nonnull
    @Override
    public AzureVmTargetEnvironmentConfiguration createDefaultConfig() {
        return new AzureVmTargetEnvironmentConfiguration();
    }

    @Override
    public boolean providesNewWizard(@Nonnull Project project, @Nullable LanguageRuntimeType<?> runtimeType) {
        return true;
    }


    @Nonnull
    @Override
    public AzureVmTargetEnvironmentConfiguration duplicateConfig(@Nonnull AzureVmTargetEnvironmentConfiguration config) {
        return duplicateTargetConfiguration(this, config);
    }

    @Nonnull
    @Override
    public Configurable createConfigurable(@Nonnull Project project, @Nonnull AzureVmTargetEnvironmentConfiguration config, @Nullable LanguageRuntimeType<?> languageRuntimeType, @Nullable Configurable configurable) {
        return new SshTargetConfigurable(project, config);
    }

    @Nonnull
    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/vm.create_environment_request")
    @SuppressWarnings("UnstableApiUsage")
    public TargetEnvironmentRequest createEnvironmentRequest(@Nonnull Project project, @Nonnull AzureVmTargetEnvironmentConfiguration config) {
        return new SshRemoteEnvironmentRequest(project, config, new TargetPlatform(Platform.UNIX));
    }

    @Override
    public @NotNull <T extends Component> ActionListener createBrowser(@NotNull Project project, @NlsContexts.DialogTitle String title, @NotNull TextComponentAccessor<T> textComponentAccessor, @NotNull T component, @NotNull Supplier<? extends TargetEnvironmentConfiguration> configurationSupplier) {
        return new ActionListener() {
            public final void actionPerformed(ActionEvent it) {
                final TargetEnvironmentConfiguration configuration = configurationSupplier.get();
                if (configuration instanceof SshTargetEnvironmentConfiguration) {
                    final SshConfig sshConfig = ((SshTargetEnvironmentConfiguration) configuration).findSshConfig(project);
                    final SshUiData uiData = sshConfig != null ? new SshUiData(sshConfig, true) : null;
                    SshTargetType.Companion.handleBrowsing$intellij_remoteRun(uiData, project, title, component, textComponentAccessor);
                } else {
                    Messages.showWarningDialog(component, RemoteSdkBundle.message("dialog.message.got.unexpected.settings.for.browsing", new Object[0]), title);
                }
            }
        };
    }
}
