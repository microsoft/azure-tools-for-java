package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.EndpointOnNonAzureOpenAIAuthCheck.EndpointOnNonAzureOpenAIAuthVisitor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class is used to test the EndpointOnNonAzureOpenAIAuthCheck class.
 * The EndpointOnNonAzureOpenAIAuthCheck class is a LocalInspectionTool that checks if the endpoint method is used with KeyCredential for non-Azure OpenAI clients.
 * If the endpoint method is used with KeyCredential for non-Azure OpenAI clients, a warning is registered.
 * An example that should be flagged is:
 * OpenAI Client client = new OpenAIClientBuilder()
 * .credential(new KeyCredential("key"))
 * .endpoint("endpoint")
 * .buildClient();
 */
public class EndpointOnNonAzureOpenAIAuthCheckTest {

    // Declare as instance variables
    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiMethodCallExpression mockMethodCall;

    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
        mockMethodCall = mock(PsiMethodCallExpression.class);
    }

    /**
     * This test checks if the endpoint method is used with KeyCredential for non-Azure OpenAI clients.
     * If the endpoint method is used with KeyCredential for non-Azure OpenAI clients, a warning is registered.
     */
    @Test
    public void testEndpointOnNonAzureOpenAIAuthCheck() {

        int numOfInvocation = 1;
        String endpoint = "endpoint";
        String credential = "credential";
        String KeyCredentialPackageName = "KeyCredential";
        String azurePackageName = "com.azure.ai.openai";

        verifyRegisterProblem(numOfInvocation, endpoint, credential, KeyCredentialPackageName, azurePackageName);
    }

    /**
     * This test checks if the endpoint method is not used with KeyCredential for non-Azure OpenAI clients.
     * If the endpoint method is not used with KeyCredential for non-Azure OpenAI clients, no warning is registered.
     */
    @Test
    public void testNoEndpoint() {

        int numOfInvocation = 0;
        String endpoint = "notEndpoint";
        String credential = "credential";
        String KeyCredentialPackageName = "KeyCredential";
        String azurePackageName = "com.azure.ai.openai";

        verifyRegisterProblem(numOfInvocation, endpoint, credential, KeyCredentialPackageName, azurePackageName);
    }

    /**
     * This test checks if the endpoint method is used but credential is not used.
     * If the endpoint method is used but credential is not used, no warning is registered.
     */
    @Test
    public void testNoCredential() {

        int numOfInvocation = 0;
        String endpoint = "endpoint";
        String credential = "notCredential";
        String KeyCredentialPackageName = "KeyCredential";
        String azurePackageName = "com.azure.ai.openai";

        verifyRegisterProblem(numOfInvocation, endpoint, credential, KeyCredentialPackageName, azurePackageName);
    }

    /**
     * This test checks if the endpoint method is used with KeyCredential but for Azure OpenAI clients.
     * If the endpoint method is used with KeyCredential but for Azure OpenAI clients, no warning is registered.
     */
    @Test
    public void testWithAzureKeyCredential() {

        int numOfInvocation = 0;
        String endpoint = "endpoint";
        String credential = "notCredential";
        String KeyCredentialPackageName = "com.azure.core.credential.AzureKeyCredential";
        String azurePackageName = "com.azure.ai.openai";

        verifyRegisterProblem(numOfInvocation, endpoint, credential, KeyCredentialPackageName, azurePackageName);
    }

    /**
     * creates a JavaElementVisitor object for visiting method call expressions
     */
    private JavaElementVisitor createVisitor() {
        return new EndpointOnNonAzureOpenAIAuthVisitor(mockHolder);
    }

    /**
     * This method verifies if the registerProblem method is called with the correct parameters
     */
    private void verifyRegisterProblem(int numOfInvocation, String endpoint, String credential, String KeyCredentialPackageName, String azurePackageName) {

        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);

        PsiMethodCallExpression qualifierOne = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpressionOne = mock(PsiReferenceExpression.class);

        PsiNewExpression newExpression = mock(PsiNewExpression.class);
        PsiExpressionList argumentList = mock(PsiExpressionList.class);
        PsiExpression[] arguments = new PsiExpression[]{newExpression};

        PsiJavaCodeReferenceElement classReference = mock(PsiJavaCodeReferenceElement.class);

        PsiVariable parent = mock(PsiVariable.class);
        PsiType qualifierTYpe = mock(PsiType.class);

        when(mockMethodCall.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.getReferenceName()).thenReturn(endpoint);

        // isUsingKeyCredential
        when(methodExpression.getQualifierExpression()).thenReturn(qualifierOne);
        when(qualifierOne.getMethodExpression()).thenReturn(methodExpressionOne);
        when(methodExpressionOne.getReferenceName()).thenReturn(credential);
        when(methodExpressionOne.getQualifierExpression()).thenReturn(null);

        when(qualifierOne.getArgumentList()).thenReturn(argumentList);
        when(argumentList.getExpressions()).thenReturn(arguments);

        // isKeyCredential

        when(newExpression.getClassReference()).thenReturn(classReference);
        when(classReference.getReferenceName()).thenReturn(KeyCredentialPackageName);

        // isNonAzureOpenAIClient
        when(qualifierOne.getParent()).thenReturn(parent);
        when(parent.getType()).thenReturn(qualifierTYpe);
        when(qualifierTYpe.getCanonicalText()).thenReturn(azurePackageName);

        mockVisitor.visitMethodCallExpression(mockMethodCall);

        verify(mockHolder, times(numOfInvocation)).registerProblem(mockMethodCall, "Endpoint should not be used with KeyCredential for non-Azure OpenAI clients");
    }
}
