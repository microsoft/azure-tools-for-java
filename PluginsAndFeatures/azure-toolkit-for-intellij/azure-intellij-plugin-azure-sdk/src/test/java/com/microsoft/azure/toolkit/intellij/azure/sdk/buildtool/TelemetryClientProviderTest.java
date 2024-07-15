package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiVariable;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.TelemetryClientProvider.TelemetryClientProviderVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class tests the TelemetryClientProvider class.
 * It tests the buildVisitor, visitMethodCallExpression, and sendTelemetryData methods.
 */
public class TelemetryClientProviderTest {

    @Mock
    private TelemetryClient mockTelemetryClient;

    @Mock
    private ProblemsHolder mockProblemsHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockVisitor = createVisitor();
        TelemetryClientProviderVisitor.methodCounts = new HashMap<>();
    }

    /**
     * This test method tests the visitMethodCallExpression method.
     * It tests the method with an Azure package to ensure the method count is incremented correctly.
     */
    @Test
    public void testVisitMethodCallExpressionWithAzurePackage() {

        String packageName = "com.azure.testClient";
        String className = "testClient";
        String methodName = "upsertMethod";
        int numCountIncrease = 1;

        // Mock necessary objects
        PsiMethodCallExpression mockExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression mockMethodExpression = mock(PsiReferenceExpression.class);
        PsiReferenceExpression mockQualifier = mock(PsiReferenceExpression.class);
        PsiVariable mockElement = mock(PsiVariable.class);
        PsiClassType mockType = mock(PsiClassType.class);
        PsiClass mockClass = mock(PsiClass.class);


        // Mock method name and client name
        when(mockExpression.getMethodExpression()).thenReturn(mockMethodExpression);
        when(mockMethodExpression.getReferenceName()).thenReturn(methodName);
        when(mockMethodExpression.getQualifierExpression()).thenReturn(mockQualifier);
        when(mockQualifier.resolve()).thenReturn(mockElement);
        when(mockElement.getType()).thenReturn(mockType);
        when(mockType.resolve()).thenReturn(mockClass);
        when(mockClass.getQualifiedName()).thenReturn(packageName);
        when(mockType.getPresentableText()).thenReturn(className);

        // Build the visitor and visit the method call expression
        mockVisitor.visitElement(mockExpression);

        // Verify the method count increment
        assertTrue(TelemetryClientProviderVisitor.methodCounts.containsKey(className));

        // have this assertion if the client is an azure client
        if (numCountIncrease > 0) {
            assertEquals(numCountIncrease, TelemetryClientProviderVisitor.methodCounts.get(className).get(methodName).intValue());
        }
    }

    /**
     * This test method tests the visitMethodCallExpression method.
     * It tests the method with a non-Azure package to ensure the methodcount list is not incremented.
     */
    @Test
    public void testVisitMethodCallExpressionWithNonAzurePackage() {

        String packageName = "com.nonazure";
        String className = null;
        String methodName = null;

        PsiMethodCallExpression mockExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression mockMethodExpression = mock(PsiReferenceExpression.class);
        PsiReferenceExpression mockQualifier = mock(PsiReferenceExpression.class);
        PsiVariable mockElement = mock(PsiVariable.class);
        PsiClassType mockType = mock(PsiClassType.class);
        PsiClass mockClass = mock(PsiClass.class);

        // Mock method name and client name
        when(mockExpression.getMethodExpression()).thenReturn(mockMethodExpression);
        when(mockMethodExpression.getReferenceName()).thenReturn(methodName);
        when(mockMethodExpression.getQualifierExpression()).thenReturn(mockQualifier);
        when(mockQualifier.resolve()).thenReturn(mockElement);
        when(mockElement.getType()).thenReturn(mockType);
        when(mockType.resolve()).thenReturn(mockClass);
        when(mockClass.getQualifiedName()).thenReturn(packageName);
        when(mockType.getPresentableText()).thenReturn(className);


        // Build the visitor and visit the method call expression
        mockVisitor.visitMethodCallExpression(mockExpression);

        // assert that the methodCounts map is empty
        assertTrue(TelemetryClientProviderVisitor.methodCounts.isEmpty());

    }

    /**
     * This test method tests the sendTelemetryData method.
     * It tests that the telemetry data is sent correctly.
     */
    @Test
    public void testSendTelemetryData() {
        // Populate methodCounts with test data
        Map<String, Integer> methodMap = new HashMap<>();
        methodMap.put("testMethod", 1);
        TelemetryClientProviderVisitor.methodCounts.put("testClient", methodMap);

        // Inject the mock telemetry client
        TelemetryClientProviderVisitor.telemetryClient = mockTelemetryClient;

        // Call the method to send telemetry data
        TelemetryClientProviderVisitor.sendTelemetryData();

        // Verify that trackMetric and trackEvent were called
        verify(mockTelemetryClient, times(1)).trackEvent(eq("azure_sdk_usage_frequency"), anyMap(), anyMap());
        verify(mockTelemetryClient, times(1)).flush();
    }

    // Helper method to create a visitor
    private JavaElementVisitor createVisitor() {
        boolean isOnTheFly = false;
        TelemetryClientProviderVisitor visitor = new TelemetryClientProviderVisitor(mockProblemsHolder, false);

        return visitor;
    }
}
