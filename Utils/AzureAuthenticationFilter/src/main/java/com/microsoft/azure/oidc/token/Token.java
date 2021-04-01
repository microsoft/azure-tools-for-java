/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.token;

import java.util.List;

import com.microsoft.azure.oidc.common.algorithm.Algorithm;
import com.microsoft.azure.oidc.common.id.ID;
import com.microsoft.azure.oidc.common.issuer.Issuer;
import com.microsoft.azure.oidc.common.name.Name;
import com.microsoft.azure.oidc.common.timestamp.TimeStamp;
import com.microsoft.azure.oidc.token.email.Email;
import com.microsoft.azure.oidc.token.payload.Payload;
import com.microsoft.azure.oidc.token.signature.Signature;

public interface Token {

    Name getKeyName();

    Algorithm getAlgorithm();

    TimeStamp getIssuedAt();

    TimeStamp getNotBefore();

    TimeStamp getExpiration();

    Issuer getIssuer();

    ID getAudience();

    ID getUserID();

    List<Email> getUserEmails();

    String getValue();

    Payload getPayload();

    Signature getSignature();

    boolean equals(Object object);

    int hashCode();

}
