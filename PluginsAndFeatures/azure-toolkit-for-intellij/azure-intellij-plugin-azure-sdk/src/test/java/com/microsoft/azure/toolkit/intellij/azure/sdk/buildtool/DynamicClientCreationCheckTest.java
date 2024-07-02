package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.DynamicClientCreationCheck.DynamicClientCreationVisitor;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
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
    public void testDynamicClientCreationWithExpressionStatement() {

        // assert visitor
        assertVisitor();

        int NUMBER_OF_INVOCATIONS = 1;
        String METHOD_NAME = "buildClient";
        String AZURE_PACKAGE = "com.azure.";

        // verify register problem
        verifyRegisterProblem(METHOD_NAME, AZURE_PACKAGE, NUMBER_OF_INVOCATIONS);
    }

    JavaElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        DynamicClientCreationVisitor mockVisitor = new DynamicClientCreationVisitor(mockHolder, isOnTheFly);
        return mockVisitor;
    }

    void assertVisitor() {
        // assert visitor
        assertNotNull(mockVisitor);
        assertTrue(mockVisitor instanceof JavaElementVisitor);
    }

    void verifyRegisterProblem(String METHOD_NAME, String AZURE_PACKAGE, int NUMBER_OF_INVOCATIONS) {

        // main method
        PsiStatement statement = mock(PsiStatement.class);
        PsiStatement body = mock(PsiStatement.class);
        PsiBlockStatement blockStatement = mock(PsiBlockStatement.class);
        PsiCodeBlock codeBlock = mock(PsiCodeBlock.class);
        PsiExpressionStatement blockChild = mock(PsiExpressionStatement.class);
        PsiStatement[] blockStatements = new PsiStatement[] { blockChild };

        // check client creation
        PsiAssignmentExpression expression = mock(PsiAssignmentExpression.class);
        PsiMethodCallExpression rhs = mock(PsiMethodCallExpression.class);

        // isClientCreation method
        PsiMethodCallExpression methodCallExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);
        PsiExpression qualifierExpression = mock(PsiExpression.class);
        PsiType type = mock(PsiType.class);


        // main method
        when(mockElement.getBody()).thenReturn(blockStatement);
        when(blockStatement.getCodeBlock()).thenReturn(codeBlock);
        when(codeBlock.getStatements()).thenReturn(blockStatements);

        // check client creation
        when(blockChild.getExpression()).thenReturn(expression);
        when(expression.getRExpression()).thenReturn(rhs);

        // isClientCreation method
        when(methodCallExpression.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.getReferenceName()).thenReturn(METHOD_NAME);
        when(methodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(qualifierExpression.getType()).thenReturn(type);
        when(type.getCanonicalText()).thenReturn(AZURE_PACKAGE);

        mockVisitor.visitForStatement(mockElement);

        // Verify problem is registered
        verify(mockHolder,
                times(NUMBER_OF_INVOCATIONS)).registerProblem(Mockito.eq(methodCallExpression),
                Mockito.contains("Dynamic client creation detected. Create a single client instance and reuse it instead."));
    }
}
