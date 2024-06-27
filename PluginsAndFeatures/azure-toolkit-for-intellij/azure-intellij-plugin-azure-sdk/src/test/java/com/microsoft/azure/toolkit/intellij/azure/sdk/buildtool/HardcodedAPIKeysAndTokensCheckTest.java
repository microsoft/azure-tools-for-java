package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.HardcodedAPIKeysAndTokensCheck.APIKeysAndTokensVisitor;

import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiNewExpression;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the HardcodedAPIKeysAndTokensCheck class for hardcoded API keys and tokens.
 * When a client is authenticated with AzurekeyCredentials and AccessToken, a problem is registered.
 These are some instances that a flag would be raised.
 * 1. TextAnalyticsClient client = new TextAnalyticsClientBuilder()
 *         .endpoint(endpoint)
 *         .credential(new AzureKeyCredential(apiKey))
 *         .buildClient();
 *
 * 2. TokenCredential credential = request -> {
 *         AccessToken token = new AccessToken("<your-hardcoded-token>", OffsetDateTime.now().plusHours(1));
 *     }
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
    @Test
    public void testHardcodedAPIKeysAndTokensCheck() {

        // assert visitor
        assertVisitor(mockVisitor);

        int NUMBER_OF_INVOCATIONS = 1; // number of times registerProblem is called

        String CLIENT_TO_CHECK_ONE = "AccessToken";
        verifyRegisterProblem(CLIENT_TO_CHECK_ONE, NUMBER_OF_INVOCATIONS);

        String CLIENT_TO_CHECK_TWO = "AzureKeyCredential";
        verifyRegisterProblem(CLIENT_TO_CHECK_TWO, NUMBER_OF_INVOCATIONS);
    }

    /**
     * Test for non-auth client use.
     * These are other Azure clients that are not AzureKeyCredential or AccessToken.
     */
    @Test
    public void testNonAuthAzureClientUse() {

        // assert visitor
        assertVisitor(mockVisitor);

        String CLIENT_TO_CHECK = "SomeOtherClient";
        int NUMBER_OF_INVOCATIONS = 0;
        verifyRegisterProblem(CLIENT_TO_CHECK, NUMBER_OF_INVOCATIONS);
    }

    /**
     * Test for null class reference.
     */
    @Test
    public void testNullClassReference() {

        // assert visitor
        assertVisitor(mockVisitor);

        String CLIENT_TO_CHECK = ""; // null class reference
        int NUMBER_OF_INVOCATIONS = 0;
        verifyRegisterProblem(CLIENT_TO_CHECK, NUMBER_OF_INVOCATIONS);
    }


    // Helper method to create visitor.
    PsiElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        APIKeysAndTokensVisitor visitor = new HardcodedAPIKeysAndTokensCheck.APIKeysAndTokensVisitor(mockHolder, isOnTheFly);
        return visitor;
    }


    // Helper method to assert visitor is not null and is an instance of JavaElementVisitor
    void assertVisitor(PsiElementVisitor mockVisitor) {

        assertNotNull(mockVisitor);
        assertTrue(mockVisitor instanceof JavaElementVisitor);
    }

    // Helper method to verify registerProblem is called
    void verifyRegisterProblem(String CLIENT_TO_CHECK, int NUMBER_OF_INVOCATIONS) {

        PsiNewExpression newExpression = mock(PsiNewExpression.class);
        PsiJavaCodeReferenceElement javaCodeReferenceElement = mock(PsiJavaCodeReferenceElement.class);

        when(newExpression.getClassReference()).thenReturn(javaCodeReferenceElement);
        when(javaCodeReferenceElement.getReferenceName()).thenReturn(CLIENT_TO_CHECK);

        when(javaCodeReferenceElement.getQualifiedName()).thenReturn("com.azure");

        mockVisitor.visitElement(newExpression);

        // Verify registerProblem is called
        verify(mockHolder, times(NUMBER_OF_INVOCATIONS)).registerProblem(eq(newExpression),
                Mockito.contains("Use of API keys or tokens in code is not recommended. " +
                        "We recommend Secretless authentication through DefaultAzureCredential"));
    }
}
