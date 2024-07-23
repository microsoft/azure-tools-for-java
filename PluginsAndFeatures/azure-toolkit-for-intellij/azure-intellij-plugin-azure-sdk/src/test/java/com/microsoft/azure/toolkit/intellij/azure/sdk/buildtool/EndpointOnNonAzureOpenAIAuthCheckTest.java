package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiMethodCallExpression;
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

    @Test
    public void testEndpointOnNonAzureOpenAIAuthCheck() {

        int numOfInvocation = 1;
        String endpoint = "endpoint";
        String credential = "credential";
        String KeyCredentialPackageName = "com.azure.core.credential.KeyCredential";
        String azurePackageName = "com.azure.ai.openai";

        verifyRegisterProblem(numOfInvocation, endpoint, credential, KeyCredentialPackageName, azurePackageName);
    }

    @Test
    public void testNoEndpoint(){

        int numOfInvocation = 0;
        String endpoint = "notEndpoint";
        String credential = "credential";
        String KeyCredentialPackageName = "com.azure.core.credential.KeyCredential";
        String azurePackageName = "com.azure.ai.openai";

        verifyRegisterProblem(numOfInvocation, endpoint, credential, KeyCredentialPackageName, azurePackageName);
    }

    @Test
    public void testNoCredential(){

        int numOfInvocation = 0;
        String endpoint = "endpoint";
        String credential = "notCredential";
        String KeyCredentialPackageName = "com.azure.core.credential.KeyCredential";
        String azurePackageName = "com.azure.ai.openai";

        verifyRegisterProblem(numOfInvocation, endpoint, credential, KeyCredentialPackageName, azurePackageName);
    }

    @Test
    public void testWithAzureKeyCredential(){

        int numOfInvocation = 0;
        String endpoint = "endpoint";
        String credential = "notCredential";
        String KeyCredentialPackageName = "com.azure.core.credential.AzureKeyCredential";
        String azurePackageName = "com.azure.ai.openai";

        verifyRegisterProblem(numOfInvocation, endpoint, credential, KeyCredentialPackageName, azurePackageName);
    }



    private JavaElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        EndpointOnNonAzureOpenAIAuthVisitor mockVisitor = new EndpointOnNonAzureOpenAIAuthVisitor(mockHolder, isOnTheFly);
        return mockVisitor;
    }


    private void verifyRegisterProblem(int numOfInvocation, String endpoint, String credential, String KeyCredentialPackageName, String azurePackageName) {


        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);

        PsiMethodCallExpression qualifierOne = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpressionOne = mock(PsiReferenceExpression.class);

        PsiExpression expression = mock(PsiExpression.class);
        PsiExpressionList argumentList = mock(PsiExpressionList.class);
        PsiExpression[] arguments = new PsiExpression[]{expression};

        PsiType type = mock(PsiType.class);
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
        when(expression.getType()).thenReturn(type);
        when(type.getCanonicalText()).thenReturn(KeyCredentialPackageName);

        // isNonAzureOpenAIClient
        when(qualifierOne.getParent()).thenReturn(parent);
        when(parent.getType()).thenReturn(qualifierTYpe);
        when(qualifierTYpe.getCanonicalText()).thenReturn(azurePackageName);

        mockVisitor.visitMethodCallExpression(mockMethodCall);

        verify(mockHolder, times(numOfInvocation)).registerProblem(mockMethodCall, "Endpoint should not be used with KeyCredential for non-Azure OpenAI clients");


    }
}
