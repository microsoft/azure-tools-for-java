package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.KustoQueriesWithTimeIntervalInQueryStringCheck.KustoQueriesVisitor;

// Import necessary libraries
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiVariable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * This class tests the KustoQueriesWithTimeIntervalInQueryStringCheck class.
 *
 * These are some example queries that should be flagged:
 * 1. "String query1 = \"| where timestamp > ago(1d)\";": This query uses the ago function to create a time interval of 1 day. This should be FLAGGED.
 * 2. "String query2 = \"| where timestamp > datetime(2021-01-01)\";": This query compares the timestamp to a specific datetime. This should be FLAGGED.
 * 3. "String query3 = \"| where timestamp > now()\";": This query uses the now function to get the current timestamp. This should be FLAGGED.
 * 4. "String query4 = \"| where timestamp > startofday()\";": This query uses the startofday function to get the start of the current day. This should be FLAGGED.
 * 5. "String query5 = \"| where timestamp > startofmonth()\";": This query uses the startofmonth function to get the start of the current month. This should be FLAGGED.
 * 6. "String query11 = \"| where timestamp > ago(\" + days + \")\";": This query uses a variable to define the time interval. This should be FLAGGED.
 * 7. "String query12 = \"| where timestamp > datetime(\" + date + \")\";": This query uses a variable to define the datetime. This should not be FLAGGED.
 */

public class KustoQueriesWithTimeIntervalInQueryStringCheckTest {

    // Declare as instance variables
    @Mock
    private ProblemsHolder mockHolder;
    private PsiElementVisitor mockVisitor;
    private PsiElement mockElement;

    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
        mockElement = mock(PsiElement.class);
    }

    /**
     * This is the main test method that tests the KustoQueriesWithTimeIntervalInQueryStringCheck class.
     * It tests the KustoQueriesVisitor class by calling the visitElement method with a local variable and a polyadic expression.
     * The local variable is used in a query string or the polyadic expression is used in a query string.
     * The method then checks if a problem is registered when a local variable or a polyadic expression is used in a query string
     * and if the method call is to an Azure client.
     */
    @Test
    public void testKustoQueriesWithTimeIntervalInQueryStringCheck() {

        // assert visitor
        assertVisitor(mockVisitor);

        // verify register problem with local variable as the query string
        verifyRegisterProblemWithLocalVariable();

        // verify register problem with polyadic expression as the query string
        verifyRegisterProblemWithPolyadicExpression();
    }

    /**
     * This test checks the checkExpression method with a null expression as input
     */
    @Test
    public void testCheckExpressionWithNullExpression() {
        // Arrange
        PsiExpression nullExpression = null;

        // Act
        ((KustoQueriesVisitor) mockVisitor).checkExpression(nullExpression, mockElement);
    }

    /**
     * This test checks the checkExpression method with no operands,
     * which may be due to a PolyadicExpressions that was not formed correctly
     */
    @Test
    public void testProcessPsiPolyadicExpressionsWithNoOperands() {

        // Arrange
        Project mockProject = mock(Project.class);
        PsiExpression mockNewExpression = mock(PsiExpression.class);
        PsiPolyadicExpression mockExpression = mock(PsiPolyadicExpression.class);
        PsiElementFactory mockPsiElementFactory = mock(PsiElementFactory.class);
        JavaPsiFacade mockJavaPsiFacade = mock(JavaPsiFacade.class);

        when(mockExpression.getOperands()).thenReturn(new PsiExpression[]{});
        when(mockExpression.getProject()).thenReturn(mockProject);
        when(mockJavaPsiFacade.getInstance(mockProject)).thenReturn(mockJavaPsiFacade);
        when(mockJavaPsiFacade.getElementFactory()).thenReturn(mockPsiElementFactory);
        when(mockPsiElementFactory.createExpressionFromText(eq("datetime(2d)"), isNull())).thenReturn(mockNewExpression);

        // Act
        PsiElement result = ((KustoQueriesVisitor) mockVisitor).processPsiPolyadicExpressions(mockExpression);

        // Assert
        // In this case, we expect that the result is the same as the original expression,
        // because the expression has no operands and therefore nothing was replaced
        assertEquals(mockExpression, result);
    }

    /**
     * Create a visitor by calling the buildVisitor method of KustoQueriesWithTimeIntervalInQueryStringCheck
     * @return PsiElementVisitor
     */
    PsiElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        KustoQueriesVisitor visitor = new KustoQueriesWithTimeIntervalInQueryStringCheck.KustoQueriesVisitor(mockHolder, isOnTheFly);
        return visitor;
    }

    /**
     * Assert that the visitor is not null and is an instance of JavaElementVisitor
     * @param visitor
     */
    void assertVisitor(PsiElementVisitor visitor) {
        assertNotNull(visitor);
        assertTrue(visitor instanceof JavaElementVisitor);
    }


    /**
     * This method tests the registerProblem method with a local variable as the query string
     */
    void verifyRegisterProblemWithLocalVariable() {

        PsiLocalVariable variable = mock(PsiLocalVariable.class);
        PsiLiteralExpression initializer = mock(PsiLiteralExpression.class);
        PsiLocalVariable parentElement = mock(PsiLocalVariable.class);

        PsiMethodCallExpressionImpl methodCall = mock(PsiMethodCallExpressionImpl.class);
        PsiExpressionList argumentList = mock(PsiExpressionList.class);
        PsiReferenceExpression argument = mock(PsiReferenceExpression.class);
        PsiExpression[] arguments = new PsiExpression[]{argument};
        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiVariable resolvedElement = mock(PsiVariable.class);
        PsiElement resolvedElementTwo = mock(PsiElement.class);
        PsiReferenceExpression qualifierExpression = mock(PsiReferenceExpression.class);
        PsiVariable resolvedVariable = mock(PsiVariable.class);
        PsiType type = mock(PsiType.class);

        // stubs for handle local variable method
        when(variable.getInitializer()).thenReturn(initializer);
        when(variable.getName()).thenReturn("stringQuery");

        // stubs for checkExpression method
        when(initializer.getText()).thenReturn("datetime(2022-01-01)");
        when(variable.getParent()).thenReturn(parentElement);
        when(parentElement.getName()).thenReturn("stringQuery");

        // stubs for handleMethodCall method
        when(methodCall.getArgumentList()).thenReturn(argumentList);
        when(argumentList.getExpressions()).thenReturn(arguments);
        when(argument.resolve()).thenReturn(resolvedElement);
        when(resolvedElement.getName()).thenReturn("stringQuery");

        // stubs for isAzureClient method
        when(methodCall.getMethodExpression()).thenReturn(referenceExpression);
        when(referenceExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(qualifierExpression.resolve()).thenReturn(resolvedVariable);
        when(resolvedVariable.getType()).thenReturn(type);
        when(type.getCanonicalText()).thenReturn("com.azure");

        // Visit the variable to store its name if it's a query string
        mockVisitor.visitElement(variable);

        // Visit the method call to check if the query variable is used and the method call is to an Azure client
        mockVisitor.visitElement(methodCall);

        // Verify that the problem was registered correctly for the method call
        verify(mockHolder, times(1)).registerProblem(eq(methodCall), contains("KQL queries with time intervals in the query string detected."));
    }

    void verifyRegisterProblemWithPolyadicExpression() {

        PsiPolyadicExpression polyadicExpression = mock(PsiPolyadicExpression.class);
        PsiReferenceExpression operand = mock(PsiReferenceExpression.class);
        PsiExpression[] operands = new PsiExpression[]{operand};
        PsiVariable resolvedVariable = mock(PsiVariable.class);
        PsiExpression initializer = mock(PsiExpression.class);
        PsiElementFactory elementFactory = mock(PsiElementFactory.class);
        Project project = mock(Project.class);
        JavaPsiFacade javaPsiFacade = mock(JavaPsiFacade.class);
        PsiExpression newExpression = mock(PsiExpression.class);
        PsiLocalVariable parentElement = mock(PsiLocalVariable.class);

        // stubs for handlePolyadicExpression method
        when(polyadicExpression.copy()).thenReturn(polyadicExpression);
        when(polyadicExpression.getText()).thenReturn("datetime(2022-01-01)");

        // stubs for processPsiPolyadicExpressions method
        when(polyadicExpression.getOperands()).thenReturn(operands);
        when(operand.resolve()).thenReturn(resolvedVariable);
        when(resolvedVariable.getInitializer()).thenReturn(initializer);
        when(initializer.getText()).thenReturn("datetime(2022-01-01)");

        when(polyadicExpression.getProject()).thenReturn(project);
        when(JavaPsiFacade.getInstance(polyadicExpression.getProject())).thenReturn(javaPsiFacade);
        when(javaPsiFacade.getElementFactory()).thenReturn(elementFactory);
        when(elementFactory.createExpressionFromText(eq(initializer.getText()), isNull())).thenReturn(newExpression);

        // stubs for checkExpression method
        when(initializer.getText()).thenReturn("datetime(2022-01-01)");
        when(polyadicExpression.getParent()).thenReturn(parentElement);
        when(parentElement.getName()).thenReturn("stringQuery");

        PsiMethodCallExpressionImpl methodCall = mock(PsiMethodCallExpressionImpl.class);
        PsiExpressionList argumentList = mock(PsiExpressionList.class);
        PsiReferenceExpression argument = mock(PsiReferenceExpression.class);
        PsiExpression[] arguments = new PsiExpression[]{argument};
        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiVariable resolvedElement = mock(PsiVariable.class);
        PsiReferenceExpression qualifierExpression = mock(PsiReferenceExpression.class);
        PsiType type = mock(PsiType.class);

        // stubs for handleMethodCall method
        when(methodCall.getArgumentList()).thenReturn(argumentList);
        when(argumentList.getExpressions()).thenReturn(arguments);
        when(argument.resolve()).thenReturn(resolvedElement);
        when(resolvedElement.getName()).thenReturn("stringQuery");

        // stubs for isAzureClient method
        when(methodCall.getMethodExpression()).thenReturn(referenceExpression);
        when(referenceExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(qualifierExpression.resolve()).thenReturn(resolvedVariable);
        when(resolvedVariable.getType()).thenReturn(type);
        when(type.getCanonicalText()).thenReturn("com.azure");

        // Visit the variable to store its name if it's a query string
        mockVisitor.visitElement(polyadicExpression);

        // Visit the method call to check if the query variable is used and the method call is to an Azure client
        mockVisitor.visitElement(methodCall);
    }
}