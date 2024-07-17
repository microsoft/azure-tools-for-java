package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.DetectDiscouragedAPIUsageCheck.DetectDiscouragedAPIUsageVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * This class tests the DetectDiscouragedAPIUsageCheck class.
 * It tests the buildVisitor method and the visitElement method.
 * It tests the check for the usage of discouraged APIs in the code.
 * The test checks for the use of the following discouraged APIs:
 * 1. connectionString
 * 2. getCompletions
 * <p>
 * This is an example of a problem that would be raised:
 * 1. ServiceBusSenderClient senderClient2 = new ServiceBusClientBuilder().connectionString(connectionString2).sender().queueName("myQueueName2").buildClient();
 * * 2. BlobServiceClient blobServiceClient2 = new BlobServiceClientBuilder().connectionString(connectionString2).buildClient();
 * 3. Completions completions = client.getCompletions(deploymentOrModelId, new CompletionsOptions(prompt));
 */
public class DetectDiscouragedAPIUsageCheckTest {

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
     * This test checks the buildVisitor method.
     * It checks if the visitor is created and if it is an instance of JavaElementVisitor.
     * It also verifies that a warning is raised when a discouraged API is detected.
     */
    @ParameterizedTest
    @CsvSource({"connectionString, Connection String detected. Use DefaultAzureCredential for Azure service client authentication instead if the service client supports Token Credential (Entra ID Authentication)", "getCompletions, getCompletions API detected. Use the getChatCompletions API instead."})
    public void testDetectDiscouragedAPIUsageCheck(String methodToCheck, String suggestionMessage) {

        int numOfInvocations = 1;
        String packageName = "com.azure";

        verifyRegisterProblem(mockVisitor, methodToCheck, numOfInvocations, packageName, suggestionMessage);
    }

    /**
     * Problem isn't registered because the method to check is different
     * from the methods that should be flagged
     */
    @Test
    public void differentMethodCheck() {
        String methodToCheck = "differentMethod";
        int numOfInvocations = 0;
        String packageName = "com.azure";
        String suggestionMessage = "";

        verifyRegisterProblem(mockVisitor, methodToCheck, numOfInvocations, packageName, suggestionMessage);
    }

    /**
     * Problem isn't registered because the package name is different
     * from the package that should be flagged
     */
    @Test
    public void differentClassCheck() {
        String methodToCheck = "connectionString";
        int numOfInvocations = 0;
        String packageName = "com.microsoft.azure";
        String suggestionMessage = "";

        verifyRegisterProblem(mockVisitor, methodToCheck, numOfInvocations, packageName, suggestionMessage);
    }

    /**
     * Problem isn't registered because the package name is null
     */
    @Test
    public void nullClassCheck() {
        String methodToCheck = "getCompletions";
        int numOfInvocations = 0;
        String packageName = null;
        String suggestionMessage = "";

        verifyRegisterProblem(mockVisitor, methodToCheck, numOfInvocations, packageName, suggestionMessage);
    }

    /**
     * This method creates a visitor for the DetectDiscouragedAPIUsageCheck class.
     *
     * @return PsiElementVisitor visitor
     */
    private PsiElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        DetectDiscouragedAPIUsageVisitor visitor = new DetectDiscouragedAPIUsageVisitor(mockHolder, isOnTheFly);
        return visitor;
    }

    /**
     * Verifies that a warning is raised when a discouraged API is detected.
     *
     * @param visitor          PsiElementVisitor visitor to inspect elements in the code
     * @param methodToCheck    String method to check for in the code
     * @param numOfInvocations int number of times registerProblem should be called
     * @param packageName      String package name of the class
     */
    private void verifyRegisterProblem(PsiElementVisitor visitor, String methodToCheck, int numOfInvocations, String packageName, String suggestionMessage) {

        PsiMethodCallExpression methodCallExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);
        PsiMethod resolvedMethod = mock(PsiMethod.class);
        PsiClass containingClass = mock(PsiClass.class);
        PsiElement problemElement = mock(PsiElement.class);

        when(methodCallExpression.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.resolve()).thenReturn(resolvedMethod);
        when(resolvedMethod.getContainingClass()).thenReturn(containingClass);
        when(resolvedMethod.getName()).thenReturn(methodToCheck);
        when(containingClass.getQualifiedName()).thenReturn(packageName);
        when(methodExpression.getReferenceNameElement()).thenReturn(problemElement);

        (visitor).visitElement(methodCallExpression);

        // Verify problem is registered
        verify(mockHolder, times(numOfInvocations)).registerProblem(Mockito.eq(problemElement), Mockito.contains(suggestionMessage));
    }
}
