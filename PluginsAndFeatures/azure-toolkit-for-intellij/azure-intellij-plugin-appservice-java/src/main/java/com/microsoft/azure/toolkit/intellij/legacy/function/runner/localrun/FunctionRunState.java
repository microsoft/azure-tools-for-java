/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.remote.RemoteConfigurationType;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.PsiMethod;
import com.intellij.util.EnvironmentUtil;
import com.microsoft.azure.toolkit.intellij.common.ReadStreamLineThread;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger;
import com.microsoft.azure.toolkit.intellij.common.help.AzureWebHelpProvider;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DotEnvBeforeRunTaskProvider;
import com.microsoft.azure.toolkit.intellij.function.components.connection.FunctionConnectionCreationDialog;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTableUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.storage.azurite.AzuriteService;
import com.microsoft.azure.toolkit.intellij.storage.azurite.AzuriteTaskProvider;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor.CONFIG_CORE_TOOLS;
import static com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor.DOWNLOAD_CORE_TOOLS;
import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTable.AZURE_WEB_JOB_STORAGE_KEY;

@Slf4j
public class FunctionRunState extends AzureRunProfileState<Boolean> {

    private static final int DEFAULT_FUNC_PORT = 7071;
    private static final int DEFAULT_DEBUG_PORT = 5005;
    private static final String DEBUG_PARAMETERS =
            "--language-worker -- \"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%s\"";
    private static final String HOST_JSON = "host.json";
    private static final String EXTENSION_BUNDLE = "extensionBundle";
    private static final String EXTENSION_BUNDLE_ID = "Microsoft.Azure.Functions.ExtensionBundle";
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("version \"(.*)\"");
    private static final Pattern PORT_EXCEPTION_PATTERN = Pattern.compile("Port \\d+ is unavailable");
    private static final ComparableVersion JAVA_9 = new ComparableVersion("9");
    private static final ComparableVersion FUNC_3 = new ComparableVersion("3");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION = new ComparableVersion("3.0.2630");
    private static final ComparableVersion MINIMUM_JAVA_9_SUPPORTED_VERSION_V2 = new ComparableVersion("2.7.2628");
    private static final BindingEnum[] FUNCTION_WITHOUT_FUNCTION_EXTENSION = {BindingEnum.HttpOutput, BindingEnum.HttpTrigger};
    private static final List<String> AZURE_WEB_JOBS_STORAGE_NOT_REQUIRED_TRIGGERS = Arrays.asList("httptrigger", "kafkatrigger", "rabbitmqtrigger",
            "activityTrigger", "entityTrigger");
    private static final String CONNECTION_TITLE = "AzureWebJobsStorage is missing";
    private static final String CONNECTION_DESCRIPTION = "Please set the resource connection for AzureWebJobsStorage.";
    private boolean isDebuggerLaunched;
    private File stagingFolder;
    private Process installProcess;
    private Process process;
    private final Executor executor;
    private final FunctionRunConfiguration functionRunConfiguration;
    @Getter
    private final List<Connection<?, ?>> connections = new ArrayList<>();

    public FunctionRunState(@NotNull Project project, FunctionRunConfiguration functionRunConfiguration, Executor executor) {
        super(project);
        this.executor = executor;
        this.functionRunConfiguration = functionRunConfiguration;
    }

    @AzureOperation(name = "boundary/function.launch_debugger")
    private void launchDebugger(final Project project, int debugPort) {
        final Runnable runnable = () -> {
            final RunManagerImpl manager = new RunManagerImpl(project);
            final RemoteConfiguration remoteConfig = (RemoteConfiguration) RemoteConfigurationType.getInstance().createTemplateConfiguration(project);
            remoteConfig.PORT = String.valueOf(debugPort);
            remoteConfig.HOST = "localhost";
            remoteConfig.USE_SOCKET_TRANSPORT = true;
            remoteConfig.SERVER_MODE = false;
            remoteConfig.setName("azure functions");
            final RunnerAndConfigurationSettings configuration = new RunnerAndConfigurationSettingsImpl(manager, remoteConfig, false);
            manager.setTemporaryConfiguration(configuration);
            Optional.ofNullable(ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG))
                            .ifPresent(executor -> ExecutionUtil.runConfiguration(configuration, executor));
        };
        AzureTaskManager.getInstance().runAndWait(runnable, AzureTask.Modality.ANY);
    }

    @Override
    @AzureOperation(name = "user/function.run_app")
    protected Boolean executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) throws Exception {
        // Prepare staging Folder
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        validateFunctionRuntime();
        stagingFolder = FunctionUtils.getTempStagingFolder();
        addProcessTerminatedListener(processHandler);
        prepareStagingFolder(stagingFolder, processHandler, operation);
        // Run Function Host
        runFunctionCli(processHandler, stagingFolder);
        return true;
    }

    private void applyResourceConnection(Map<String, String> appSettings) {
        if (functionRunConfiguration.isConnectionEnabled()) {
            final Set<Connection<?, ?>> identityConnection = functionRunConfiguration.getConnections().stream()
                    .filter(Connection::isManagedIdentityConnection)
                    .collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(identityConnection)) {
                final String resources = identityConnection.stream().map(Connection::getResource).map(Resource::getName).collect(Collectors.joining(", "));
                AzureMessager.getMessager().warning(String.format("Managed Identity connection is not supported for function app, your connections connected to %s may not work as expected.", resources));
            }
            final DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask loadDotEnvBeforeRunTask = functionRunConfiguration.getLoadDotEnvBeforeRunTask();
            loadDotEnvBeforeRunTask.loadEnv().forEach(env -> appSettings.put(env.getKey(), env.getValue()));
        }
    }

    private List<Connection<?, ?>> getConnections() {
        return Optional.ofNullable(functionRunConfiguration).map(FunctionRunConfiguration::getConnections).orElse(Collections.emptyList());
    }

    @AzureOperation(name = "internal/function.validate_runtime")
    private void validateFunctionRuntime() {
        final ComparableVersion funcVersion = getFuncVersion();
        final ComparableVersion javaVersion = getJavaVersion();
        if (funcVersion == null || javaVersion == null) {
            AzureMessager.getMessager().warning(message("function.skip_local_run_validation"));
            return;
        }
        if (javaVersion.compareTo(JAVA_9) < 0) {
            // No need validate function host version within java 8 or earlier
            return;
        }
        final ComparableVersion minimumVersion = funcVersion.compareTo(FUNC_3) >= 0 ? MINIMUM_JAVA_9_SUPPORTED_VERSION : MINIMUM_JAVA_9_SUPPORTED_VERSION_V2;
        if (funcVersion.compareTo(minimumVersion) < 0) {
            throw new AzureToolkitRuntimeException(message("function.run.error.funcOutOfDate"),
                    message("function.run.error.funcOutOfDate.tips"), DOWNLOAD_CORE_TOOLS, CONFIG_CORE_TOOLS);
        }
    }

    @Nullable
    @AzureOperation(name = "boundary/function.get_version.func", params = {"this.functionRunConfiguration.getFuncPath()"})
    private ComparableVersion getFuncVersion() {
        File funcFile = Optional.ofNullable(functionRunConfiguration.getFuncPath()).filter(StringUtils::isNotBlank).map(File::new).orElse(null);
        if (funcFile == null || !funcFile.exists()) {
            final File settingsFuncFile = Optional.ofNullable(Azure.az().config().getFunctionCoreToolsPath()).filter(StringUtils::isNotBlank).map(File::new).orElse(null);
            if (settingsFuncFile != null && settingsFuncFile.exists()) {
                funcFile = settingsFuncFile;
                functionRunConfiguration.setFuncPath(funcFile.getAbsolutePath());
                final AzureString message = AzureString.format("the configured Function Core Tools for this Run Configuration is not found, use the default one \"{0}\" instead.", funcFile.getAbsolutePath());
                AzureMessager.getDefaultMessager().warning(message);
            } else {
                throw new AzureToolkitRuntimeException(message("function.run.error.runtimeNotFound"), DOWNLOAD_CORE_TOOLS, CONFIG_CORE_TOOLS);
            }
        }
        try {
            final String funcVersion = CommandUtils.exec(String.format("%s -v", funcFile.getName()), funcFile.getParent());
            return StringUtils.isEmpty(funcVersion) ? null : new ComparableVersion(funcVersion);
        } catch (final IOException e) {
            // swallow exception to get func version
            log.info("Failed to get version of Azure Functions Core Tools", e);
            return null;
        }
    }

    // Get java runtime version following the strategy of function core tools
    // Get java version of JAVA_HOME first, fall back to use PATH if JAVA_HOME not exists
    @Nullable
    @AzureOperation(name = "boundary/function.validate_jre")
    private static ComparableVersion getJavaVersion() {
        try {
            final String javaHome = System.getenv("JAVA_HOME");
            final String executeFolder = FileUtil.exists(javaHome) ? Paths.get(javaHome, "bin").toString() : null;
            final String javaVersion = CommandUtils.exec("java -version", executeFolder, true);
            if (StringUtils.isEmpty(javaVersion)) {
                return null;
            }
            final Matcher matcher = JAVA_VERSION_PATTERN.matcher(javaVersion);
            return matcher.find() ? new ComparableVersion(matcher.group(1)) : null;
        } catch (final Throwable e) {
            // swallow exception to get java version
            log.info("Failed to get java version", e);
            return null;
        }
    }

    @AzureOperation(name = "boundary/function.run_cli.folder", params = {"stagingFolder.getName()"})
    private int runFunctionCli(RunProcessHandler processHandler, File stagingFolder)
            throws InterruptedException, ExecutionException {
        isDebuggerLaunched = false;
        final int debugPort = FunctionUtils.findFreePort(DEFAULT_DEBUG_PORT);
        process = getFunctionCliProcess(stagingFolder, debugPort);
        // Redirect function cli output to console
        readInputStreamByLines(process.getInputStream(), inputLine -> {
            if (isDebugMode() && isFuncInitialized(inputLine) && !isDebuggerLaunched) {
                // launch debugger when func ready
                isDebuggerLaunched = true;
                launchDebugger(project, debugPort);
            }
            if (processHandler.isProcessRunning()) {
                processHandler.setText(inputLine);
            }
        });
        final String[] error = new String[1];
        readInputStreamByLines(process.getErrorStream(), inputLine -> {
            error[0] = inputLine;
            if (processHandler.isProcessRunning()) {
                processHandler.println(inputLine, ProcessOutputTypes.STDERR);
            }
        });
        // Pending for function cli
        try {
            final int result = process.waitFor();
            if (result != 0) {
                throw new AzureToolkitRuntimeException(error[0]);
            }
            return result;
        } catch (final InterruptedException i) {
            // swallow interrupted exception which is caused by user stop
            return 0;
        }
    }

    private boolean isFuncInitialized(String input) {
        return StringUtils.containsIgnoreCase(input, "Job host started") ||
                StringUtils.containsIgnoreCase(input, "Listening for transport dt_socket at address");
    }

    private void readInputStreamByLines(InputStream inputStream, Consumer<String> stringConsumer) {
        new ReadStreamLineThread(inputStream, stringConsumer).start();
    }

    private void addProcessTerminatedListener(RunProcessHandler processHandler) {
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                stopProcessIfAlive(process);
                stopProcessIfAlive(installProcess);
            }
        });
    }

    private Process getFunctionCliProcess(File stagingFolder, int debugPort) throws ExecutionException {
        final String funcPath = functionRunConfiguration.getFuncPath();
        final String funcArguments = Optional.ofNullable(functionRunConfiguration.getFunctionHostArguments())
                .filter(StringUtils::isNoneBlank).orElseGet(FunctionUtils::getDefaultFuncArguments);
        final String debugParameters = isDebugMode() ? String.format(DEBUG_PARAMETERS, debugPort) : null;
        final String[] command = Stream.of(funcArguments, debugParameters)
                .filter(StringUtils::isNoneBlank)
                .collect(Collectors.joining(StringUtils.SPACE))
                .split(StringUtils.SPACE);
        final GeneralCommandLine result = new GeneralCommandLine();
        result.withEnvironment(EnvironmentUtil.getEnvironmentMap());
        result.withExePath(funcPath);
        result.withParameters(command);
        result.withWorkDirectory(stagingFolder);
        return result.createProcess();
    }

    private Process getExtensionInstallProcess(File stagingFolder) throws ExecutionException {
        final String funcPath = functionRunConfiguration.getFuncPath();
        final GeneralCommandLine result = new GeneralCommandLine();
        result.withEnvironment(EnvironmentUtil.getEnvironmentMap());
        result.withExePath(funcPath);
        result.withParameters("extensions", "install", "--java");
        result.withWorkDirectory(stagingFolder);
        return result.createProcess();
    }

    @AzureOperation(name = "boundary/function.prepare_staging_folder.folder|app", params = {"stagingFolder.getName()", "this.functionRunConfiguration.getFuncPath()"})
    private void prepareStagingFolder(File stagingFolder,
                                      RunProcessHandler processHandler,
                                      final @NotNull Operation operation) throws Exception {
        final RunProcessHandlerMessenger messenger = new RunProcessHandlerMessenger(processHandler);
        OperationContext.current().setMessager(messenger);
        final Module module = functionRunConfiguration.getModule();
        if (module == null) {
            throw new AzureToolkitRuntimeException("Module was not valid in function run configuration.");
        }
        final Path hostJsonPath = Optional.ofNullable(functionRunConfiguration.getHostJsonPath())
                .filter(StringUtils::isNotEmpty).map(Paths::get)
                .orElseGet(() -> Paths.get(FunctionUtils.getDefaultHostJsonPath(module)));
        final PsiMethod[] methods = ReadAction.compute(() -> FunctionUtils.findFunctionsByAnnotation(module));
        final Path folder = stagingFolder.toPath();
        try {
            final Map<String, FunctionConfiguration> configMap =
                    FunctionUtils.prepareStagingFolder(folder, hostJsonPath, project, module, methods);
            final List<BindingEnum> functionBindingList = FunctionUtils.getFunctionBindingList(configMap);
            operation.trackProperty(TelemetryConstants.TRIGGER_TYPE, StringUtils.join(functionBindingList, ","));
            final Map<String, String> configurationAppSettings = FunctionUtils.loadAppSettingsFromSecurityStorage(functionRunConfiguration.getAppSettingsKey());
            final boolean useLocalSettings = MapUtils.isEmpty(configurationAppSettings);
            final Path localSettingsJson = Optional.ofNullable(functionRunConfiguration.getLocalSettingsJsonPath())
                    .filter(StringUtils::isNotEmpty).map(Paths::get)
                    .orElseGet(() -> Paths.get(FunctionUtils.getDefaultLocalSettingsJsonPath(module)));
            final Map<String, String> appSettings = useLocalSettings ?
                    FunctionAppSettingsTableUtils.getAppSettingsFromLocalSettingsJson(localSettingsJson.toFile()) : configurationAppSettings;
            // Do not copy local settings if user have already set it in configuration
            applyResourceConnection(appSettings);
            validateAppSettings(appSettings, functionBindingList);
            FunctionUtils.copyLocalSettingsToStagingFolder(folder, localSettingsJson, appSettings, useLocalSettings);

            final Set<BindingEnum> bindingClasses = getFunctionBindingEnums(configMap);
            if (isInstallingExtensionNeeded(bindingClasses, processHandler)) {
                installProcess = getExtensionInstallProcess(stagingFolder);
            }
        } catch (final AzureExecutionException | IOException e) {
            final String error = String.format("failed prepare staging folder[%s]", folder);
            throw new AzureToolkitRuntimeException(error, e);
        }
        if (Objects.isNull(installProcess)) {
            return;
        }
        readInputStreamByLines(installProcess.getErrorStream(), inputLine -> {
            if (processHandler.isProcessRunning()) {
                processHandler.println(inputLine, ProcessOutputTypes.STDERR);
            }
        });
        readInputStreamByLines(installProcess.getInputStream(), inputLine -> {
            if (processHandler.isProcessRunning()) {
                processHandler.setText(inputLine);
            }
        });
        try {
            final int exitCode = installProcess.waitFor();
            if (exitCode != 0) {
                throw new AzureExecutionException(message("function.run.error.installFuncFailed"));
            }
        } catch (final InterruptedException e) {
            // swallow interrupted exception which is caused by user cancel
        }
    }

    // todo: @hanli, check whether there are connections refered in function binding list but not defined in app settings
    private void validateAppSettings(@Nonnull final Map<String, String> appSettings, @Nonnull final List<BindingEnum> functionBindingList) {

        final String webJobStorage = appSettings.get(AZURE_WEB_JOB_STORAGE_KEY);
        if (StringUtils.isEmpty(webJobStorage) && isWebJobStorageRequired(functionBindingList)) {
            final Callable<Connection<?, ?>> connectionSupplier = () -> createMissingConnection(appSettings, AZURE_WEB_JOB_STORAGE_KEY, "Storage",
                    CONNECTION_DESCRIPTION, CONNECTION_TITLE, AzureWebHelpProvider.HELP_AZURE_WEB_JOBS_STORAGE);
            final Connection<?, ?> connection = AzureTaskManager.getInstance().runAndWait("Add Missing Connection", connectionSupplier).join();
            Optional.ofNullable(connection).ifPresent(c -> saveConnection(c, appSettings));
        }
    }

    @AzureOperation(
            name = "internal/function.add_missing_connection.name",
            params = {"connectionName"}
    )
    @Nullable
    private Connection<?, ?> createMissingConnection(@Nonnull final Map<String, String> appSettings, @Nonnull final String connectionName, @Nonnull final String resourceType
            , @Nonnull final String description, @Nonnull final String title, @Nullable final String connectionHelpTopic) {
        final FunctionConnectionCreationDialog dialog = new FunctionConnectionCreationDialog(project, functionRunConfiguration.getModule(), resourceType, connectionHelpTopic);
        dialog.setTitle(title);
        dialog.setFixedConnectionName(connectionName);
        dialog.setDescription(description);
        dialog.setOKActionText("Connect");
        return dialog.showAndGet() ? dialog.getConnection() : null;
    }

    @AzureOperation(
            name = "user/function.add_missing_connection.consumer|resource",
            params = {"connection.getConsumer().getName()", "connection.getResource().getName()"}
    )
    private void saveConnection(@Nonnull final Connection<?, ?> connection, @Nonnull final Map<String, String> appSettings) {
        appSettings.putAll(connection.getEnvironmentVariables(project));
        if (!(connection.getResource().getData() instanceof AzuriteStorageAccount)) {
            return;
        }
        // start azurite if azurite connection is added
        final Boolean startAzurite = AzureTaskManager.getInstance().runAndWait("Start Azurite", () -> AzuriteService.getInstance().startAzurite(project)).join();
        if (BooleanUtils.isTrue(startAzurite)) {
            AzuriteTaskProvider.AzuriteBeforeRunTask.addStopAzuriteListener(this.functionRunConfiguration);
        }
    }

    private boolean isWebJobStorageRequired(@Nonnull List<BindingEnum> bindings) {
        return bindings.stream().map(BindingEnum::getType)
                .filter(type -> StringUtils.endsWithIgnoreCase(type, "Trigger"))
                .anyMatch(type -> AZURE_WEB_JOBS_STORAGE_NOT_REQUIRED_TRIGGERS.stream()
                        .noneMatch(trigger -> StringUtils.equalsIgnoreCase(type, trigger)));
    }

    private boolean isDebugMode() {
        return executor instanceof DefaultDebugExecutor;
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        return functionRunConfiguration.getModel().getTelemetryProperties();
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.RUN_FUNCTION_APP);
    }

    @Override
    @AzureOperation(name = "boundary/function.complete_run.func", params = {"this.functionRunConfiguration.getFuncPath()"})
    protected void onSuccess(Boolean result, RunProcessHandler processHandler) {
        stopProcessIfAlive(process);

        if (!processHandler.isProcessTerminated()) {
            processHandler.setText(message("function.run.hint.succeed"));
            processHandler.notifyComplete();
        }
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
    }

    @Override
    protected void onFail(@NotNull Throwable error, @NotNull RunProcessHandler processHandler) {
        super.onFail(error, processHandler);
        stopProcessIfAlive(process);
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
    }

    private boolean isInstallingExtensionNeeded(Set<BindingEnum> bindingTypes, RunProcessHandler processHandler) {
        final Map<String, Object> hostJson = readHostJson(stagingFolder.getAbsolutePath());
        final Map<String, Object> extensionBundle = hostJson == null ? null : (Map<String, Object>) hostJson.get(EXTENSION_BUNDLE);
        if (extensionBundle != null && extensionBundle.containsKey("id") &&
                StringUtils.equalsIgnoreCase((CharSequence) extensionBundle.get("id"), EXTENSION_BUNDLE_ID)) {
            processHandler.println(message("function.run.hint.skipInstallExtensionBundle"), ProcessOutputTypes.STDOUT);
            return false;
        }
        final boolean isNonHttpTriggersExist = bindingTypes.stream().anyMatch(binding ->
                !Arrays.asList(FUNCTION_WITHOUT_FUNCTION_EXTENSION).contains(binding));
        if (!isNonHttpTriggersExist) {
            processHandler.println(message("function.run.hint.skipInstallExtensionHttp"), ProcessOutputTypes.STDOUT);
            return false;
        }
        return true;
    }

    private static Map<String, Object> readHostJson(String stagingFolder) {
        final File hostJson = new File(stagingFolder, HOST_JSON);
        // noinspection unchecked
        return JsonUtils.readFromJsonFile(hostJson, Map.class);
    }

    private static Set<BindingEnum> getFunctionBindingEnums(Map<String, FunctionConfiguration> configMap) {
        final Set<BindingEnum> result = new HashSet<>();
        configMap.values().forEach(configuration -> configuration.getBindings().
                forEach(binding -> result.add(binding.getBindingEnum())));
        return result;
    }

    private static void stopProcessIfAlive(final Process proc) {
        if (proc != null && proc.isAlive()) {
            OSProcessUtil.killProcessTree(proc);
        }
    }
}
