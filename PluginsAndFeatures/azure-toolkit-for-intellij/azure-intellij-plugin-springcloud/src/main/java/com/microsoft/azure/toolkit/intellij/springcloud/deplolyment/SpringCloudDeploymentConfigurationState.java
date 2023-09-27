/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.springcloud.SpringCloudActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.intellij.common.runconfig.RunConfigurationUtils;
import com.microsoft.azure.toolkit.intellij.common.utils.JdkUtils;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import com.microsoft.azure.toolkit.lib.springcloud.Utils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle.message;

public class SpringCloudDeploymentConfigurationState implements RunProfileState {
    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "The app is still starting, " +
        "you could start streaming log to check if something wrong in server side.";
    private static final String NOTIFICATION_TITLE = "Querying app status";
    private static final String DEPLOYMENT_SUCCEED = "Deployment was successful but the app may still be starting.";

    private final SpringCloudDeploymentConfiguration config;
    private final Project project;

    public SpringCloudDeploymentConfigurationState(Project project, SpringCloudDeploymentConfiguration configuration) {
        this.config = configuration;
        this.project = project;
    }

    @Override
    @ExceptionNotification
    public @Nullable ExecutionResult execute(Executor executor, @Nonnull ProgramRunner<?> runner) {
        final Action<Void> retry = Action.retryFromFailure(() -> this.execute(executor, runner));
        final RunProcessHandler processHandler = new RunProcessHandler();
        processHandler.addDefaultListener();
        processHandler.startNotify();
        final ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        final ConsoleMessager messager = new ConsoleMessager(consoleView);
        consoleView.attachToProcess(processHandler);
        final Runnable execute = () -> {
            try {
                final SpringCloudDeployment springCloudDeployment = this.execute(messager);
                messager.info(DEPLOYMENT_SUCCEED);
                processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_RESULT, true);
                processHandler.notifyComplete();
                waitUntilAppReady(springCloudDeployment);
            } catch (final Exception e) {
                final Action<?> action = getOpenStreamingLogAction(config.getDeployment());
                if (Objects.nonNull(action)) {
                    messager.error(e, "Azure", retry, action);
                } else {
                    messager.error(e, "Azure", retry);
                }
                processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_RESULT, false);
                processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_EXCEPTION, e);
                processHandler.notifyProcessTerminated(-1);
            }
        };
        final Disposable subscribe = Mono.fromRunnable(execute)
            .doOnTerminate(processHandler::notifyComplete)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@Nonnull ProcessEvent event) {
                subscribe.dispose();
            }
        });

        return new DefaultExecutionResult(consoleView, processHandler);
    }

    @AzureOperation(name = "user/springcloud.deploy_app.app", params = {"this.config.getApp().getName()"}, source = "this.config.getDeployment()")
    public SpringCloudDeployment execute(IAzureMessager messager) {
        OperationContext.current().setMessager(messager);
        OperationContext.current().setTelemetryProperties(getTelemetryProperties());
        final SpringCloudDeploymentDraft deployment = this.config.getDeployment();
        final Optional<File> opFile = Optional.ofNullable(deployment).map(SpringCloudDeploymentDraft::getArtifact).map(IArtifact::getFile);
        final Action.Id<Void> REOPEN = Action.Id.of("user/springcloud.reopen_deploy_dialog");
        final Action<Void> reopen = new Action<>(REOPEN).withHandler((v) -> DeploySpringCloudAppAction.deploy(this.config, this.project));
        if (opFile.isEmpty() || opFile.filter(File::exists).isEmpty()) {
            throw new AzureToolkitRuntimeException(
                message("springcloud.deploy_app.no_artifact").toString(),
                message("springcloud.deploy_app.no_artifact.tips").toString(),
                reopen.withLabel("Add BeforeRunTask"));
        }
        final SpringCloudApp app = deployment.getParent();
        final SpringCloudCluster cluster = app.getParent();
        if (!Optional.of(cluster).map(SpringCloudCluster::isEnterpriseTier).orElse(true)) {
            final Integer appVersion = Optional.ofNullable(deployment.getRuntimeVersion())
                .map(v -> v.split("\\s|_")[1]).map(Integer::parseInt)
                .orElseThrow(() -> new AzureToolkitRuntimeException("Invalid runtime version: " + deployment.getRuntimeVersion()));
            final Integer artifactVersion = JdkUtils.getBytecodeLanguageLevel(opFile.get());
            if (Objects.nonNull(artifactVersion) && artifactVersion > appVersion) {
                final AzureString message = AzureString.format(
                    "The bytecode version of artifact (%s) is \"%s (%s)\", " +
                        "which is incompatible with the runtime \"%s\" of the target app (%s). " +
                        "This will cause the App to fail to start normally after deploying. Please consider rebuilding the artifact or selecting another app.",
                    opFile.get().getName(), artifactVersion + 44, "Java " + artifactVersion, "Java " + appVersion, app.getName());
                throw new AzureToolkitRuntimeException(message.toString(), reopen.withLabel("Reopen Deploy Dialog"));
            }
        }
        final Map<String, String> environmentVariables = Optional.ofNullable(deployment.getEnvironmentVariables()).orElseGet(HashMap::new);
        environmentVariables.putAll(this.config.getEnvironmentVariables());
        deployment.setEnvironmentVariables(environmentVariables);
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runOnPooledThread(() -> opFile.map(f -> this.config.getModule())
            .map(AzureModule::from)
            .ifPresent(module -> tm.runLater(() -> tm.write(() -> module
                .initializeWithDefaultProfileIfNot()
                .addApp(app).save()))));
        try {
            deployment.commit();
        } catch (final Exception e) {
            app.refresh();
            Optional.ofNullable(app.getActiveDeployment()).ifPresent(d -> d.startStreamingLog(true));
            throw new AzureToolkitRuntimeException(e);
        }
        app.refresh();
        printPublicUrl(app);
        return deployment;
    }

    private void printPublicUrl(final SpringCloudApp app) {
        final IAzureMessager messager = AzureMessager.getMessager();
        if (!app.isPublicEndpointEnabled()) {
            return;
        }
        messager.info(String.format("Getting public url of app(%s)...", app.getName()));
        String publicUrl = app.getApplicationUrl();
        if (StringUtils.isEmpty(publicUrl)) {
            publicUrl = Utils.pollUntil(() -> {
                app.refresh();
                return app.getApplicationUrl();
            }, StringUtils::isNotBlank, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            messager.warning("Failed to get application url");
        } else {
            messager.info(String.format("Application url: %s", publicUrl));
        }
    }

    @Nullable
    private Action<?> getOpenStreamingLogAction(@Nullable SpringCloudDeployment deployment) {
        try {
            final SpringCloudAppInstance appInstance = Optional.ofNullable(deployment).map(SpringCloudDeployment::getLatestInstance).orElse(null);
            if (Objects.isNull(appInstance)) {
                return Optional.ofNullable(deployment)
                    .map(d -> AzureActionManager.getInstance().getAction(SpringCloudActionsContributor.STREAM_LOG_APP).bind(d.getParent()))
                    .orElse(null);
            }
            return AzureActionManager.getInstance().getAction(SpringCloudActionsContributor.STREAM_LOG).bind(appInstance);
        } catch (Throwable e) {
            return null;
        }
    }

    private void waitUntilAppReady(SpringCloudDeployment springCloudDeployment) {
        AzureTaskManager.getInstance().runInBackground(NOTIFICATION_TITLE, () -> {
            final SpringCloudApp app = springCloudDeployment.getParent();
            final IAzureMessager messager = AzureMessager.getMessager();
            if (!springCloudDeployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
                messager.warning(GET_DEPLOYMENT_STATUS_TIMEOUT, null, getOpenStreamingLogAction(springCloudDeployment));
            } else {
                messager.success(AzureString.format("App({0}) started successfully", app.getName()), null,
                    AzureActionManager.getInstance().getAction(SpringCloudActionsContributor.OPEN_PUBLIC_URL).bind(app),
                    AzureActionManager.getInstance().getAction(SpringCloudActionsContributor.OPEN_TEST_URL).bind(app));
            }
        });
    }

    protected Map<String, String> getTelemetryProperties() {
        final Map<String, String> props = new HashMap<>();
        final SpringCloudDeploymentDraft deployment = Objects.requireNonNull(config.getDeployment());
        props.put("runtime", String.valueOf(deployment.getRuntimeVersion()));
        props.put("subscriptionId", deployment.getSubscriptionId());
        props.put("public", String.valueOf(deployment.getParent().isPublicEndpointEnabled()));
        props.put("jvmOptions", String.valueOf(StringUtils.isNotEmpty(deployment.getJvmOptions())));
        props.put("instanceCount", String.valueOf(deployment.getCapacity()));
        props.put("memory", String.valueOf(deployment.getMemoryInGB()));
        props.put("cpu", String.valueOf(deployment.getCpu()));
        props.put("persistentStorage", String.valueOf(deployment.getParent().isPublicEndpointEnabled()));
        return props;
    }

    @RequiredArgsConstructor
    private static class ConsoleMessager extends IntellijAzureMessager {
        private final ConsoleView consoleView;

        @Override
        public boolean show(IAzureMessage msg) {
            final IAzureMessage raw = msg.getRawMessage();
            if (raw.getType() == IAzureMessage.Type.INFO) {
                println(raw.getContent(), ConsoleViewContentType.NORMAL_OUTPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.SUCCESS) {
                println(raw.getContent(), ConsoleViewContentType.NORMAL_OUTPUT);
            } else if (raw.getType() == IAzureMessage.Type.DEBUG) {
                println(raw.getContent(), ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.WARNING) {
                println(raw.getContent(), ConsoleViewContentType.LOG_WARNING_OUTPUT);
            } else if (raw.getType() == IAzureMessage.Type.ERROR) {
                println(raw.getContent(), ConsoleViewContentType.ERROR_OUTPUT);
            }
            return super.show(msg);
        }

        private void println(String originText, ConsoleViewContentType type) {
            consoleView.print(originText + System.lineSeparator(), type);
        }
    }
}
