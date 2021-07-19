/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.core;

import com.google.gson.JsonObject;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.lang.jvm.JvmAnnotation;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.azure.functions.annotation.StorageAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.utils.JsonUtils;
import com.microsoft.intellij.configuration.AzureConfigurations;
import com.microsoft.intellij.secure.IdeaSecureStore;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FunctionUtils {
    public static final String FUNCTION_JAVA_LIBRARY_ARTIFACT_ID = "azure-functions-java-library";
    private static final String AZURE_FUNCTION_ANNOTATION_CLASS =
            "com.microsoft.azure.functions.annotation.FunctionName";
    private static final String FUNCTION_JSON = "function.json";
    private static final String HTTP_OUTPUT_DEFAULT_NAME = "$return";
    private static final String DEFAULT_HOST_JSON = "{\"version\":\"2.0\",\"extensionBundle\":" +
            "{\"id\":\"Microsoft.Azure.Functions.ExtensionBundle\",\"version\":\"[1.*, 2.0.0)\"}}\n";
    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{ \"IsEncrypted\": false, \"Values\": " +
            "{ \"FUNCTIONS_WORKER_RUNTIME\": \"java\" } }";
    private static final String AZURE_FUNCTIONS = "azure-functions";
    private static final String AZURE_FUNCTION_CUSTOM_BINDING_CLASS =
            "com.microsoft.azure.functions.annotation.CustomBinding";
    private static final Map<BindingEnum, List<String>> REQUIRED_ATTRIBUTE_MAP = new HashMap<>();
    private static final List<String> CUSTOM_BINDING_RESERVED_PROPERTIES = Arrays.asList("type", "name", "direction");

    static {
        //initialize required attributes, which will be saved to function.json even if it equals to its default value
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.EventHubTrigger, Arrays.asList("cardinality"));
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.HttpTrigger, Arrays.asList("authLevel"));
    }

    public static void saveAppSettingsToSecurityStorage(String key, Map<String, String> appSettings) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        final String appSettingsJsonValue = JsonUtils.toJsonString(appSettings);
        IdeaSecureStore.getInstance().savePassword(key, appSettingsJsonValue);
    }

    public static Map<String, String> loadAppSettingsFromSecurityStorage(String key) {
        if (StringUtils.isEmpty(key)) {
            return new HashMap<>();
        }
        final String value = IdeaSecureStore.getInstance().loadPassword(key);
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

    @AzureOperation(
        name = "function.clean_staging_folder",
        params = {"stagingFolder.getName()"},
        type = AzureOperation.Type.TASK
    )
    public static void cleanUpStagingFolder(File stagingFolder) {
        try {
            if (stagingFolder != null) {
                FileUtils.deleteDirectory(stagingFolder);
            }
        } catch (final IOException e) {
            // swallow exceptions while clean up
        }
    }

    @AzureOperation(
        name = "function.list_function_modules",
        params = {"project.getName()"},
        type = AzureOperation.Type.TASK
    )
    public static Module[] listFunctionModules(Project project) {
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        return Arrays.stream(modules).filter(m -> {
            if (isModuleInTestScope(m)) {
                return false;
            }
            final GlobalSearchScope scope = GlobalSearchScope.moduleWithLibrariesScope(m);
            final PsiClass ecClass = JavaPsiFacade.getInstance(project).findClass(AZURE_FUNCTION_ANNOTATION_CLASS,
                                                                                  scope);
            return ecClass != null;
        }).toArray(Module[]::new);
    }

    public static Module getFunctionModuleByName(Project project, String name) {
        final Module[] modules = listFunctionModules(project);
        return Arrays.stream(modules)
                     .filter(module -> StringUtils.equals(name, module.getName()))
                     .findFirst().orElse(null);
    }

    @AzureOperation(
        name = "function.validate_project",
        params = {"project.getName()"},
        type = AzureOperation.Type.TASK
    )
    public static boolean isFunctionProject(Project project) {
        if (project == null) {
            return false;
        }
        final List<Library> libraries = new ArrayList<>();
        OrderEnumerator.orderEntries(project).productionOnly().forEachLibrary(library -> {
            if (StringUtils.contains(library.getName(), FUNCTION_JAVA_LIBRARY_ARTIFACT_ID)) {
                libraries.add(library);
            }
            return true;
        });
        return libraries.size() > 0;
    }

    @AzureOperation(
        name = "function.list_function_methods",
        params = {"module.getName()"},
        type = AzureOperation.Type.TASK
    )
    public static PsiMethod[] findFunctionsByAnnotation(Module module) {
        final PsiClass functionNameClass = JavaPsiFacade.getInstance(module.getProject())
                                                        .findClass(AZURE_FUNCTION_ANNOTATION_CLASS,
                                                                   GlobalSearchScope.moduleWithLibrariesScope(module));
        final List<PsiMethod> methods = new ArrayList<>(AnnotatedElementsSearch
                                                                .searchPsiMethods(functionNameClass,
                                                                                  GlobalSearchScope.moduleScope(module))
                                                                .findAll());
        return methods.toArray(new PsiMethod[0]);
    }

    public static Path getDefaultHostJson(Project project) {
        return new File(project.getBasePath(), "host.json").toPath();
    }

    public static boolean isFunctionClassAnnotated(final PsiMethod method) {
        return MetaAnnotationUtil.isMetaAnnotated(method,
                                                  ContainerUtil.immutableList(FunctionUtils.AZURE_FUNCTION_ANNOTATION_CLASS));
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

    @AzureOperation(
        name = "function.copy_settings",
        params = {"localSettingJson", "stagingFolder"},
        type = AzureOperation.Type.TASK
    )
    public static void copyLocalSettingsToStagingFolder(Path stagingFolder,
                                                        Path localSettingJson,
                                                        Map<String, String> appSettings) throws IOException {
        final File localSettingsFile = new File(stagingFolder.toFile(), "local.settings.json");
        copyFilesWithDefaultContent(localSettingJson, localSettingsFile, DEFAULT_LOCAL_SETTINGS_JSON);
        if (MapUtils.isNotEmpty(appSettings)) {
            updateLocalSettingValues(localSettingsFile, appSettings);
        }
    }

    @AzureOperation(
        name = "function.prepare_staging_folder",
        type = AzureOperation.Type.TASK
    )
    public static Map<String, FunctionConfiguration> prepareStagingFolder(Path stagingFolder, Path hostJson, Module module, PsiMethod[] methods)
            throws AzureExecutionException, IOException {
        final Map<String, FunctionConfiguration> configMap = generateConfigurations(methods);
        if (stagingFolder.toFile().isDirectory()) {
            FileUtils.cleanDirectory(stagingFolder.toFile());
        }

        final Path jarFile = JarUtils.buildJarFileToStagingPath(stagingFolder.toString(), module);
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

        final List<File> jarFiles = new ArrayList<>();
        OrderEnumerator.orderEntries(module).productionOnly().forEachLibrary(lib -> {
            if (StringUtils.isNotEmpty(lib.getName()) && ArrayUtils.contains(lib.getName().split("\\:"), FUNCTION_JAVA_LIBRARY_ARTIFACT_ID)) {
                return true;
            }

            if (lib != null) {
                for (final VirtualFile virtualFile : lib.getFiles(OrderRootType.CLASSES)) {
                    final File file = new File(stripExtraCharacters(virtualFile.getPath()));
                    if (file.exists()) {
                        jarFiles.add(file);
                    }
                }
            }
            return true;
        });
        final File libFolder = new File(stagingFolder.toFile(), "lib");
        for (final File file : jarFiles) {
            FileUtils.copyFileToDirectory(file, libFolder);
        }
        return configMap;
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

    public static String getFuncPath() throws IOException, InterruptedException {
        final AzureConfigurations.AzureConfigurationData state = AzureConfigurations.getInstance().getState();
        if (StringUtils.isBlank(state.functionCoreToolsPath())) {
            return FunctionCliResolver.resolveFunc();
        }
        return state.functionCoreToolsPath();
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
        JsonUtils.writeJsonToFile(file, json);
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
                message("function.binding.error.parseFailed",
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
        final PsiAnnotation storageAccount = AnnotationUtil.findAnnotation(method,
                                                                           StorageAccount.class.getCanonicalName());

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
        final File src = sourcePath == null ? null : sourcePath.toFile();
        if (src != null && src.exists()) {
            FileUtils.copyFile(src, dest);
        } else {
            FileUtils.write(dest, defaultContent, Charset.defaultCharset());
        }
    }

    private static void updateLocalSettingValues(File target, Map<String, String> appSettings) throws IOException {
        final JsonObject jsonObject = JsonUtils.readJsonFile(target);
        final JsonObject valueObject = new JsonObject();
        appSettings.entrySet().forEach(entry -> valueObject.addProperty(entry.getKey(), entry.getValue()));
        jsonObject.add("Values", valueObject);
        JsonUtils.writeJsonToFile(target, jsonObject);
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
}
