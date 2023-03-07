/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.core;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.lang.jvm.JvmAnnotation;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureBundle;
import com.microsoft.azure.toolkit.intellij.common.IdeUtils;
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.STORAGE_ACCOUNT;

public class FunctionUtils {
    private static final int MAX_PORT = 65535;

    private static final String AZURE_FUNCTION_ANNOTATION_CLASS =
            "com.microsoft.azure.functions.annotation.FunctionName";
    private static final String FUNCTION_JSON = "function.json";
    private static final String HTTP_OUTPUT_DEFAULT_NAME = "$return";
    private static final String DEFAULT_HOST_JSON = "{\"version\":\"2.0\",\"extensionBundle\":" +
            "{\"id\":\"Microsoft.Azure.Functions.ExtensionBundle\",\"version\":\"[3.*, 4.0.0)\"}}\n";
    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{ \"IsEncrypted\": false, \"Values\": " +
            "{ \"FUNCTIONS_WORKER_RUNTIME\": \"java\" } }";
    private static final String AZURE_FUNCTIONS = "azure-functions";
    private static final String AZURE_FUNCTION_CUSTOM_BINDING_CLASS =
            "com.microsoft.azure.functions.annotation.CustomBinding";
    private static final Map<BindingEnum, List<String>> REQUIRED_ATTRIBUTE_MAP = new HashMap<>();
    private static final List<String> CUSTOM_BINDING_RESERVED_PROPERTIES = Arrays.asList("type", "name", "direction");
    private static final String AZURE_FUNCTIONS_APP_SETTINGS = "Azure Functions App Settings";
    private static final String AZURE_FUNCTIONS_JAVA_LIBRARY = "azure-functions-java-library";
    private static final String AZURE_FUNCTIONS_JAVA_CORE_LIBRARY = "azure-functions-java-core-library";
    private static final Pattern ARTIFACT_NAME_PATTERN = Pattern.compile("(.*)-(\\d+\\.)?(\\d+\\.)?(\\*|\\d+).*");

    static {
        //initialize required attributes, which will be saved to function.json even if it equals to its default value
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.EventHubTrigger, Arrays.asList("cardinality"));
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.HttpTrigger, Arrays.asList("authLevel"));
    }

    public static void saveAppSettingsToSecurityStorage(String key, Map<String, String> appSettings) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        final String appSettingsJsonValue = JsonUtils.toJson(appSettings);
        IntelliJSecureStore.getInstance().savePassword(AZURE_FUNCTIONS_APP_SETTINGS, key, null, appSettingsJsonValue);
    }

    public static Map<String, String> loadAppSettingsFromSecurityStorage(String key) {
        if (StringUtils.isEmpty(key)) {
            return new HashMap<>();
        }
        final String value = IntelliJSecureStore.getInstance().loadPassword(AZURE_FUNCTIONS_APP_SETTINGS, key, null);
        return StringUtils.isEmpty(value) ? new HashMap<>() : JsonUtils.fromJson(value, Map.class);
    }

    public static File getTempStagingFolder() {
        try {
            final Path path = Files.createTempDirectory(AZURE_FUNCTIONS);
            final File file = path.toFile();
            FileUtils.forceDeleteOnExit(file);
            return file;
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException("failed to get temp staging folder", e);
        }
    }

    @AzureOperation(name = "boundary/function.clean_staging_folder.folder", params = {"stagingFolder.getName()"})
    public static void cleanUpStagingFolder(File stagingFolder) {
        try {
            if (stagingFolder != null) {
                FileUtils.deleteDirectory(stagingFolder);
            }
        } catch (final IOException e) {
            // swallow exceptions while clean up
        }
    }

    @AzureOperation(name = "boundary/function.list_function_modules.project", params = {"project.getName()"})
    public static Module[] listFunctionModules(Project project) {
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        return Arrays.stream(modules).filter(m -> {
            if (isModuleInTestScope(m)) {
                return false;
            }
            final GlobalSearchScope scope = GlobalSearchScope.moduleWithLibrariesScope(m);
            final Callable<PsiClass> psiClassSupplier = () -> JavaPsiFacade.getInstance(project).findClass(AZURE_FUNCTION_ANNOTATION_CLASS, scope);
            final PsiClass ecClass = AzureTaskManager.getInstance().readAsObservable(new AzureTask<>(psiClassSupplier)).toBlocking().first();
            return ecClass != null;
        }).toArray(Module[]::new);
    }

    public static Module getFunctionModuleByName(Project project, String name) {
        final Module[] modules = listFunctionModules(project);
        return Arrays.stream(modules)
                     .filter(module -> StringUtils.equals(name, module.getName()))
                     .findFirst().orElse(null);
    }

    @AzureOperation(name = "boundary/common.validate_func_project.project", params = {"project.getName()"})
    public static boolean isFunctionProject(Project project) {
        if (project == null) {
            return false;
        }
        final List<Library> libraries = new ArrayList<>();
        OrderEnumerator.orderEntries(project).productionOnly().forEachLibrary(library -> {
            if (StringUtils.containsAnyIgnoreCase(library.getName(), AZURE_FUNCTIONS_JAVA_LIBRARY, AZURE_FUNCTIONS_JAVA_CORE_LIBRARY)) {
                libraries.add(library);
            }
            return true;
        });
        return libraries.size() > 0;
    }

    @AzureOperation(name = "boundary/function.list_function_methods.module", params = {"module.getName()"})
    public static PsiMethod[] findFunctionsByAnnotation(Module module) {
        if (module == null) {
            return new PsiMethod[0];
        }
        final PsiClass functionNameClass = JavaPsiFacade.getInstance(module.getProject())
                                                        .findClass(AZURE_FUNCTION_ANNOTATION_CLASS,
                                                                   GlobalSearchScope.moduleWithLibrariesScope(module));
        final List<PsiMethod> methods = new ArrayList<>(AnnotatedElementsSearch
                                                                .searchPsiMethods(functionNameClass,
                                                                                  GlobalSearchScope.moduleScope(module))
                                                                .findAll());
        return methods.toArray(new PsiMethod[0]);
    }

    public static boolean isFunctionClassAnnotated(final PsiMethod method) {
        try {
            return MetaAnnotationUtil.isMetaAnnotated(method,
                    ContainerUtil.immutableList(FunctionUtils.AZURE_FUNCTION_ANNOTATION_CLASS));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static @Nullable Path createTempleHostJson() {
        try {
            final File result = File.createTempFile("host", ".json");
            FileUtils.write(result, DEFAULT_HOST_JSON, Charset.defaultCharset());
            return result.toPath();
        } catch (final IOException e) {
            return null;
        }
    }

    @AzureOperation(name = "internal/function.copy_settings.settings|folder", params = {"localSettingJson", "stagingFolder"})
    public static void copyLocalSettingsToStagingFolder(@Nonnull final Path stagingFolder,
                                                        @Nullable final Path localSettingJson,
                                                        @Nullable Map<String, String> appSettings, boolean useLocalSettings) throws IOException {
        final File localSettingsFile = new File(stagingFolder.toFile(), "local.settings.json");
        copyFilesWithDefaultContent(localSettingJson, localSettingsFile, DEFAULT_LOCAL_SETTINGS_JSON);
        if (MapUtils.isNotEmpty(appSettings)) {
            updateLocalSettingValues(localSettingsFile, appSettings, useLocalSettings);
        }
    }

    @AzureOperation(name = "boundary/function.prepare_staging_folder")
    public static Map<String, FunctionConfiguration> prepareStagingFolder(@Nonnull final Path stagingFolder, @Nullable final Path hostJson,
                                                                          @Nonnull final Project project, @Nonnull final Module module, PsiMethod[] methods)
            throws AzureExecutionException, IOException {
        final Map<String, FunctionConfiguration> configMap = ReadAction.compute(() -> generateConfigurations(methods));
        if (stagingFolder.toFile().isDirectory()) {
            FileUtils.cleanDirectory(stagingFolder.toFile());
        }

        final Path jarFile;
        // test if it is gradle project
        final IntellijGradleFunctionProject gradleProject = new IntellijGradleFunctionProject(project, module);
        if (gradleProject.isValid() && gradleProject.getArtifactFile() != null) {
            jarFile = gradleProject.getArtifactFile().toPath();
            gradleProject.packageJar();
            if (!gradleProject.getArtifactFile().exists()) {
                final String error = String.format("Failed generate jar file for project(%s)", gradleProject.getName());
                throw new AzureToolkitRuntimeException(error);
            }
            FileUtils.copyFileToDirectory(gradleProject.getArtifactFile(), stagingFolder.toFile());
        } else {
            jarFile = JarUtils.buildJarFileToStagingPath(stagingFolder.toString(), module);
        }

        final String scriptFilePath = "../" + jarFile.getFileName().toString();
        configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
        for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
            if (StringUtils.isNotBlank(config.getKey())) {
                final File functionJsonFile = Paths.get(stagingFolder.toString(), config.getKey(), FUNCTION_JSON)
                                                   .toFile();
                writeFunctionJsonFile(functionJsonFile, config.getValue());
            }
        }

        final File hostJsonFile = new File(stagingFolder.toFile(), "host.json");
        copyFilesWithDefaultContent(hostJson, hostJsonFile, DEFAULT_HOST_JSON);

        final List<File> dependencies = new ArrayList<>();
        if (gradleProject.isValid()) {
            gradleProject.getDependencies().forEach(lib -> dependencies.add(lib));
        } else {
            OrderEnumerator.orderEntries(module).productionOnly().forEach(lib -> {
                if (lib instanceof ModuleOrderEntry) {
                    Optional.ofNullable(getArtifactFromModule(((ModuleOrderEntry) lib).getModule())).ifPresent(dependencies::add);
                } else if (lib instanceof LibraryOrderEntry) {
                    Arrays.stream(lib.getFiles(OrderRootType.CLASSES)).map(virtualFile -> new File(stripExtraCharacters(virtualFile.getPath())))
                            .filter(File::exists)
                            .forEach(dependencies::add);
                }
                return true;
            });
        }
        final String libraryToExclude = dependencies.stream()
                .map(FunctionUtils::getArtifactIdFromFile)
                .filter(name -> StringUtils.equalsAnyIgnoreCase(name, AZURE_FUNCTIONS_JAVA_CORE_LIBRARY))
                .findFirst().orElse(AZURE_FUNCTIONS_JAVA_LIBRARY);

        final File libFolder = new File(stagingFolder.toFile(), "lib");
        for (final File file : dependencies) {
            if (!StringUtils.equalsIgnoreCase(getArtifactIdFromFile(file), libraryToExclude)) {
                if (!file.exists()) {
                    throw new AzureToolkitRuntimeException(String.format("Dependency artifact (%s) not found, please correct the dependency and try again", file.getAbsolutePath()));
                }
                FileUtils.copyFileToDirectory(file, libFolder);
            }
        }
        return configMap;
    }

    // get artifact based on module
    @Nullable
    private static File getArtifactFromModule(final Module module) {
        final Project project = module.getProject();
        final AzureArtifactManager artifactManager = AzureArtifactManager.getInstance(project);
        return artifactManager.getAllSupportedAzureArtifacts().stream()
                .filter(artifact -> Objects.equals(artifactManager.getModuleFromAzureArtifact(artifact), module))
                .findFirst()
                .map(azureArtifact -> artifactManager.getFileForDeployment(azureArtifact))
                .map(File::new)
                .orElse(null);
    }

    private static String getArtifactIdFromFile(@Nonnull final File file) {
        final Matcher matcher = ARTIFACT_NAME_PATTERN.matcher(file.getName());
        return matcher.matches() ? StringUtils.substringBeforeLast(file.getName(), "-") :
                StringUtils.substringBeforeLast(file.getName(), ".jar");
    }

    public static String getTargetFolder(Module module) {
        if (module == null) {
            return StringUtils.EMPTY;
        }
        final Project project = module.getProject();
        final MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(module);
        final String functionAppName = mavenProject == null ? null : mavenProject.getProperties().getProperty(
                "functionAppName");
        final String stagingFolderName = StringUtils.isEmpty(functionAppName) ? module.getName() : functionAppName;
        return Paths.get(project.getBasePath(), "target", "azure-functions", stagingFolderName).toString();
    }

    public static String getDefaultHostJsonPath(final Module module) {
        return findFileInFunctionModule(module, "host.json");
    }

    public static String getDefaultLocalSettingsJsonPath(final Module module) {
        return findFileInFunctionModule(module, "local.settings.json");
    }

    public static String findFileInFunctionModule(final Module module, final String file) {
        String result;
        try {
            result = ReadAction.compute(() -> FilenameIndex.getVirtualFilesByName(file, GlobalSearchScope.projectScope(module.getProject())))
                    .stream().filter(m -> IdeUtils.isSameModule(module, ModuleUtil.findModuleForFile(m, module.getProject())))
                    .sorted(Comparator.comparing(VirtualFile::getCanonicalPath))
                    .findFirst()
                    .map(VirtualFile::getCanonicalPath).orElse(null);
        } catch (final RuntimeException e) {
            result = null;
        }
        return StringUtils.isNotEmpty(result) ? result :
                Paths.get(ModuleUtil.getModuleDirPath(module), file).toString();
    }

    public static String getFuncPath() throws IOException, InterruptedException {
        final AzureConfiguration config = Azure.az().config();
        if (StringUtils.isBlank(config.getFunctionCoreToolsPath())) {
            return FunctionCliResolver.resolveFunc();
        }
        return config.getFunctionCoreToolsPath();
    }

    public static List<String> getFunctionBindingList(Map<String, FunctionConfiguration> configMap) {
        return configMap.values().stream().flatMap(configuration -> configuration.getBindings().stream())
                        .map(Binding::getType)
                        .sorted()
                        .distinct()
                        .collect(Collectors.toList());
    }

    private static void writeFunctionJsonFile(File file, FunctionConfiguration config) throws IOException {
        final Map<String, Object> json = new LinkedHashMap<>();
        json.put("scriptFile", config.getScriptFile());
        json.put("entryPoint", config.getEntryPoint());
        final List<Map<String, Object>> lists = new ArrayList<>();
        if (config.getBindings() != null) {
            for (final Binding binding : config.getBindings()) {
                final Map<String, Object> bindingJson = new LinkedHashMap<>();
                bindingJson.put("type", binding.getType());
                bindingJson.put("direction", binding.getDirection());
                bindingJson.put("name", binding.getName());
                final Map<String, Object> attributes = binding.getBindingAttributes();
                for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
                    // Skip 'name' property since we have serialized before the for-loop
                    if (bindingJson.containsKey(entry.getKey())) {
                        continue;
                    }
                    bindingJson.put(entry.getKey(), entry.getValue());
                }
                lists.add(bindingJson);
            }
            json.put("bindings", lists.toArray());
        }
        file.getParentFile().mkdirs();
        JsonUtils.writeToJsonFile(file, json);
    }

    private static String stripExtraCharacters(String fileName) {
        // TODO-dp this is not robust enough (eliminated !/ at the end of the jar)
        return StringUtils.endsWith(fileName, "!/") ?
               fileName.substring(0, fileName.length() - 2) : fileName;
    }

    private static Map<String, FunctionConfiguration> generateConfigurations(final PsiMethod[] methods)
            throws AzureExecutionException {
        final Map<String, FunctionConfiguration> configMap = new HashMap<>();
        for (final PsiMethod method : methods) {
            final PsiAnnotation annotation = AnnotationUtil.findAnnotation(method,
                                                                           FunctionUtils.AZURE_FUNCTION_ANNOTATION_CLASS);
            final String functionName = AnnotationUtil.getDeclaredStringAttributeValue(annotation, "value");
            configMap.put(functionName, generateConfiguration(method));
        }
        return configMap;
    }

    private static FunctionConfiguration generateConfiguration(PsiMethod method) throws AzureExecutionException {
        final FunctionConfiguration config = new FunctionConfiguration();
        final List<Binding> bindings = new ArrayList<>();
        processParameterAnnotations(method, bindings);
        processMethodAnnotations(method, bindings);
        patchStorageBinding(method, bindings);
        config.setEntryPoint(method.getContainingClass().getQualifiedName() + "." + method.getName());
        // Todo: add set bindings method in tools-common
        config.setBindings(bindings);
        return config;
    }

    private static void processParameterAnnotations(final PsiMethod method, final List<Binding> bindings)
            throws AzureExecutionException {
        for (final JvmParameter param : method.getParameters()) {
            bindings.addAll(parseAnnotations(method.getProject(), param.getAnnotations()));
        }
    }

    private static List<Binding> parseAnnotations(final Project project,
                                                  JvmAnnotation[] annotations) throws AzureExecutionException {
        final List<Binding> bindings = new ArrayList<>();

        for (final JvmAnnotation annotation : annotations) {
            final Binding binding = getBinding(project, annotation);
            if (binding != null) {
                Log.debug("Adding binding: " + binding.toString());
                bindings.add(binding);
            }
        }

        return bindings;
    }

    private static Binding getBinding(final Project project, JvmAnnotation annotation) throws AzureExecutionException {
        if (annotation == null) {
            return null;
        }
        if (!(annotation instanceof PsiAnnotation)) {
            throw new AzureExecutionException(
                AzureBundle.message("function.binding.error.parseFailed",
                        PsiAnnotation.class.getCanonicalName(),
                        annotation.getClass().getCanonicalName()));
        }

        final BindingEnum annotationEnum =
                Arrays.stream(BindingEnum.values())
                      .filter(bindingEnum -> StringUtils.equalsIgnoreCase(bindingEnum.name(),
                              ClassUtils.getShortClassName(annotation.getQualifiedName())))
                      .findFirst()
                      .orElse(null);
        return annotationEnum == null ? getUserDefinedBinding(project, (PsiAnnotation) annotation)
                                      : createBinding(project, annotationEnum, (PsiAnnotation) annotation);
    }

    private static Binding getUserDefinedBinding(final Project project, PsiAnnotation annotation) throws AzureExecutionException {
        final PsiJavaCodeReferenceElement referenceElement = annotation.getNameReferenceElement();
        if (referenceElement == null) {
            return null;
        }
        final PsiAnnotation customBindingAnnotation =
                AnnotationUtil.findAnnotation((PsiModifierListOwner) referenceElement.resolve(),
                                              AZURE_FUNCTION_CUSTOM_BINDING_CLASS);
        if (customBindingAnnotation == null) {
            return null;
        }
        final Map<String, Object> annotationProperties = AnnotationHelper.evaluateAnnotationProperties(project,
                                                                                                       annotation,
                                                                                                       CUSTOM_BINDING_RESERVED_PROPERTIES);
        final Map<String, Object> customBindingProperties = AnnotationHelper.evaluateAnnotationProperties(project,
                                                                                                          customBindingAnnotation,
                                                                                                          null);

        final Map<String, Object> mergedMap = new HashMap<>(annotationProperties);
        customBindingProperties.forEach(mergedMap::putIfAbsent);
        final Binding extendBinding = new Binding(BindingEnum.CustomBinding) {

            public String getName() {
                return (String) mergedMap.get("name");
            }

            public String getDirection() {
                return (String) mergedMap.get("direction");
            }

            public String getType() {
                return (String) mergedMap.get("type");
            }
        };

        annotationProperties.forEach((name, value) -> {
            if (!CUSTOM_BINDING_RESERVED_PROPERTIES.contains(name)) {
                extendBinding.setAttribute(name, value);
            }
        });
        return extendBinding;
    }

    private static void processMethodAnnotations(final PsiMethod method, final List<Binding> bindings)
            throws AzureExecutionException {
        if (!method.getReturnType().equals(Void.TYPE)) {
            bindings.addAll(parseAnnotations(method.getProject(), method.getAnnotations()));

            if (bindings.stream().anyMatch(b -> b.getBindingEnum() == BindingEnum.HttpTrigger) &&
                    bindings.stream().noneMatch(b -> StringUtils.equalsIgnoreCase(b.getName(), "$return"))) {
                bindings.add(getHTTPOutBinding());
            }
        }
    }

    private static Binding getHTTPOutBinding() {
        final Binding result = new Binding(BindingEnum.HttpOutput);
        result.setName(HTTP_OUTPUT_DEFAULT_NAME);
        return result;
    }

    private static void patchStorageBinding(final PsiMethod method, final List<Binding> bindings) {
        final PsiAnnotation storageAccount = AnnotationUtil.findAnnotation(method, STORAGE_ACCOUNT);

        if (storageAccount != null) {
            // todo: Remove System.out.println
            System.out.println(message("function.binding.storage.found"));

            final String connectionString = AnnotationUtil.getDeclaredStringAttributeValue(storageAccount, "value");
            // Replace empty connection string
            bindings.stream().filter(binding -> binding.getBindingEnum().isStorage())
                    .filter(binding -> StringUtils.isEmpty((String) binding.getAttribute("connection")))
                    .forEach(binding -> binding.setAttribute("connection", connectionString));

        } else {
            // todo: Remove System.out.println
            System.out.println(message("function.binding.storage.notFound"));
        }
    }

    private static Binding createBinding(final Project project, BindingEnum bindingEnum, PsiAnnotation annotation)
            throws AzureExecutionException {
        final Binding binding = new Binding(bindingEnum);
        AnnotationHelper.evaluateAnnotationProperties(project, annotation, REQUIRED_ATTRIBUTE_MAP.get(bindingEnum))
                .forEach((name, value) -> {
                    binding.setAttribute(name, value);
                });
        return binding;
    }

    private static void copyFilesWithDefaultContent(Path sourcePath, File dest, String defaultContent)
            throws IOException {
        final File src = Optional.ofNullable(sourcePath).map(Path::toFile).orElse(null);
        if (src != null && src.exists()) {
            FileUtils.copyFile(src, dest);
        } else {
            FileUtils.write(dest, defaultContent, Charset.defaultCharset());
        }
    }

    private static void updateLocalSettingValues(File target, Map<String, String> appSettings, boolean useLocalSettings) throws IOException {
        final Map<String, Object> jsonObject = ObjectUtils.firstNonNull(JsonUtils.readFromJsonFile(target, Map.class), new HashMap<>());
        if (jsonObject.containsKey("Values") && useLocalSettings) {
            final Object values = jsonObject.get("Values");
            if (values instanceof Map) {
                ((Map) values).putAll(appSettings);
            }
        } else {
            jsonObject.put("Values", new HashMap<>(appSettings));
        }
        JsonUtils.writeToJsonFile(target, jsonObject);
    }

    private static boolean isModuleInTestScope(Module module) {
        if (module == null) {
            return false;
        }
        final CompilerModuleExtension cme = CompilerModuleExtension.getInstance(module);
        if (cme == null) {
            return false;
        }
        return cme.getCompilerOutputUrl() == null && cme.getCompilerOutputUrlForTests() != null;
    }

    public static String getDefaultFuncArguments() {
        final int freePort = findFreePort();
        return freePort > 0 ? getDefaultFuncArguments(freePort) : "host start";
    }

    public static String getDefaultFuncArguments(final int port) {
        return String.format("host start --port %s", port);
    }

    public static int findFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            return -1;
        }
    }

    public static int findFreePort(int startPort, int... skipPorts) {
        ServerSocket socket = null;
        for (int port = startPort; port <= MAX_PORT; port++) {
            if (!ArrayUtils.contains(skipPorts, port) && isPortFree(port)) {
                return port;
            }
        }
        return -1;
    }

    private static final boolean isPortFree(int port) {
        return SystemInfo.isMac ? isPortFreeInMac(port) : isPortFreeInWindows(port);
    }

    private static final boolean isPortFreeInWindows(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static final boolean isPortFreeInMac(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            // setReuseAddress(false) is required only on OSX,
            // otherwise the code will not work correctly on that platform
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
