/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.openid.keystore.impl;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.oidc.common.name.Name;
import com.microsoft.azure.oidc.common.name.NameFactory;
import com.microsoft.azure.oidc.common.name.impl.SimpleNameFactory;
import com.microsoft.azure.oidc.common.timestamp.TimeStamp;
import com.microsoft.azure.oidc.common.timestamp.TimeStampFactory;
import com.microsoft.azure.oidc.common.timestamp.impl.SimpleTimeStampFactory;
import com.microsoft.azure.oidc.configuration.key.Key;
import com.microsoft.azure.oidc.configuration.key.KeyFactory;
import com.microsoft.azure.oidc.configuration.key.exponent.Exponent;
import com.microsoft.azure.oidc.configuration.key.exponent.ExponentFactory;
import com.microsoft.azure.oidc.configuration.key.exponent.impl.SimpleExponentFactory;
import com.microsoft.azure.oidc.configuration.key.impl.SimpleKeyFactory;
import com.microsoft.azure.oidc.configuration.key.modulus.Modulus;
import com.microsoft.azure.oidc.configuration.key.modulus.ModulusFactory;
import com.microsoft.azure.oidc.configuration.key.modulus.impl.SimpleModulusFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.openid.keystore.KeyStoreParser;

public final class SimpleKeyStoreParser implements KeyStoreParser {
    private static final KeyStoreParser INSTANCE = new SimpleKeyStoreParser();

    private final KeyFactory keyFactory = SimpleKeyFactory.getInstance();

    private final NameFactory nameFactory = SimpleNameFactory.getInstance();

    private final ModulusFactory modulusFactory = SimpleModulusFactory.getInstance();

    private final ExponentFactory exponentFactory = SimpleExponentFactory.getInstance();

    private TimeStampFactory timeStampFactory = SimpleTimeStampFactory.getInstance();

    @Override
    public Map<Name, Key> getKeys(final JsonNode node) {
        if (node == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final Map<Name, Key> keys = new HashMap<Name, Key>();
        for (final JsonNode n : node.get("keys")) {
            final TimeStamp notBefore = timeStampFactory.createTimeStamp(n.has("nbf") ? n.get("nbf").asLong() : 0L);
            final Name keyName = nameFactory.createKeyName(n.get("kid").asText());
            final Modulus modulus = modulusFactory.createKeyValue(n.get("n").asText());
            final Exponent exponent = exponentFactory.createKeyExponent(n.get("e").asText());
            final Key key = keyFactory.createKey(notBefore, modulus, exponent);
            keys.put(keyName, key);
        }
        return keys;
    }

    public static KeyStoreParser getInstance() {
        return INSTANCE;
    }
}
