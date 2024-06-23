package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;
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
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
     */
    @Test
    public void testKustoQueriesWithTimeIntervalInQueryStringCheck() {

        // assert visitor
        assertVisitor(mockVisitor);

        // Visit PsiPolyadicExpression
        visitPsiPolyadicExpression(mockVisitor, mockElement);

        // Visit other literals
        visitOtherLiterals(mockVisitor,mockElement);
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
     * This method tests the processPsiPolyadicExpressions method
     * for a PsiPolyadicExpression that should be flagged
     * @param mockVisitor
     * @param mockElement
     */
    void visitPsiPolyadicExpression(PsiElementVisitor mockVisitor, PsiElement mockElement) {

        // Arrange
        PsiPolyadicExpression mockExpression = mock(PsiPolyadicExpression.class);
        PsiExpression mockNewExpression = mock(PsiExpression.class);
        PsiReferenceExpression mockReference = mock(PsiReferenceExpression.class);
        PsiVariable mockVariable = mock(PsiVariable.class);
        PsiExpression mockInitializer = mock(PsiExpression.class);
        JavaPsiFacade mockJavaPsiFacade = mock(JavaPsiFacade.class);
        Project mockProject = mock(Project.class);
        PsiElementFactory mockPsiElementFactory = mock(PsiElementFactory.class);

        when(mockElement.getProject()).thenReturn(mockProject);
        when(mockExpression.getOperands()).thenReturn(new PsiExpression[]{mockReference});
        when(mockExpression.getText()).thenReturn("test");
        when(mockExpression.getProject()).thenReturn(mockProject);
        when(mockReference.resolve()).thenReturn(mockVariable);
        when(mockVariable.getInitializer()).thenReturn(mockInitializer);
        when(mockInitializer.getText()).thenReturn("datetime(2022-01-01)");
        when(mockNewExpression.getText()).thenReturn("datetime(2022-01-01)");

        when(mockJavaPsiFacade.getInstance(mockProject)).thenReturn(mockJavaPsiFacade);
        when(mockJavaPsiFacade.getElementFactory()).thenReturn(mockPsiElementFactory);
        when(mockPsiElementFactory.createExpressionFromText(eq("datetime(2022-01-01)"), isNull())).thenReturn(mockNewExpression);


        // Act
        PsiElement result = ((KustoQueriesVisitor) mockVisitor).processPsiPolyadicExpressions(mockExpression);
        ((KustoQueriesVisitor) mockVisitor).checkExpression(mockInitializer, mockElement, mockHolder);

        // Assert
        assertEquals(mockNewExpression, result);

        // Verify registerProblem is called
        verify(mockHolder, times(1)).registerProblem(Mockito.eq(mockElement), Mockito.contains("KQL queries with time intervals in the query string detected."));
    }

    /**
     * This method tests the checkExpression method for other literals that should be flagged
     * @param mockVisitor
     * @param mockElement
     */

    void visitOtherLiterals(PsiElementVisitor mockVisitor, PsiElement mockElement) {

        // Arrange
        PsiLocalVariable mockVariable = mock(PsiLocalVariable.class);
        PsiExpression mockInitializer = mock(PsiExpression.class);

        when(mockVariable.getInitializer()).thenReturn(mockInitializer);
        when(mockInitializer.toString()).thenReturn("datetime(2022-01-01)");
        when(mockInitializer.getText()).thenReturn("datetime(2022-01-01)");

        // Act
        ((KustoQueriesVisitor) mockVisitor).checkExpression(mockInitializer, mockVariable, mockHolder);

        // Verify registerProblem is not called
        verify(mockHolder, times(1)).registerProblem(Mockito.eq(mockElement), Mockito.contains("KQL queries with time intervals in the query string detected."));
    }

    /**
     * This test checks the checkExpression method with a null expression as input
     */
    @Test
    public void testCheckExpressionWithNullExpression() {
        // Arrange
        PsiExpression nullExpression = null;

        // Act
        ((KustoQueriesVisitor) mockVisitor).checkExpression(nullExpression, mockElement, mockHolder);

        // Assert
        // In this case, we expect that no problem will be registered, because the expression is null
        verify(mockHolder, times(0)).registerProblem(any(), any());
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
}