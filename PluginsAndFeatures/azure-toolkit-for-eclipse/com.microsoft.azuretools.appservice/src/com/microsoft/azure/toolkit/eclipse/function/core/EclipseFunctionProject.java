/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.core;

import com.microsoft.azure.toolkit.eclipse.function.utils.AnnotationUtils;
import com.microsoft.azure.toolkit.eclipse.function.utils.JarUtils;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotation;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotationClass;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionMethod;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionProject;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandlerImpl;
import com.microsoft.azuretools.core.utils.MavenUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class EclipseFunctionProject extends FunctionProject {
    private static final String FUNCTION_JAVA_LIBRARY_ARTIFACT_ID = "azure-functions-java-library";
    private static final String AZURE_FUNCTION_ANNOTATION_CLASS =
            "com.microsoft.azure.functions.annotation.FunctionName";

    private final IJavaProject eclipseProject;

    public EclipseFunctionProject(IJavaProject project, File stagingFolder) {
        this.eclipseProject = project;
        setName(project.getElementName());
        setBaseDirectory(project.getProject().getLocation().toFile());
        setStagingFolder(stagingFolder);
    }

    public void buildJar() throws Exception {
        IFile pom = MavenUtils.getPomFile(eclipseProject.getProject());
        final MavenProject mavenProject = MavenUtils.toMavenProject(pom);

        final List<File> jarFiles = new ArrayList<>();
        mavenProject.getArtifacts().forEach(t -> {
            if (!StringUtils.equals(t.getScope(), "test") && !StringUtils.contains(t.getArtifactId(), FUNCTION_JAVA_LIBRARY_ARTIFACT_ID)) {
                jarFiles.add(t.getFile());
            }
        });
        setClassesOutputDirectory(new File(mavenProject.getBuild().getOutputDirectory()));
        setDependencies(jarFiles);
        final File jarFile = JarUtils.buildJarFileToTempFile(this);
        this.setArtifactFile(jarFile);
    }

    public IJavaProject getEclipseProject() {
        return eclipseProject;
    }

    public static CollectingSearchRequestor searchFunctionNameAnnotation(IType type) throws CoreException {
        IJavaSearchScope scope = JavaSearchScopeFactory.getInstance().createJavaSearchScope(new IJavaElement[]{type},
                false);
        return searchFunctionNameAnnotation(type.getJavaProject(), scope);
    }

    @AzureOperation(
            name = "function.list_function_methods",
            params = {"project.getName()"},
            type = AzureOperation.Type.TASK
    )
    @Override
    public List<FunctionMethod> findAnnotatedMethods() {
        try {
            final IJavaSearchScope scope = JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(getEclipseProject(), false);
            return searchFunctionNameAnnotation(getEclipseProject(), scope).getResults()
                    .stream()
                    .filter(t -> t.getElement() instanceof IMethod)
                    .map(t -> ((IMethod) t.getElement())).distinct()
                    .map(m -> create(getMethodBinding(m)))
                    .collect(Collectors.toList());

        } catch (CoreException ex) {
            throw new AzureToolkitRuntimeException("Cannot parse azure function annotations", ex);
        }
    }

    @Override
    public void installExtension(String funcPath) {
        // need to refactor this code using 'func path'
        final CommandHandler commandHandler = new CommandHandlerImpl();
        final FunctionCoreToolsHandler functionCoreToolsHandler = new FunctionCoreToolsHandlerImpl(commandHandler);
        try {
            functionCoreToolsHandler.installExtension(getStagingFolder(), getBaseDirectory());
        } catch (AzureExecutionException e) {
            throw new AzureToolkitRuntimeException("Cannot run install on Azure Function staging folder:" + getStagingFolder(), e);
        }
    }

    public static CollectingSearchRequestor searchFunctionNameAnnotation(IJavaProject javaProject, IJavaSearchScope scope) throws CoreException {
        CollectingSearchRequestor requester = new CollectingSearchRequestor();
        IType type = javaProject.findType(AZURE_FUNCTION_ANNOTATION_CLASS);
        if (type != null) {

            SearchPattern pattern = SearchPattern.createPattern(type,
                    IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE,
                    SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);

            new SearchEngine().search(pattern, new SearchParticipant[]{SearchEngine
                    .getDefaultSearchParticipant()}, scope, requester, new NullProgressMonitor());
        }
        return requester;
    }

    private IMethodBinding getMethodBinding(IMethod method) {
        IType declaringType = method.getDeclaringType();
        ASTParser parser = ASTParser.newParser(AST.JLS16);
        try {
            if (declaringType.getCompilationUnit() != null) {
                parser.setSource(declaringType.getCompilationUnit());
            } else if (!isAvailable(declaringType.getSourceRange())) {
                parser.setProject(declaringType.getJavaProject());
                IBinding[] bindings = parser.createBindings(new IJavaElement[]{declaringType}, new NullProgressMonitor());
                if (bindings.length == 0) {
                    throw new AzureToolkitRuntimeException(String.format("Cannot resolve binding for method '%s'", method.getElementName()));
                }
                if (bindings[0] instanceof ITypeBinding) {
                    ITypeBinding classBinding = (ITypeBinding) bindings[0];
                    return Arrays.stream(classBinding.getDeclaredMethods()).filter(t -> isSameMethod(method, t)).findAny().orElse(null);
                }
                throw new AzureToolkitRuntimeException(String.format("Illegal binding for method '%s'", method.getElementName()));
            } else {
                parser.setSource(declaringType.getClassFile());
            }
        } catch (JavaModelException ex) {
            throw new AzureToolkitRuntimeException(String.format("Cannot parse source for method '%s'", method.getElementName()), ex);
        }
        parser.setIgnoreMethodBodies(true);
        parser.setResolveBindings(true);

        CompilationUnit root = (CompilationUnit) parser.createAST(new NullProgressMonitor());
        MethodDeclaration node = (MethodDeclaration) root.findDeclaringNode(method.getKey());
        return node.resolveBinding();
    }

    private boolean isSameMethod(IMethod method, IMethodBinding methodBinding) {
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

    private FunctionMethod create(IMethodBinding method) {
        FunctionMethod functionMethod = new FunctionMethod();
        functionMethod.setName(method.getName());
        functionMethod.setReturnTypeName(method.getReturnType().getQualifiedName());
        functionMethod.setAnnotations(method.getAnnotations() == null ? Collections.emptyList() :
                Arrays.stream(method.getAnnotations()).map(EclipseFunctionProject::create).collect(Collectors.toList()));

        int len = method.getParameterTypes().length;
        List<FunctionAnnotation[]> parameterAnnotations = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            IAnnotationBinding[] paramAnnotations = method.getParameterAnnotations(i);
            if (paramAnnotations != null) {
                parameterAnnotations.add(Arrays.stream(paramAnnotations).map(EclipseFunctionProject::create).toArray(FunctionAnnotation[]::new));
            }
        }
        functionMethod.setParameterAnnotations(parameterAnnotations);
        functionMethod.setDeclaringTypeName(method.getDeclaringClass().getQualifiedName());
        return functionMethod;
    }

    private static FunctionAnnotation create(@Nonnull IAnnotationBinding obj) {
        return create(obj, true);
    }

    private static FunctionAnnotation create(@Nonnull IAnnotationBinding obj, boolean resolveAnnotationType) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> defaultMap = new HashMap<>();

        for (IMemberValuePairBinding pair : obj.getDeclaredMemberValuePairs()) {
            map.put(pair.getName(), AnnotationUtils.calculateJdtValue(pair.getValue()));
        }

        for (IMemberValuePairBinding pair : obj.getAllMemberValuePairs()) {
            defaultMap.put(pair.getName(), AnnotationUtils.calculateJdtValue(pair.getValue()));
        }

        FunctionAnnotation functionAnnotation = new FunctionAnnotation();
        functionAnnotation.setAnnotationClass(toFunctionAnnotationClass(obj.getAnnotationType(), resolveAnnotationType));

        functionAnnotation.setProperties(map);
        functionAnnotation.setDefaultProperties(defaultMap);
        return functionAnnotation;
    }

    private static FunctionAnnotationClass toFunctionAnnotationClass(ITypeBinding type, boolean resolveAnnotationType) {
        FunctionAnnotationClass res = new FunctionAnnotationClass();
        res.setFullName(type.getQualifiedName());
        res.setName(type.getName());
        if (resolveAnnotationType) {
            res.setAnnotations(Arrays.stream(type.getAnnotations()).map(a -> create(a, false)).collect(Collectors.toList()));
        }
        return res;
    }

    private String removeGeneric(String signature) {
        return signature.replaceAll("<.*>", "");
    }

    private boolean isAvailable(ISourceRange range) {
        return range != null && range.getOffset() != -1;
    }
}
