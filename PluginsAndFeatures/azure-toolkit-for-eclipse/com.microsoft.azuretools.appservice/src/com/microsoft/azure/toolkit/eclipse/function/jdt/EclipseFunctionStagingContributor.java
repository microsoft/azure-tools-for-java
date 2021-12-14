/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.jdt;

import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionMethod;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionProject;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionStagingInitializer;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EclipseFunctionStagingContributor extends FunctionStagingInitializer {
    private static final String AZURE_FUNCTION_ANNOTATION_CLASS =
            "com.microsoft.azure.functions.annotation.FunctionName";

    public EclipseFunctionStagingContributor() {
        super(EclipseFunctionStagingContributor::findAnnotatedMethods, EclipseFunctionStagingContributor::installExtension);
    }

    public static CollectingSearchRequestor searchFunctionNameAnnotation(IType type) throws CoreException {
        IJavaSearchScope scope = JavaSearchScopeFactory.getInstance().createJavaSearchScope(new IJavaElement[] {type},
                false);
        return searchFunctionNameAnnotation(type.getJavaProject(), scope);
    }

    @AzureOperation(
            name = "function.list_function_methods",
            params = {"project.getName()"},
            type = AzureOperation.Type.TASK
    )
    public static List<FunctionMethod> findAnnotatedMethods(FunctionProject project) {
        EclipseFunctionProject myProject = (EclipseFunctionProject) project;
        try {
            final IJavaSearchScope scope = JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(myProject.getEclipseProject(), false);
            return searchFunctionNameAnnotation(myProject.getEclipseProject(), scope).getResults()
                    .stream()
                    .filter(t -> t.getElement() instanceof IMethod)
                    .map(t -> ((IMethod) t.getElement())).distinct()
                    .map(m -> JdtFunctionAdaptor.create(getMethodBinding(m)))
                    .collect(Collectors.toList());

        } catch (CoreException ex) {
            throw new AzureToolkitRuntimeException("Cannot parse azure function annotations", ex);
        }
    }

    public static void installExtension(FunctionProject project) {
        //TODO(andxu): add func install code
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

    private static IMethodBinding getMethodBinding(IMethod method) {
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

    private static boolean isAvailable(ISourceRange range) {
        return range != null && range.getOffset() != -1;
    }
}
