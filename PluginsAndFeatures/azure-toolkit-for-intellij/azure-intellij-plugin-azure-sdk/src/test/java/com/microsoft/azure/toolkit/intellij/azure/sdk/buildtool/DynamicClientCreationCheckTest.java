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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        // assert visitor
        assertVisitor();

        int NUMBER_OF_INVOCATIONS = 1;
        String METHOD_NAME = "buildClient";
        String AZURE_PACKAGE = "com.azure.";

        // verify register problem with assignment expression
        verifyRegisterProblemWithAssignmentExpression(METHOD_NAME, AZURE_PACKAGE, NUMBER_OF_INVOCATIONS);

        // verify register problem with declaration statement
        verifyRegisterProblemWithDeclarationStatement(METHOD_NAME, AZURE_PACKAGE, NUMBER_OF_INVOCATIONS);
    }

    /**
     * This is a test case that verifies the behavior of the DynamicClientCreationCheck when
     * it encounters a code block that does not match the criteria for dynamic client creation.
     * This involves a method call on an object that is not part of the com.azure package
     */
    @Test // unhappy path
     void testDynamicClientCreationWithNonAzurePackage() {
        // assert visitor
        assertVisitor();

        int NUMBER_OF_INVOCATIONS = 0;
        String METHOD_NAME = "buildClient";
        String AZURE_PACKAGE = "com.Notazure.";

        // verify register problem with assignment expression
        verifyRegisterProblemWithAssignmentExpression(METHOD_NAME, AZURE_PACKAGE, NUMBER_OF_INVOCATIONS);

        // verify register problem with declaration statement
        verifyRegisterProblemWithDeclarationStatement(METHOD_NAME, AZURE_PACKAGE, NUMBER_OF_INVOCATIONS);
    }

    /**
     * This is a test case that verifies the behavior of the DynamicClientCreationCheck when
     * it encounters a code block that does not match the criteria for dynamic client creation.
     * This involves a method call that is not part of the METHODS_TO_CHECK list.
     */
    @Test // unhappy path
    void testDynamicClientCreationWithNonBuildMethod() {
        // assert visitor
        assertVisitor();

        int NUMBER_OF_INVOCATIONS = 0;
        String METHOD_NAME = "NotbuildClient";
        String AZURE_PACKAGE = "com.azure.";

        // verify register problem with assignment expression
        verifyRegisterProblemWithAssignmentExpression(METHOD_NAME, AZURE_PACKAGE, NUMBER_OF_INVOCATIONS);

        // verify register problem with declaration statement
        verifyRegisterProblemWithDeclarationStatement(METHOD_NAME, AZURE_PACKAGE, NUMBER_OF_INVOCATIONS);
    }

    /**
     * This helper method creates a new instance of the DynamicClientCreationVisitor class.
     */
    JavaElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        DynamicClientCreationVisitor mockVisitor = new DynamicClientCreationVisitor(mockHolder, isOnTheFly);
        return mockVisitor;
    }

    /**
     * This helper method asserts that the mockVisitor is not null and is an instance of the JavaElementVisitor class.
     */
    void assertVisitor() {
        // assert visitor
        assertNotNull(mockVisitor);
        assertTrue(mockVisitor instanceof JavaElementVisitor);
    }

    /**
     * This method verifies that a problem is registered when a client creation method is found
     * building a client from the com.azure package in an assignment expression.
     *
     * @param METHOD_NAME
     * @param AZURE_PACKAGE
     * @param NUMBER_OF_INVOCATIONS
     */
    void verifyRegisterProblemWithAssignmentExpression(String METHOD_NAME, String AZURE_PACKAGE, int NUMBER_OF_INVOCATIONS) {

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
        when(methodExpression.getReferenceName()).thenReturn(METHOD_NAME);
        when(methodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(qualifierExpression.getType()).thenReturn(type);
        when(qualifierExpression.getType().getCanonicalText()).thenReturn(AZURE_PACKAGE);

        mockVisitor.visitForStatement(mockElement);

        //  Verify problem is registered
        verify(mockHolder,
                times(NUMBER_OF_INVOCATIONS)).registerProblem(Mockito.eq(rhs),
                Mockito.contains("Dynamic client creation detected. Create a single client instance and reuse it instead."));
    }

    /**
     * This method verifies that a problem is registered when a client creation method is found
     * building a client from the com.azure package in a declaration statement.
     * @param METHOD_NAME
     * @param AZURE_PACKAGE
     * @param NUMBER_OF_INVOCATIONS
     */
    void verifyRegisterProblemWithDeclarationStatement(String METHOD_NAME, String AZURE_PACKAGE, int NUMBER_OF_INVOCATIONS) {

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
        when(methodExpression.getReferenceName()).thenReturn(METHOD_NAME);
        when(methodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(qualifierExpression.getType()).thenReturn(type);
        when(qualifierExpression.getType().getCanonicalText()).thenReturn(AZURE_PACKAGE);

        mockVisitor.visitForStatement(mockElement);

        //  Verify problem is registered
        verify(mockHolder,
                times(NUMBER_OF_INVOCATIONS)).registerProblem(Mockito.eq(initializer),
                Mockito.contains("Dynamic client creation detected. Create a single client instance and reuse it instead."));
    }
}
