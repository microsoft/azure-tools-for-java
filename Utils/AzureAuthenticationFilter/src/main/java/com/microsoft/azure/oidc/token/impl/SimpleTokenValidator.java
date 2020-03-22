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
package com.microsoft.azure.oidc.token.impl;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.oidc.application.settings.ApplicationSettings;
import com.microsoft.azure.oidc.application.settings.ApplicationSettingsLoader;
import com.microsoft.azure.oidc.application.settings.impl.SimpleApplicationSettingsLoader;
import com.microsoft.azure.oidc.common.timestamp.TimeStamp;
import com.microsoft.azure.oidc.common.timestamp.TimeStampFactory;
import com.microsoft.azure.oidc.common.timestamp.impl.SimpleTimeStampFactory;
import com.microsoft.azure.oidc.configuration.Configuration;
import com.microsoft.azure.oidc.configuration.ConfigurationCache;
import com.microsoft.azure.oidc.configuration.impl.SimpleConfigurationCache;
import com.microsoft.azure.oidc.exception.GeneralException;
import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfigurationService;
import com.microsoft.azure.oidc.filter.configuration.algorithm.impl.SimpleAlgorithmConfigurationService;
import com.microsoft.azure.oidc.token.Token;
import com.microsoft.azure.oidc.token.TokenValidator;

public class SimpleTokenValidator implements TokenValidator {
    private static final TokenValidator INSTANCE = new SimpleTokenValidator();
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTokenValidator.class);

    private final ApplicationSettingsLoader applicationSettingsLoader = SimpleApplicationSettingsLoader.getInstance();

    private final ConfigurationCache configurationCache = SimpleConfigurationCache.getInstance();

    private final TimeStampFactory timeStampFactory = SimpleTimeStampFactory.getInstance();

    private final AlgorithmConfigurationService algorithmConfigurationService = SimpleAlgorithmConfigurationService.getInstance();

    @Override
    public Boolean validateSignature(final Token token) {
        if (token == null) {
            throw new PreconditionException("Required parameter is null");
        }
        if (algorithmConfigurationService.get().getAlgorithmClassMap().get(token.getAlgorithm().getName()).equals("HMAC")) {
            return Boolean.FALSE;
        }
        final Configuration configuration = configurationCache.load();
        if (configuration == null) {
            throw new GeneralException("Error loading configuration");
        }
        try {
            final TimeStamp now = timeStampFactory.createTimeStamp(System.currentTimeMillis() / 1000);
            if (configuration.getKey(token.getKeyName()).getNotBefore().compareTo(now) > 0) {
                return Boolean.FALSE;
            }
            final Base64 decoder = new Base64();
            final BigInteger exponent = new BigInteger(1,
                    decoder.decode(configuration.getKey(token.getKeyName()).getExponent().getValue()));
            final BigInteger modulus = new BigInteger(1,
                    decoder.decode(configuration.getKey(token.getKeyName()).getSecret().getValue()));
            final RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, exponent);
            final KeyFactory keyFactory = KeyFactory
                    .getInstance(algorithmConfigurationService.get().getAlgorithmClassMap().get(token.getAlgorithm().getName()));
            final PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
            final Signature sig = Signature
                    .getInstance(algorithmConfigurationService.get().getAlgorithmMap().get(token.getAlgorithm().getName()));
            sig.initVerify(pubKey);
            sig.update(token.getPayload().getValue().getBytes());
            return sig.verify(decoder.decode(token.getSignature().getValue()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SignatureException | InvalidKeyException e) {
            LOGGER.error(e.getMessage(), e);
            return Boolean.FALSE;
        }
    }

    @Override
    public Boolean validateAudience(final Token token) {
        if (token == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final ApplicationSettings applicationSettings = applicationSettingsLoader.load();
        if (token.getAudience().equals(applicationSettings.getApplicationId())) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean validateIssuer(final Token token) {
        if (token == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final Configuration configuration = configurationCache.load();
        if (configuration == null) {
            throw new GeneralException("Error loading configuration");
        }
        if (token.getIssuer().equals(configuration.getIssuer())) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean validateIssuedAt(final Token token) {
        if (token == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final TimeStamp now = timeStampFactory.createTimeStamp(System.currentTimeMillis() / 1000);
        if (token.getIssuedAt().compareTo(now) <= 0) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean validateNotBefore(final Token token) {
        if (token == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final TimeStamp now = timeStampFactory.createTimeStamp(System.currentTimeMillis() / 1000);
        if (token.getNotBefore().compareTo(now) <= 0) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean validateExpiration(final Token token) {
        if (token == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final TimeStamp now = timeStampFactory.createTimeStamp(System.currentTimeMillis() / 1000);
        if (token.getExpiration().compareTo(now) > 0) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean validateCommon(final Token token) {
        if (token == null) {
            throw new PreconditionException("Required parameter is null");
        }
        if (!validateAudience(token)) {
            return Boolean.FALSE;
        }
        if (!validateIssuer(token)) {
            return Boolean.FALSE;
        }
        if (!validateIssuedAt(token)) {
            return Boolean.FALSE;
        }
        if (!validateNotBefore(token)) {
            return Boolean.FALSE;
        }
        if (!validateSignature(token)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public static TokenValidator getInstance() {
        return INSTANCE;
    }
}
