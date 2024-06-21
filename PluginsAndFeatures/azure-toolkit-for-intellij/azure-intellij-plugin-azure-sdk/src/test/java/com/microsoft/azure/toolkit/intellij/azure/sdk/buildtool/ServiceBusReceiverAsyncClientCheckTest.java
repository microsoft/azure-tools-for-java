package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;


// Import necessary libraries
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiTypeElement;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiType;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


public class ServiceBusReceiverAsyncClientCheckTest {

    // Create a mock ProblemsHolder to be used in the test
    private ProblemsHolder mockHolder;

    // Create a Java code snippet that uses ServiceBusReceiverAsyncClient with situations that should be flagged
    private String createJavaCode() {
        StringBuilder javaCode = new StringBuilder();

        javaCode.append("public class TestClass {\n");

        // Define the client as a private final member
        javaCode.append("\tprivate final ServiceBusReceiverAsyncClient client;\n");

        // Create a constructor for TestClass
        javaCode.append("\tpublic TestClass() {\n");
        javaCode.append("\t\tthis.client = new ServiceBusReceiverAsyncClient();\n");
        javaCode.append("\t}\n");

        // Create a getClient method to access the client
        javaCode.append("\tpublic ServiceBusReceiverAsyncClient getClient() {\n");
        javaCode.append("\t\treturn this.client;\n");
        javaCode.append("\t}\n");

        javaCode.append("}");

        return javaCode.toString();
    }


    @Test
    public void testBuildVisitor() {
        // Arrange
        String javaCode = createJavaCode();
        PsiElementVisitor visitor = createVisitor();

        // Mock PsiFileFactory
        PsiFileFactory psiFileFactory = mockPsiFileFactory();

        // Create PsiFile
        PsiFile psiFile = createPsiFile(javaCode, psiFileFactory);

        // Accept visitor
        psiFile.accept(visitor);

        // Assert
        assertVisitor(visitor);

        // Visit Type Element
        visitTypeElement(visitor);
    }

    // Create a visitor by calling the buildVisitor method of ServiceBusReceiverAsyncClientCheck
    private PsiElementVisitor createVisitor() {
        ServiceBusReceiverAsyncClientCheck check = new ServiceBusReceiverAsyncClientCheck();
        mockHolder = mock(ProblemsHolder.class);
        boolean isOnTheFly = true;

        return check.buildVisitor(mockHolder, isOnTheFly);
    }

    // Mock PsiFileFactory and create a PsiFile to be used in the test
    private PsiFileFactory mockPsiFileFactory() {
        Project mockProject = mock(Project.class);
        PsiFileFactory mockFileFactory = mock(PsiFileFactory.class);
        when(PsiFileFactory.getInstance(mockProject)).thenReturn(mockFileFactory);

        return PsiFileFactory.getInstance(mockProject);
    }

    // Create a PsiFile from the Java code to simulate the file to be inspected
    private PsiFile createPsiFile(String javaCode, PsiFileFactory psiFileFactory) {
        PsiFile mockFile = mock(PsiFile.class);
        when(psiFileFactory.createFileFromText(anyString(), any(FileType.class), any())).thenReturn(mockFile);

        return psiFileFactory.createFileFromText(javaCode, JavaFileType.INSTANCE, javaCode);
    }


    // Assert that the visitor is not null and is an instance of JavaElementVisitor
    private void assertVisitor(PsiElementVisitor visitor) {
        assertNotNull(visitor);
        assertTrue(visitor instanceof JavaElementVisitor);
    }

    // Visit a Type Element and verify that a problem was registered
    private void visitTypeElement(PsiElementVisitor visitor) {
        PsiType mockType = mock(PsiType.class);
        when(mockType.getPresentableText()).thenReturn("ServiceBusReceiverAsyncClient");

        PsiTypeElement mockTypeElement = mock(PsiTypeElement.class);
        when(mockTypeElement.getType()).thenReturn(mockType);

        ((JavaElementVisitor) visitor).visitTypeElement(mockTypeElement);

        verify(mockHolder, times(1)).registerProblem((Mockito.eq(mockTypeElement)), Mockito.contains("Use of ServiceBusReceiverAsyncClient detected. Use ServiceBusProcessorClient instead."));
    }
}