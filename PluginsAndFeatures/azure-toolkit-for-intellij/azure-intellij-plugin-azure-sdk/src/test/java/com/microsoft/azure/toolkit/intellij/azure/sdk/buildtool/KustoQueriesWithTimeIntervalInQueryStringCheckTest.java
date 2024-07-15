package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.KustoQueriesWithTimeIntervalInQueryStringCheck.KustoQueriesVisitor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiVariable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * This class tests the KustoQueriesWithTimeIntervalInQueryStringCheck class.
 * <p>
 * These are some example queries that should be flagged:
 * 1. "String query1 = \"| where timestamp > ago(1d)\";": This query uses the ago function to create a time interval of 1 day. This should be FLAGGED.
 * 2. "String query2 = \"| where timestamp > datetime(2021-01-01)\";": This query compares the timestamp to a specific datetime. This should be FLAGGED.
 * 3. "String query3 = \"| where timestamp > now()\";": This query uses the now function to get the current timestamp. This should be FLAGGED.
 * 4. "String query4 = \"| where timestamp > startofday()\";": This query uses the startofday function to get the start of the current day. This should be FLAGGED.
 * 5. "String query5 = \"| where timestamp > startofmonth()\";": This query uses the startofmonth function to get the start of the current month. This should be FLAGGED.
 * 6. "String query11 = \"| where timestamp > ago(\" + days + \")\";": This query uses a variable to define the time interval. This should be FLAGGED.
 * 7. "String query12 = \"| where timestamp > datetime(\" + date + \")\";": This query uses a variable to define the datetime. This should not be FLAGGED.
 * If these queries are used in a method call to an Azure client, they should be flagged.
 * <p>
 * eg BlobClient blobAsyncClient = new BlobClientBuilder().buildClient();
 * <p>
 * String kqlQueryOne = "ExampleTable\n" +
 * "| where TimeGenerated > ago(1h)" +
 * "| summarize count() by bin(TimeGenerated, 1h);";
 * <p>
 * String result = blobAsyncClient.query(kqlQueryOne);
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
    @ParameterizedTest
    @ValueSource(strings = {"datetime(startDate)", "filter.datetime(2022-01-01)", "time.now()", "time.startofday()", "time.startofmonth()", "range.between(datetime(2022-01-01), datetime(2022-02-01)"})
    public void testKustoQueriesWithTimeIntervalInQueryStringCheck(String queryString) {

        String packageName = "com.azure";

        int numOfInvocations = 1;

        // verify register problem with local variable as the query string
        verifyRegisterProblemWithLocalVariable(queryString, packageName, numOfInvocations);

        // verify register problem with polyadic expression as the query string
        verifyRegisterProblemWithPolyadicExpression(queryString, packageName, numOfInvocations);
    }

    @Test
    public void testWithWrongPackageName() {

        String packageName = "com.microsoft.azure";
        String queryString = "datetime(startDate)";

        int numOfInvocations = 0;

        // verify register problem with local variable as the query string
        verifyRegisterProblemWithLocalVariable(queryString, packageName, numOfInvocations);

        // verify register problem with polyadic expression as the query string
        verifyRegisterProblemWithPolyadicExpression(queryString, packageName, numOfInvocations);
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
     * Create a visitor by calling the buildVisitor method of KustoQueriesWithTimeIntervalInQueryStringCheck
     *
     * @return PsiElementVisitor
     */
    PsiElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        KustoQueriesVisitor visitor = new KustoQueriesWithTimeIntervalInQueryStringCheck.KustoQueriesVisitor(mockHolder, isOnTheFly);
        return visitor;
    }


    /**
     * This method tests the registerProblem method with a local variable as the query string
     */
    void verifyRegisterProblemWithLocalVariable(String queryString, String packageName, int numOfInvocations) {

        PsiLocalVariable variable = mock(PsiLocalVariable.class);
        PsiLiteralExpression initializer = mock(PsiLiteralExpression.class);
        PsiLocalVariable parentElement = mock(PsiLocalVariable.class);
        PsiClass containingClass = mock(PsiClass.class);

        PsiMethodCallExpressionImpl methodCall = mock(PsiMethodCallExpressionImpl.class);
        PsiExpressionList argumentList = mock(PsiExpressionList.class);
        PsiReferenceExpression argument = mock(PsiReferenceExpression.class);
        PsiExpression[] arguments = new PsiExpression[]{argument};
        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiVariable resolvedElement = mock(PsiVariable.class);
        PsiReferenceExpression qualifierExpression = mock(PsiReferenceExpression.class);

        // stubs for handle local variable method
        when(variable.getInitializer()).thenReturn(initializer);
        when(variable.getName()).thenReturn("stringQuery");

        // stubs for checkExpression method
        when(initializer.getText()).thenReturn(queryString);
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
        when(PsiTreeUtil.getParentOfType(methodCall, PsiClass.class)).thenReturn(containingClass);
        when(containingClass.getQualifiedName()).thenReturn(packageName);

        // Visit the variable to store its name if it's a query string
        mockVisitor.visitElement(variable);

        // Visit the method call to check if the query variable is used and the method call is to an Azure client
        mockVisitor.visitElement(methodCall);

        // Verify that the problem was registered correctly for the method call
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(methodCall), contains("KQL queries with time intervals in the query string detected."));
    }

    void verifyRegisterProblemWithPolyadicExpression(String queryString, String packageName, int numOfInvocations) {

        PsiPolyadicExpression polyadicExpression = mock(PsiPolyadicExpression.class);
        PsiExpression initializer = mock(PsiExpression.class);
        PsiLocalVariable parentElement = mock(PsiLocalVariable.class);
        PsiClass containingClass = mock(PsiClass.class);

        // stubs for handlePolyadicExpression method
        when(polyadicExpression.getText()).thenReturn(queryString);

        // stubs for checkExpression method
        when(initializer.getText()).thenReturn(queryString);
        when(polyadicExpression.getParent()).thenReturn(parentElement);
        when(parentElement.getName()).thenReturn("stringQuery");

        PsiMethodCallExpressionImpl methodCall = mock(PsiMethodCallExpressionImpl.class);
        PsiExpressionList argumentList = mock(PsiExpressionList.class);
        PsiReferenceExpression argument = mock(PsiReferenceExpression.class);
        PsiExpression[] arguments = new PsiExpression[]{argument};
        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiVariable resolvedElement = mock(PsiVariable.class);
        PsiReferenceExpression qualifierExpression = mock(PsiReferenceExpression.class);

        // stubs for handleMethodCall method
        when(methodCall.getArgumentList()).thenReturn(argumentList);
        when(argumentList.getExpressions()).thenReturn(arguments);
        when(argument.resolve()).thenReturn(resolvedElement);
        when(resolvedElement.getName()).thenReturn("stringQuery");

        // stubs for isAzureClient method
        when(methodCall.getMethodExpression()).thenReturn(referenceExpression);
        when(referenceExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(PsiTreeUtil.getParentOfType(methodCall, PsiClass.class)).thenReturn(containingClass);
        when(containingClass.getQualifiedName()).thenReturn(packageName);

        // Visit the variable to store its name if it's a query string
        mockVisitor.visitElement(polyadicExpression);

        // Visit the method call to check if the query variable is used and the method call is to an Azure client
        mockVisitor.visitElement(methodCall);

        // Verify that the problem was registered correctly for the method call
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(methodCall), contains("KQL queries with time intervals in the query string detected."));
    }
}
