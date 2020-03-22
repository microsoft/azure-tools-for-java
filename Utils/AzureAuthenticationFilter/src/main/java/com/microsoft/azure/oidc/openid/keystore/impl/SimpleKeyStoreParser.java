/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
