package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.ConnectionStringCheck.ConnectionStringVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * This class tests the ConnectionStringCheck class.
 * It tests the buildVisitor method and the visitElement method.
 * It tests the check for the usage of connection strings API to create clients in the Azure SDK for Java.
 */
public class ConnectionStringCheckTest {

    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private PsiElementVisitor mockVisitor;


    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
    }

    /**
     * This method creates a visitor for the ConnectionStringCheck class.
     *
     * @return PsiElementVisitor visitor
     */
    private PsiElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        ConnectionStringVisitor visitor = new ConnectionStringCheck.ConnectionStringVisitor(mockHolder, isOnTheFly);
        return visitor;
    }

    /**
     * This test checks the buildVisitor method.
     * It checks if the visitor is created and if it is an instance of JavaElementVisitor.
     * It also verifies that a warning is raised when a connection string being used to create a client is detected.
     * <p>
     * This is an example of a passing test.
     * 1. ServiceBusSenderClient senderClient2 = new ServiceBusClientBuilder().connectionString(connectionString2).sender().queueName("myQueueName2").buildClient();
     * 2. BlobServiceClient blobServiceClient2 = new BlobServiceClientBuilder().connectionString(connectionString2).buildClient();
     */
    @Test
    public void testConnectionStringCheck() {

        String methodToCheck = "connectionString";
        int numOfInvocations = 1;
        String packageName = "com.azure";

        verifyRegisterProblem(mockVisitor, methodToCheck, numOfInvocations, packageName);
    }

    /**
     * Problem isn't registered because the method to check is different
     * from the method that should be flagged
     */
    @Test
    public void differentMethodCheck() {
        String methodToCheck = "differentMethod";
        int numOfInvocations = 0;
        String packageName = "com.azure";

        verifyRegisterProblem(mockVisitor, methodToCheck, numOfInvocations, packageName);
    }

    /**
     * Problem isn't registered because the class name is different
     * from the class that should be flagged
     */
    @Test
    public void differentClassCheck() {
        String methodToCheck = "connectionString";
        int numOfInvocations = 0;
        String packageName = "com.microsoft.azure";

        verifyRegisterProblem(mockVisitor, methodToCheck, numOfInvocations, packageName);
    }

    /**
     * Problem isn't registered because the class name is null
     */
    @Test
    public void nullClassCheck() {
        String methodToCheck = "connectionString";
        int numOfInvocations = 0;
        String packageName = null;

        verifyRegisterProblem(mockVisitor, methodToCheck, numOfInvocations, packageName);
    }

    /**
     * Verifies that a warning is raised when a connection string being used to create a client is detected.
     *
     * @param visitor          PsiElementVisitor visitor to inspect elements in the code
     * @param methodToCheck    String method to check for in the code
     * @param numOfInvocations int number of times registerProblem should be called
     * @param packageName      String package name of the class
     */
    private void verifyRegisterProblem(PsiElementVisitor visitor, String methodToCheck, int numOfInvocations, String packageName) {

        PsiMethodCallExpression methodCallExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);
        PsiMethod resolvedMethod = mock(PsiMethod.class);
        PsiClass containingClass = mock(PsiClass.class);

        when(methodCallExpression.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.resolve()).thenReturn(resolvedMethod);
        when(resolvedMethod.getContainingClass()).thenReturn(containingClass);
        when(resolvedMethod.getName()).thenReturn(methodToCheck);
        when(containingClass.getQualifiedName()).thenReturn(packageName);

        (visitor).visitElement(methodCallExpression);

        // Verify problem is registered
        verify(mockHolder, times(numOfInvocations)).registerProblem(Mockito.eq(methodCallExpression), Mockito.contains("Connection String detected. Use DefaultAzureCredential for Azure service client authentication instead if the service client supports Token Credential (Entra ID Authentication)."));
    }
}
