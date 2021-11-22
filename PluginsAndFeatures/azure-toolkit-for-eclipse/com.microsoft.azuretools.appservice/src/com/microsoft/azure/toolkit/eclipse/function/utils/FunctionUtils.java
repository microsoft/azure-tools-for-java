/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.utils;

import com.google.gson.JsonObject;
import com.microsoft.azure.functions.annotation.ExponentialBackoffRetry;
import com.microsoft.azure.functions.annotation.FixedDelayRetry;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.StorageAccount;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingFactory;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.Retry;
import com.microsoft.azuretools.core.utils.MavenUtils;
import com.microsoft.azuretools.utils.JsonUtils;
import lombok.Lombok;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FunctionUtils {
    private static final String MULTI_RETRY_ANNOTATION = "Fixed delay retry and exponential backoff retry are not compatible, " +
            "please use either of them for one trigger";
    public static final String FUNCTION_JAVA_LIBRARY_ARTIFACT_ID = "azure-functions-java-library";
    private static final String AZURE_FUNCTION_ANNOTATION_CLASS =
            "com.microsoft.azure.functions.annotation.FunctionName";
    private static final String FUNCTION_JSON = "function.json";
    private static final String DEFAULT_HOST_JSON = "{\"version\":\"2.0\",\"extensionBundle\":" +
            "{\"id\":\"Microsoft.Azure.Functions.ExtensionBundle\",\"version\":\"[1.*, 2.0.0)\"}}\n";
    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{ \"IsEncrypted\": false, \"Values\": " +
            "{ \"FUNCTIONS_WORKER_RUNTIME\": \"java\" } }";
    private static final String AZURE_FUNCTIONS = "azure-functions-for-eclipse";
    private static final Map<BindingEnum, List<String>> REQUIRED_ATTRIBUTE_MAP = new HashMap<>();
    private static final List<String> CUSTOM_BINDING_RESERVED_PROPERTIES = Arrays.asList("type", "name", "direction");

    static {
        //initialize required attributes, which will be saved to function.json even if it equals to its default value
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.EventHubTrigger, Arrays.asList("cardinality"));
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.HttpTrigger, Arrays.asList("authLevel"));
    }

    public static List<IJavaProject> listJavaProjects() {
        return listProjects(project -> {
            try {
                return project.hasNature(JavaCore.NATURE_ID);
            } catch (CoreException e) {
                throw Lombok.sneakyThrow(e);
            }
        }).stream().map(JavaCore::create).collect(Collectors.toList());
    }

    public static List<IProject> listProjects(Predicate<IProject> predicate) {
        List<IProject> projectList = new ArrayList<>();
        try {
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            IProject[] projects = workspaceRoot.getProjects();
            for (IProject project : projects) {
                if (project.isOpen() && predicate.test(project)) {
                    projectList.add(project);
                }
            }
        } catch (Throwable e) {
            throw new AzureToolkitRuntimeException("Cannot list projects.", e);
        }
        return projectList;
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
    public static IJavaProject[] listFunctionProjects() {
        return listJavaProjects().stream().filter(FunctionUtils::isFunctionProject).toArray(IJavaProject[]::new);
    }

    @AzureOperation(
            name = "common.validate_project",
            params = {"project.getName()"},
            type = AzureOperation.Type.TASK
    )
    public static boolean isFunctionProject(IJavaProject project) {
        if (project == null) {
            return false;
        }
        if (!MavenUtils.isMavenProject(project.getProject())) {
            return false;
        }
        try {
            CollectingSearchRequestor requestor2 = searchFunctionNameAnnotation(project);
            return !requestor2.getResults().isEmpty();
        } catch (CoreException e) {
            // ignore
        }
        return false;
    }

    public static Path createTempleHostJson() {
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
        appSettings.forEach(valueObject::addProperty);
        jsonObject.add("Values", valueObject);
        JsonUtils.writeJsonToFile(target, jsonObject);
    }

    @AzureOperation(
            name = "function.prepare_staging_folder",
            type = AzureOperation.Type.TASK
    )
    public static void prepareStagingFolder(IJavaProject project, Path stagingFolder, Path hostJson,
                                                                          Path localSettingJson)
            throws Exception {

        List<IMethod> methods = findFunctionsByAnnotation(project);
        final Map<String, FunctionConfiguration> configMap = generateConfigurations(methods);
        if (stagingFolder.toFile().isDirectory()) {
            FileUtils.cleanDirectory(stagingFolder.toFile());
        }

        if (!MavenUtils.isMavenProject(project.getProject())) {
            throw new AzureToolkitRuntimeException("Non-maven project is not supported in eclipse by now.");
        }

        IFile pom = MavenUtils.getPomFile(project.getProject());
        final MavenProject mavenProject = MavenUtils.toMavenProject(pom);

        final Path jarFile = JarUtils.buildJarFileToStagingPath(stagingFolder.toString(), mavenProject);
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

        final File localSettingFile = new File(stagingFolder.toFile(), "local.settings.json");
        copyFilesWithDefaultContent(localSettingJson, localSettingFile, DEFAULT_LOCAL_SETTINGS_JSON);

        final List<File> jarFiles = new ArrayList<>();
        mavenProject.getArtifacts().forEach(t -> {
            if (!StringUtils.equals(t.getScope(), "test") && !StringUtils.contains(t.getArtifactId(), FUNCTION_JAVA_LIBRARY_ARTIFACT_ID)) {
                jarFiles.add(t.getFile());
            }
        });
        final File libFolder = new File(stagingFolder.toFile(), "lib");
        for (final File file : jarFiles) {
            FileUtils.copyFileToDirectory(file, libFolder);
        }
    }

    private static CollectingSearchRequestor searchFunctionNameAnnotation(IJavaProject javaProject) throws CoreException {
        CollectingSearchRequestor requester = new CollectingSearchRequestor();
        IType type = javaProject.findType(AZURE_FUNCTION_ANNOTATION_CLASS);
        if (type != null) {

            SearchPattern pattern = SearchPattern.createPattern(type,
                    IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE,
                    SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);

            IJavaSearchScope workspaceScope = JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(javaProject, false);
            new SearchEngine().search(pattern, new SearchParticipant[]{SearchEngine
                    .getDefaultSearchParticipant()}, workspaceScope, requester, new NullProgressMonitor());
        }
        return requester;
    }

    @AzureOperation(
            name = "function.list_function_methods",
            params = {"project.getName()"},
            type = AzureOperation.Type.TASK
    )
    public static List<IMethod> findFunctionsByAnnotation(IJavaProject project) {
        try {
            return searchFunctionNameAnnotation(project).getResults()
                    .stream()
                    .filter(t -> t.getElement() instanceof IMethod)
                    .map(t -> ((IMethod) t.getElement())).distinct().collect(Collectors.toList());
        } catch (CoreException ex) {
            // Log and warn
            return Collections.emptyList();
        }
    }

    private static Map<String, FunctionConfiguration> generateConfigurations(final List<IMethod> methods) throws JavaModelException {
        final Map<String, FunctionConfiguration> configMap = new HashMap<>();
        for (final IMethod method : methods) {
            generateConfiguration(method, configMap);
        }
        return configMap;
    }

    private static boolean isAvailable(ISourceRange range) {
        return range != null && range.getOffset() != -1;
    }

    private static IMethodBinding getMethodBinding(IMethod method) throws JavaModelException {
        IType declaringType = method.getDeclaringType();
        ASTParser parser = ASTParser.newParser(AST.JLS16);
        if (declaringType.getCompilationUnit() != null) {
            parser.setSource(declaringType.getCompilationUnit());
        } else if (!isAvailable(declaringType.getSourceRange())) {
            parser.setProject(declaringType.getJavaProject());
            IBinding[] bindings = parser.createBindings(new IJavaElement[]{declaringType}, new NullProgressMonitor());
            if (bindings.length == 0) {
                throw new RuntimeException(String.format("Cannot resolve binding for method '%s'", method.getElementName()));
            }
            if (bindings[0] instanceof ITypeBinding) {
                ITypeBinding classBinding = (ITypeBinding) bindings[0];
                return Arrays.stream(classBinding.getDeclaredMethods()).filter(t -> isSameMethod(method, t)).findAny().orElse(null);
            }
            throw new RuntimeException(String.format("Get illegal binding for method '%s'", bindings[0].getClass().getSimpleName()));
        } else {
            parser.setSource(declaringType.getClassFile());
        }
        parser.setIgnoreMethodBodies(true);
        parser.setResolveBindings(true);

        CompilationUnit root = (CompilationUnit) parser.createAST(new NullProgressMonitor());
        MethodDeclaration node = (MethodDeclaration) root.findDeclaringNode(method.getKey());
        return node.resolveBinding();
    }

    private static boolean isSameMethod(IMethod method, IMethodBinding methodBinding) {
        IMethod javaElement = (IMethod) methodBinding.getJavaElement();
        if (method.getElementName().equals(javaElement.getElementName()) &&
                method.getNumberOfParameters() == javaElement.getNumberOfParameters()) {
            for (int i = 0; i < method.getNumberOfParameters(); i++) {
                if (!removeGeneric(method.getParameterTypes()[i]).equals(
                        removeGeneric(javaElement.getParameterTypes()[i]))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static String removeGeneric(String signature) {
        return signature.replaceAll("<.*>", "");
    }

    public static FunctionConfiguration generateConfiguration(IMethod method, Map<String, FunctionConfiguration> map) throws JavaModelException {
        IMethodBinding methodBinding = getMethodBinding(method);
        IAnnotationBinding functionNameAnnotation = AnnotationUtils.findAnnotation(methodBinding.getAnnotations(), FunctionName.class);
        final FunctionConfiguration config = new FunctionConfiguration();
        String functionName = functionNameAnnotation != null ? AnnotationUtils.getDeclaredStringAttributeValue(functionNameAnnotation
                , "value") : null;

        FunctionConfiguration configuration = new FunctionConfiguration();
        configuration.setEntryPoint(StringUtils.replaceChars(methodBinding.getDeclaringClass().getBinaryName(), '$', '.') +
                "." + methodBinding.getName());
        map.put(functionName, configuration);

        int len = method.getParameterNames().length;
        for (int i = 0; i < len; i++) {
            IAnnotationBinding[] parameterAnnotations = methodBinding.getParameterAnnotations(i);
            for (IAnnotationBinding annotation : parameterAnnotations) {
                Binding binding1 = getBinding(annotation);
                if (binding1 != null) {
                    configuration.getBindings().add(binding1);
                }
            }
        }

        if (!StringUtils.equals(method.getReturnType(), "void")) {
            processMethodAnnotations(methodBinding, configuration.getBindings());
        }

        patchStorageBinding(methodBinding, configuration.getBindings());
        config.setRetry(getRetryConfigurationFromMethod(methodBinding));
        return config;
    }

    private static Binding createBinding(BindingEnum bindingEnum, IAnnotationBinding annotationBinding) {
        final Binding binding = new Binding(bindingEnum);
        AnnotationUtils.resolveAnnotationProperties(annotationBinding, REQUIRED_ATTRIBUTE_MAP.get(bindingEnum))
                .forEach(binding::setAttribute);
        return binding;
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

    public static String getFuncPath() throws IOException, InterruptedException {
        final AzureConfiguration config = Azure.az().config();
        if (StringUtils.isBlank(config.getFunctionCoreToolsPath())) {
            return FunctionCliResolver.resolveFunc();
        }
        return config.getFunctionCoreToolsPath();
    }

    private static void patchStorageBinding(final IMethodBinding method, final List<Binding> bindings) {
        final IAnnotationBinding storageAccount = AnnotationUtils.findAnnotation(method.getAnnotations(),
                StorageAccount.class);

        if (storageAccount != null) {
            final String connectionString = AnnotationUtils.getDeclaredStringAttributeValue(storageAccount, "value");
            bindings.stream().filter(binding -> binding.getBindingEnum().isStorage())
                    .filter(binding -> StringUtils.isEmpty((String) binding.getAttribute("connection")))
                    .forEach(binding -> binding.setAttribute("connection", connectionString));

        }
    }

    private static Retry getRetryConfigurationFromMethod(IMethodBinding method) {
        final IAnnotationBinding fixedDelayRetry = AnnotationUtils.findAnnotation(method.getAnnotations(), FixedDelayRetry.class);
        final IAnnotationBinding exponentialBackoffRetry = AnnotationUtils.findAnnotation(method.getAnnotations(), ExponentialBackoffRetry.class);

        if (fixedDelayRetry != null && exponentialBackoffRetry != null) {
            throw new AzureToolkitRuntimeException(MULTI_RETRY_ANNOTATION);
        }
        if (fixedDelayRetry != null) {
            return createRetryFromMap(AnnotationUtils.resolveAllAnnotationProperties(fixedDelayRetry));
        }
        if (exponentialBackoffRetry != null) {
            return createRetryFromMap(AnnotationUtils.resolveAllAnnotationProperties(exponentialBackoffRetry));
        }
        return null;
    }

    private static Retry createRetryFromMap(Map<String, Object> map) {
        return JsonUtils.fromJsonString(JsonUtils.toJsonString(map), Retry.class);
    }

    private static void processMethodAnnotations(IMethodBinding method, final List<Binding> bindings) {
        bindings.addAll(parseAnnotations(method::getAnnotations, FunctionUtils::parseMethodAnnotation));

        if (bindings.stream().anyMatch(b -> b.getBindingEnum() == BindingEnum.HttpTrigger) &&
                bindings.stream().noneMatch(b -> b.getName().equalsIgnoreCase("$return"))) {
            bindings.add(BindingFactory.getHTTPOutBinding());
        }
    }

    protected static Binding parseMethodAnnotation(final IAnnotationBinding annotation) {
        final Binding ret = parseParameterAnnotation(annotation);
        if (ret != null) {
            ret.setName("$return");
        }
        return ret;
    }

    private static Binding parseParameterAnnotation(IAnnotationBinding annotation) {
        return getBinding(annotation);
    }

    private static List<Binding> parseAnnotations(Supplier<IAnnotationBinding[]> annotationProvider,
                                                  Function<IAnnotationBinding, Binding> annotationParser) {
        final List<Binding> bindings = new ArrayList<>();

        for (final IAnnotationBinding annotation : annotationProvider.get()) {
            final Binding binding = annotationParser.apply(annotation);
            if (binding != null) {
                bindings.add(binding);
            }
        }
        return bindings;
    }

    private static Binding getBinding(IAnnotationBinding annotationBinding) {
        String fqn = annotationBinding.getAnnotationType().getBinaryName();
        final BindingEnum annotationEnum =
                Arrays.stream(BindingEnum.values())
                        .filter(bindingEnum -> StringUtils.equalsIgnoreCase(bindingEnum.name(),
                                ClassUtils.getShortClassName(fqn)))
                        .findFirst()
                        .orElse(null);
        IAnnotationBinding customBindingAnnotation = AnnotationUtils.getCustomBindingAnnotation(annotationBinding.getAnnotationType());
        if (customBindingAnnotation != null) {
            Map<String, Object> annotationProperties = AnnotationUtils.resolveAnnotationProperties(customBindingAnnotation, null);
            Map<String, Object> customBindingProperties = AnnotationUtils.resolveAnnotationProperties(annotationBinding, CUSTOM_BINDING_RESERVED_PROPERTIES);
            return createCustomBinding(annotationProperties, customBindingProperties);
        } else if (AnnotationUtils.isCustomBinding(annotationBinding)) {
            Map<String, Object> customBindingProperties = AnnotationUtils.resolveAnnotationProperties(annotationBinding, CUSTOM_BINDING_RESERVED_PROPERTIES);
            return createCustomBinding(customBindingProperties, null);
        } else if (annotationEnum != null) {
            return createBinding(annotationEnum, annotationBinding);
        }
        return null;
    }

    private static Binding createCustomBinding(Map<String, Object> map1, Map<String, Object> map2) {
        final Map<String, Object> mergedMap = new HashMap<>(map1);
        if (map2 != null) {
            map2.forEach(mergedMap::putIfAbsent);
        }
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

        map1.forEach((name, value) -> {
            if (!CUSTOM_BINDING_RESERVED_PROPERTIES.contains(name)) {
                extendBinding.setAttribute(name, value);
            }
        });
        return extendBinding;
    }
}
