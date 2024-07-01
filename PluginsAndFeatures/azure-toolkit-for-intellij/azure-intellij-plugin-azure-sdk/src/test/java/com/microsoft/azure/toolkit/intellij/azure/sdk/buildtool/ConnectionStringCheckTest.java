package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
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


/**
 * This class tests the ConnectionStringCheck class.
 * It tests the buildVisitor method and the visitElement method.
 * It tests the check for the usage of connection strings API to create clients in the Azure SDK for Java.
 *
 * This
 */
public class ConnectionStringCheckTest {

    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private PsiElement mockElement;

    @Mock
    private PsiElementVisitor mockVisitor;


    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockElement = mock(PsiElement.class);
        mockVisitor = createVisitor();
    }

    PsiElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        ConnectionStringCheck check = new ConnectionStringCheck();
        return check.buildVisitor(mockHolder, isOnTheFly);
    }

    /**
     * This test checks the buildVisitor method.
     * It checks if the visitor is created and if it is an instance of JavaElementVisitor.
     * It also verifies that a warning is raised when a connection string being used to create a client is detected.
     *
     * This is an example of a passing test.
     * 1. ServiceBusSenderClient senderClient2 = new ServiceBusClientBuilder().connectionString(connectionString2).sender().queueName("myQueueName2").buildClient();
     * 2. BlobServiceClient blobServiceClient2 = new BlobServiceClientBuilder().connectionString(connectionString2).buildClient();
     */
    @Test
    void testConnectionStringCheck() {

        assertVisitor(mockVisitor);
        String METHOD_TO_CHECK = "connectionString";
        int NUMBER_OF_INVOCATIONS = 1;
        String CLASS_NAME = "com.azure";

        verifyRegisterProblem(mockVisitor, METHOD_TO_CHECK, NUMBER_OF_INVOCATIONS, CLASS_NAME);
    }

    // Problem isn't registered because the method to check is different from the method that should be flagged
    @Test
    void differentMethodCheck() {
        String METHOD_TO_CHECK = "differentMethod";
        int NUMBER_OF_INVOCATIONS = 0;
        String CLASS_NAME = "com.azure";

        verifyRegisterProblem(mockVisitor, METHOD_TO_CHECK, NUMBER_OF_INVOCATIONS, CLASS_NAME);
    }

    // Problem isn't registered because the class name is different from the class that should be flagged
    @Test
    void differentClassCheck() {
        String METHOD_TO_CHECK = "connectionString";
        int NUMBER_OF_INVOCATIONS = 0;
        String CLASS_NAME = "com.microsoft.azure";

        verifyRegisterProblem(mockVisitor, METHOD_TO_CHECK, NUMBER_OF_INVOCATIONS, CLASS_NAME);
    }

    // Problem isn't registered because the class name is null
    @Test
    void nullClassCheck() {
        String METHOD_TO_CHECK = "connectionString";
        int NUMBER_OF_INVOCATIONS = 0;
        String CLASS_NAME = null;

        verifyRegisterProblem(mockVisitor, METHOD_TO_CHECK, NUMBER_OF_INVOCATIONS, CLASS_NAME);
    }

    // Asserts that the visitor is not null and is an instance of JavaElementVisitor
    void assertVisitor(PsiElementVisitor visitor) {
        assertNotNull(visitor);
        assertTrue(visitor instanceof JavaElementVisitor);
    }

    // Verifies that a warning is raised when a connection string being used to create a client is detected.
    void verifyRegisterProblem(PsiElementVisitor visitor, String METHOD_TO_CHECK, int NUMBER_OF_INVOCATIONS, String CLASS_NAME) {

        PsiMethodCallExpression methodCallExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);
        PsiMethod resolvedMethod = mock(PsiMethod.class);
        PsiMethod method = mock(PsiMethod.class);
        PsiClass containingClass = mock(PsiClass.class);

        when(methodCallExpression.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.resolve()).thenReturn(resolvedMethod);
        when(resolvedMethod.getContainingClass()).thenReturn(containingClass);
        when(resolvedMethod.getName()).thenReturn(METHOD_TO_CHECK);
        when(containingClass.getQualifiedName()).thenReturn(CLASS_NAME);

        (visitor).visitElement(methodCallExpression);

        // Verify problem is registered
        verify(mockHolder, times(NUMBER_OF_INVOCATIONS)).registerProblem(Mockito.eq(methodCallExpression), Mockito.contains("Connection String detected. Use AzureKeyCredentials for azure service client authentication instead."));
    }
}
