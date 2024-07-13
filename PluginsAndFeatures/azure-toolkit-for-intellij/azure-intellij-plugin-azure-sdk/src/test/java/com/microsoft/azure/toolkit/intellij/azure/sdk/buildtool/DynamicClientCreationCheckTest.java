package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.DynamicClientCreationCheck.DynamicClientCreationVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DynamicClientCreationCheckTest {

    // Declare as instance variables
    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiForStatement mockElement;

    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
        mockElement = mock(PsiForStatement.class);
    }

    /**
     * This is the main test method that tests the KustoQueriesWithTimeIntervalInQueryStringCheck class.
     *
     * This tests blocks of code that are a PsiExpressionStatement in the checkClientCreation method.
     */
    @Test
    void testDynamicClientCreationWithExpressionStatement() {

        int numOfInvocations = 1;
        String methodName = "buildClient";
        String packageName = "com.azure.";

        // verify register problem with assignment expression
        verifyRegisterProblemWithAssignmentExpression(methodName, packageName, numOfInvocations);

        // verify register problem with declaration statement
        verifyRegisterProblemWithDeclarationStatement(methodName, packageName, numOfInvocations);
    }

    /**
     * This is a test case that verifies the behavior of the DynamicClientCreationCheck when
     * it encounters a code block that does not match the criteria for dynamic client creation.
     * This involves a method call on an object that is not part of the com.azure package
     */
    @Test // unhappy path
     void testDynamicClientCreationWithNonAzurePackage() {

        int numOfInvocations = 0;
        String methodName = "buildClient";
        String packageName = "com.Notazure.";

        // verify register problem with assignment expression
        verifyRegisterProblemWithAssignmentExpression(methodName, packageName, numOfInvocations);

        // verify register problem with declaration statement
        verifyRegisterProblemWithDeclarationStatement(methodName, packageName, numOfInvocations);
    }

    /**
     * This is a test case that verifies the behavior of the DynamicClientCreationCheck when
     * it encounters a code block that does not match the criteria for dynamic client creation.
     * This involves a method call that is not part of the METHODS_TO_CHECK list.
     */
    @Test
    void testDynamicClientCreationWithNonBuildMethod() {

        int numOfInvocations = 0;
        String methodName = "NotbuildClient";
        String packageName = "com.azure.";

        // verify register problem with assignment expression
        verifyRegisterProblemWithAssignmentExpression(methodName, packageName, numOfInvocations);

        // verify register problem with declaration statement
        verifyRegisterProblemWithDeclarationStatement(methodName, packageName, numOfInvocations);
    }

    /**
     * This helper method creates a new instance of the DynamicClientCreationVisitor class.
     */
    private JavaElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        DynamicClientCreationVisitor mockVisitor = new DynamicClientCreationVisitor(mockHolder, isOnTheFly);
        return mockVisitor;
    }

    /**
     * This method verifies that a problem is registered when a client creation method is found
     * building a client from the com.azure package in an assignment expression.
     *
     * @param methodName this is the method name that is being checked for
     * @param packageName this is the package name that is being checked for
     * @param numOfInvocations this is the number of times the registerProblem method should be called
     */
    private void verifyRegisterProblemWithAssignmentExpression(String methodName, String packageName, int numOfInvocations) {

        // main method
        PsiStatement statement = mock(PsiStatement.class);
        PsiBlockStatement body = mock(PsiBlockStatement.class);
        PsiCodeBlock codeBlock = mock(PsiCodeBlock.class);
        PsiExpressionStatement blockChild = mock(PsiExpressionStatement.class);
        PsiStatement[] blockStatements = new PsiStatement[] { blockChild };

        // checkClientCreation method
        PsiAssignmentExpression expression = mock(PsiAssignmentExpression.class);
        PsiMethodCallExpression rhs = mock(PsiMethodCallExpression.class);

        // isClientCreationMethod
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);
        PsiExpression qualifierExpression = mock(PsiExpression.class);
        PsiType type = mock(PsiType.class);

        // main method
        when(mockElement.getBody()).thenReturn(body);
        when(body.getCodeBlock()).thenReturn(codeBlock);
        when(codeBlock.getStatements()).thenReturn(blockStatements);

        // checkClientCreation method
        when(blockChild.getExpression()).thenReturn(expression);
        when(expression.getRExpression()).thenReturn(rhs);

        // isClientCreationMethod
        when(rhs.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.getReferenceName()).thenReturn(methodName);
        when(methodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(qualifierExpression.getType()).thenReturn(type);
        when(qualifierExpression.getType().getCanonicalText()).thenReturn(packageName);

        mockVisitor.visitForStatement(mockElement);

        //  Verify problem is registered
        verify(mockHolder,
                times(numOfInvocations)).registerProblem(Mockito.eq(rhs),
                Mockito.contains("Dynamic client creation detected. Create a single client instance and reuse it instead."));
    }

    /**
     * This method verifies that a problem is registered when a client creation method is found
     * building a client from the com.azure package in a declaration statement.
     * @param methodName this is the method name that is being checked for
     * @param packageName this is the package name that is being checked for
     * @param numOfInvocations this is the number of times the registerProblem method should be called
     */
    private void verifyRegisterProblemWithDeclarationStatement(String methodName, String packageName, int numOfInvocations) {

        // main method
        PsiStatement statement = mock(PsiStatement.class);
        PsiBlockStatement body = mock(PsiBlockStatement.class);
        PsiCodeBlock codeBlock = mock(PsiCodeBlock.class);
        PsiDeclarationStatement blockChild = mock(PsiDeclarationStatement.class);
        PsiStatement[] blockStatements = new PsiStatement[] { blockChild };

        // checkClientCreation method
        PsiLocalVariable declaredElement = mock(PsiLocalVariable.class);
        PsiElement[] declaredElements = new PsiElement[] { declaredElement };
        PsiMethodCallExpression initializer = mock(PsiMethodCallExpression.class);

        // isClientCreationMethod
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);
        PsiExpression qualifierExpression = mock(PsiExpression.class);
        PsiType type = mock(PsiType.class);

        // main method
        when(mockElement.getBody()).thenReturn(body);
        when(body.getCodeBlock()).thenReturn(codeBlock);
        when(codeBlock.getStatements()).thenReturn(blockStatements);

        // checkClientCreation method
        when(blockChild.getDeclaredElements()).thenReturn(declaredElements);
        when(declaredElement.getInitializer()).thenReturn(initializer);

        // isClientCreationMethod
        when(initializer.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.getReferenceName()).thenReturn(methodName);
        when(methodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(qualifierExpression.getType()).thenReturn(type);
        when(qualifierExpression.getType().getCanonicalText()).thenReturn(packageName);

        mockVisitor.visitForStatement(mockElement);

        //  Verify problem is registered
        verify(mockHolder,
                times(numOfInvocations)).registerProblem(Mockito.eq(initializer),
                Mockito.contains("Dynamic client creation detected. Create a single client instance and reuse it instead."));
    }
}
