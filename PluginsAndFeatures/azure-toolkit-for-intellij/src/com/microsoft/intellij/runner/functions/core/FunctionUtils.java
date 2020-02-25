/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.functions.core;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.lang.jvm.JvmAnnotation;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.bindings.Binding;
import com.microsoft.azure.common.function.bindings.BindingEnum;
import com.microsoft.azure.common.function.configurations.FunctionConfiguration;
import com.microsoft.azure.functions.annotation.StorageAccount;
import com.microsoft.azure.maven.common.utils.SneakyThrowUtils;
import com.sun.tools.sjavac.Log;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FunctionUtils {

    private static final String FUNCTION_JSON = "function.json";
    public static final String FUNCTION_JAVA_LIBRARY_ARTIFACT_ID = "azure-functions-java-library";
    private static final String AZURE_FUNCTION_ANNOTATION_CLASS = "com.microsoft.azure.functions.annotation.FunctionName";
    private static final String HTTP_OUTPUT_DEFAULT_NAME = "$return";
    private static final String DEFAULT_HOST_JSON = "{\"version\":\"2.0\",\"extensionBundle\":" +
            "{\"id\":\"Microsoft.Azure.Functions.ExtensionBundle\",\"version\":\"[1.*, 2.0.0)\"}}\n";
    private static final String DEFAULT_LOCAL_SETTINGS_JSON = "{ \"IsEncrypted\": false, \"Values\": " +
            "{ \"FUNCTIONS_WORKER_RUNTIME\": \"java\" } }";

    public static Module[] listFunctionModules(Project project) {
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        return Arrays.stream(modules).filter(m -> {
            final GlobalSearchScope scope = GlobalSearchScope.moduleWithLibrariesScope(m);
            final PsiClass ecClass = JavaPsiFacade.getInstance(project).findClass(AZURE_FUNCTION_ANNOTATION_CLASS, scope);
            return ecClass != null;
        }).toArray(Module[]::new);
    }

    public static Module getFunctionModuleByFilePath(Project project, String moduleFilePath) {
        final Module[] modules = listFunctionModules(project);
        return Arrays.stream(modules)
                .filter(module -> StringUtils.equals(moduleFilePath, module.getModuleFilePath()))
                .findFirst().orElse(null);
    }

    public static boolean isFunctionProject(Project project) {
        final List<Library> libraries = new ArrayList<>();
        OrderEnumerator.orderEntries(project).productionOnly().forEachLibrary(library -> {
            if (StringUtils.contains(library.getName(), FUNCTION_JAVA_LIBRARY_ARTIFACT_ID)) {
                libraries.add(library);
            }
            return true;
        });
        return libraries.size() > 0;
    }

    public static PsiMethod[] findFunctionsByAnnotation(Module module) {
        final Project project = module.getProject();
        final PsiClass functionNameClass = JavaPsiFacade.getInstance(module.getProject())
                .findClass(AZURE_FUNCTION_ANNOTATION_CLASS, GlobalSearchScope.moduleWithLibrariesScope(module));
        final List<PsiMethod> methods = new ArrayList<>(AnnotatedElementsSearch
                .searchPsiMethods(functionNameClass, GlobalSearchScope.allScope(project)).findAll());
        return methods.toArray(new PsiMethod[0]);
    }

    public static final Path getDefaultHostJson(Project project) {
        return new File(project.getBasePath(), "host.json").toPath();
    }

    public static final Path createTempleHostJson() {
        try {
            final File result = File.createTempFile("host", ".json");
            FileUtils.write(result, DEFAULT_HOST_JSON, Charset.defaultCharset());
            return result.toPath();
        } catch (IOException e) {
            return null;
        }
    }

    public static void prepareStagingFolderForDeploy(Path stagingFolder, Path hostJson, Module module, Map<String, String> appSettings,
                                                     PsiMethod[] methods) throws AzureExecutionException, IOException {
        prepareStagingFolder(stagingFolder, hostJson, null, module, appSettings, methods);
    }

    private static void copyFilesWithDefaultContent(Path sourcePath, File dest, String defaultContent) throws IOException {
        final File src = sourcePath == null ? null : sourcePath.toFile();
        if (src != null && src.exists()) {
            FileUtils.copyFile(src, dest);
        } else {
            FileUtils.write(src, defaultContent, Charset.defaultCharset());
        }
    }

    private static void updateLocalSettingValues(File target, Map<String, String> appSettings) throws IOException {
        final Gson gson = JsonUtils.getGson();
        final JsonObject jsonObject = gson.fromJson(FileUtils.readFileToString(target), JsonObject.class);
        final JsonObject valueObject = new JsonObject();
        appSettings.entrySet().forEach(entry -> valueObject.addProperty(entry.getKey(), entry.getValue()));
        jsonObject.add("Values", valueObject);
        JsonUtils.writeJsonToFile(target, jsonObject);
    }

    public static void prepareStagingFolder(Path stagingFolder, Path hostJson, Path localSettingJson, Module module,
                                            Map<String, String> appSettings, PsiMethod[] methods) throws AzureExecutionException, IOException {
        final Map<String, FunctionConfiguration> configMap = generateConfigurations(methods);
        final Path jarFile = JarUtils.buildJarFileToStagingPath(stagingFolder.toString(), module);
        final String scriptFilePath = "../" + jarFile.getFileName().toString();
        configMap.values().forEach(config -> config.setScriptFile(scriptFilePath));
        for (final Map.Entry<String, FunctionConfiguration> config : configMap.entrySet()) {
            if (StringUtils.isNotBlank(config.getKey())) {
                final File functionJsonFile = Paths.get(stagingFolder.toString(), config.getKey(), FUNCTION_JSON)
                        .toFile();
                FunctionJsonWriter.writeFunctionJsonFile(functionJsonFile, config.getValue());
            }
        }

        final File hostJsonFile = new File(stagingFolder.toFile(), "host.json");
        copyFilesWithDefaultContent(hostJson, hostJsonFile, DEFAULT_HOST_JSON);
        final File localSettingsFile = new File(stagingFolder.toFile(), "local.settings.json");
        copyFilesWithDefaultContent(localSettingJson, localSettingsFile, DEFAULT_LOCAL_SETTINGS_JSON);
        if (MapUtils.isNotEmpty(appSettings)) {
            updateLocalSettingValues(localSettingsFile, appSettings);
        }

        final List<File> jarFiles = new ArrayList<>();
        OrderEnumerator.orderEntries(module).productionOnly().forEachLibrary(lib -> {
            if (StringUtils.contains(lib.getName(), FUNCTION_JAVA_LIBRARY_ARTIFACT_ID)) {
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
    }

    public static String getTargetFolder(Module module) {
        if (module == null) {
            return StringUtils.EMPTY;
        }
        final Project project = module.getProject();
        final MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(module);
        final String functionAppName = mavenProject == null ? null : mavenProject.getProperties().getProperty("functionAppName");;
        final String stagingFolderName = StringUtils.isEmpty(functionAppName) ? module.getName() : functionAppName;
        return Paths.get(project.getBasePath(), "target", "azure-functions", stagingFolderName).toString();
    }

    public static String getFuncPath() throws IOException, InterruptedException {
        return FunctionCliResolver.resolveFunc();
    }

    private static String stripExtraCharacters(String fileName) {
        // TODO-dp this is not robust enough (eliminated !/ at the end of the jar)
        if (fileName.endsWith("!/")) {
            fileName = fileName.substring(0, fileName.length() - 2);
        }
        return fileName;
    }

    private static Map<String, FunctionConfiguration> generateConfigurations(final PsiMethod[] methods)
            throws AzureExecutionException {
        final Map<String, FunctionConfiguration> configMap = new HashMap<>();
        for (final PsiMethod method : methods) {
            final PsiAnnotation annotation = AnnotationUtil.findAnnotation(method,
                    FunctionUtils.AZURE_FUNCTION_ANNOTATION_CLASS);
            final PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
            String functionName = null;
            for (final PsiNameValuePair attribute : attributes) {
                final PsiAnnotationMemberValue value = attribute.getValue();
                final String name = attribute.getAttributeName();
                if ("value".equals(name)) {
                    functionName = AnnotationHelper.getPsiAnnotationMemberValue(value);
                    break;
                }
            }
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
            bindings.addAll(parseAnnotations(param.getAnnotations()));
        }
    }

    private static List<Binding> parseAnnotations(JvmAnnotation[] annos) throws AzureExecutionException {
        final List<Binding> bindings = new ArrayList<>();

        for (final JvmAnnotation annotation : annos) {
            final Binding binding = getBinding(annotation);
            if (binding != null) {
                Log.debug("Adding binding: " + binding.toString());
                bindings.add(binding);
            }
        }

        return bindings;
    }

    private static Binding getBinding(JvmAnnotation annotation) throws AzureExecutionException {
        final BindingEnum annotationEnum = Arrays.stream(BindingEnum.values()).filter((bindingEnum) -> {
            return bindingEnum.name().toLowerCase(Locale.ENGLISH)
                    .equals(FilenameUtils.getExtension(annotation.getQualifiedName()).toLowerCase(Locale.ENGLISH));
        }).findFirst().orElse(null);
        return annotationEnum == null ? getUserDefinedBinding(annotation)
                : createBinding(annotationEnum, (PsiAnnotation) annotation);
    }

    private static Binding getUserDefinedBinding(JvmAnnotation annotation) {
        // unsupported now
        return null;
    }

    private static void processMethodAnnotations(final PsiMethod method, final List<Binding> bindings)
            throws AzureExecutionException {
        if (!method.getReturnType().equals(Void.TYPE)) {
            bindings.addAll(parseAnnotations(method.getAnnotations()));

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
            System.out.println("StorageAccount annotation found.");
            storageAccount.getAttributes().forEach(t -> {
                if (t.getAttributeName().equals("value")) {
                    try {
                        final String connectionString = AnnotationHelper.getJvmAnnotationAttributeValue(t.getAttributeValue());
                        bindings.stream().filter(binding -> binding.getBindingEnum().isStorage())
                                .filter(binding -> StringUtils.isEmpty((String) binding.getAttribute("connection")))
                                .forEach(binding -> binding.setAttribute("connection", connectionString));
                    } catch (AzureExecutionException e) {
                        SneakyThrowUtils.sneakyThrow(e);
                    }

                }
            });
            // Replace empty connection string

        } else {
            System.out.println("No StorageAccount annotation found.");
        }
    }

    private static List<String> executeMultipLineOutput(String cmd) throws IOException, InterruptedException {
        final List<String> result = new ArrayList<>();
        final Process process = Runtime.getRuntime().exec(cmd);
        final StringBuffer ret = new StringBuffer();
        try (final InputStream inputStream = process.getInputStream();
             final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             final BufferedReader br = new BufferedReader(inputStreamReader)) {
            String tmp;
            while ((tmp = br.readLine()) != null) {
                result.add(tmp);
            }
        }
        if (process.waitFor() != 0) {
            throw new IOException("Command execute fail.");
        }
        return result;
    }

    private static Binding createBinding(BindingEnum bindingEnum, PsiAnnotation annotation) throws AzureExecutionException {
        final Binding binding = new Binding(bindingEnum);
        final PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        for (final PsiNameValuePair attribute : attributes) {
            final PsiAnnotationMemberValue value = attribute.getValue();
            final String name = attribute.getAttributeName();
            if (value instanceof PsiArrayInitializerMemberValue) {
                final PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue) value)
                        .getInitializers();
                final List<String> result = Lists.newArrayListWithCapacity(initializers.length);

                for (final PsiAnnotationMemberValue initializer : initializers) {
                    result.add(AnnotationHelper.getPsiAnnotationMemberValue(initializer));
                }
                binding.setAttribute(name, result.toArray(new String[0]));
            } else {
                final String valueText = AnnotationHelper.getPsiAnnotationMemberValue(value);
                binding.setAttribute(name, valueText);
            }
        }
        return binding;
    }
}
