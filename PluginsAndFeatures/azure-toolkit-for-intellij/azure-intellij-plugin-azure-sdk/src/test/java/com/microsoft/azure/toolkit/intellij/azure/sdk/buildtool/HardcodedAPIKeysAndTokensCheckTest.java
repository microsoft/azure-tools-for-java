package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.HardcodedAPIKeysAndTokensCheck.APIKeysAndTokensVisitor;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiNewExpression;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the HardcodedAPIKeysAndTokensCheck class for hardcoded API keys and tokens.
 * When a client is authenticated with AzurekeyCredentials and AccessToken, a problem is registered.
 * These are some instances that a flag would be raised.
 * 1. TextAnalyticsClient client = new TextAnalyticsClientBuilder()
 * .endpoint(endpoint)
 * .credential(new AzureKeyCredential(apiKey))
 * .buildClient();
 * <p>
 * 2. TokenCredential credential = request -> {
 * AccessToken token = new AccessToken("<your-hardcoded-token>", OffsetDateTime.now().plusHours(1));
 * }
 */
public class HardcodedAPIKeysAndTokensCheckTest {

    // Declare as instance variables
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
     * Test the HardcodedAPIKeysAndTokensCheck class for hardcoded API keys and tokens.
     * When a client is authenticated with AzurekeyCredentials and AccessToken, a problem is registered.
     */
    @ParameterizedTest
    @ValueSource(strings = {"AzureKeyCredential", "AccessToken", "KeyCredential", "AzureNamedKeyCredential", "AzureSasCredential", "AzureNamedKey", "ClientSecretCredentialBuilder", "UsernamePasswordCredentialBuilder", "BasicAuthenticationCredential"})
    public void testHardcodedAPIKeysAndTokensCheck(String clientType) {

        int numOfInvocations = 1; // number of times registerProblem is called
        verifyRegisterProblem(clientType, numOfInvocations);
    }

    /**
     * Test for non-auth client use.
     * These are other Azure clients that are not AzureKeyCredential or AccessToken.
     */
    @Test
    public void testNonAuthAzureClientUse() {

        String authServiceToCheck = "SomeOtherClient";
        int numOfInvocations = 0;
        verifyRegisterProblem(authServiceToCheck, numOfInvocations);
    }

    /**
     * Test for null class reference.
     */
    @Test
    public void testNullClassReference() {

        String authServiceToCheck = ""; // null class reference
        int numOfInvocations = 0;
        verifyRegisterProblem(authServiceToCheck, numOfInvocations);
    }

    // Helper method to create visitor.
    PsiElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        APIKeysAndTokensVisitor visitor = new HardcodedAPIKeysAndTokensCheck.APIKeysAndTokensVisitor(mockHolder, isOnTheFly);
        return visitor;
    }

    // Helper method to verify registerProblem is called
    private void verifyRegisterProblem(String authServiceToCheck, int numOfInvocations) {

        PsiNewExpression newExpression = mock(PsiNewExpression.class);
        PsiJavaCodeReferenceElement javaCodeReferenceElement = mock(PsiJavaCodeReferenceElement.class);

        when(newExpression.getClassReference()).thenReturn(javaCodeReferenceElement);
        when(javaCodeReferenceElement.getReferenceName()).thenReturn(authServiceToCheck);

        when(javaCodeReferenceElement.getQualifiedName()).thenReturn("com.azure");

        mockVisitor.visitElement(newExpression);

        // Verify registerProblem is called
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(newExpression), Mockito.contains("DefaultAzureCredential is recommended for authentication if the service client supports Token Credential (Entra ID Authentication). " + "If not, then use Azure Key Credential for API key based authentication."));
    }
}
