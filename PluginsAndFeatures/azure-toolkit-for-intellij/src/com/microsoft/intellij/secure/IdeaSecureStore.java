/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.secure;

import com.azure.core.util.Base64Util;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.microsoft.azuretools.securestore.SecureStore;
import com.microsoft.intellij.configuration.IdeaSecureStoreSettings;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import static com.intellij.credentialStore.CredentialAttributesKt.generateServiceName;

public class IdeaSecureStore implements SecureStore {
    private static final String ROOT = "Root";
    private static final String RANDOM_KEY = "random_key";
    private static final byte []INIT_VECTOR = "23f4919d43aa436e".getBytes(StandardCharsets.UTF_8); //random iv

    private byte [] randomKey;

    private PasswordSafe passwordSafe = PasswordSafe.getInstance();

    private static class LazyHolder {
        static final IdeaSecureStore INSTANCE = new IdeaSecureStore();
    }

    public static IdeaSecureStore getInstance() {
        return LazyHolder.INSTANCE;
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private IdeaSecureStore() {
        String base64Key = loadPassword(ROOT, RANDOM_KEY, null);

        if (StringUtils.isBlank(base64Key)) {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            randomKey = secretKey.getEncoded();
            passwordSafe.setPassword(makeKey(ROOT, RANDOM_KEY, null), Base64Util.encodeToString(randomKey));
        } else {
            randomKey = Base64Util.decodeString(base64Key);
        }

        String zeroKey = encrypt("");
        final IdeaSecureStoreSettings.Data state = IdeaSecureStoreSettings.getInstance().getState();
        final List<String> keyList = state.getKeys();
        if (!keyList.contains(zeroKey)) {
            state.getKeys().clear();
            state.getKeys().add(zeroKey);
            IdeaSecureStoreSettings.getInstance().loadState(state);
        }
    }

    @Override
    public void savePassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName, @Nonnull String password) {
        final String encryptKey = encrypt(StringUtils.joinWith("|", serviceName, StringUtils.defaultString(key), StringUtils.defaultString(userName)));
        synchronized (IdeaSecureStoreSettings.getInstance()) {
            final List<String> keyList = IdeaSecureStoreSettings.getInstance().getState().getKeys();
            if (encryptKey != null && !keyList.contains(encryptKey)) {
                keyList.add(encryptKey);
                IdeaSecureStoreSettings.getInstance().loadState(IdeaSecureStoreSettings.getInstance().getState());
            }
        }
        passwordSafe.setPassword(makeKey(serviceName, key, userName), password);
    }

    @Override
    @Nullable
    public String loadPassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName) {
        return passwordSafe.getPassword(makeKey(serviceName, key, userName));
    }

    @Override
    public void forgetPassword(@Nonnull String serviceName, @Nullable String key, @Nullable String userName) {
        final String encryptKey = encrypt(StringUtils.joinWith("|", serviceName, StringUtils.defaultString(key), StringUtils.defaultString(userName)));
        synchronized (IdeaSecureStoreSettings.getInstance()) {
            final List<String> keyList = IdeaSecureStoreSettings.getInstance().getState().getKeys();
            if (encryptKey != null && keyList.contains(encryptKey)) {
                keyList.remove(encryptKey);
                IdeaSecureStoreSettings.getInstance().loadState(IdeaSecureStoreSettings.getInstance().getState());
            }
        }

        CredentialAttributes oldKey = StringUtils.isNotBlank(userName) ? new CredentialAttributes(key, userName) :
            new CredentialAttributes(key);
        passwordSafe.setPassword(oldKey, null);
        passwordSafe.setPassword(makeKey(serviceName, key, userName), null);
    }

    @Override
    public void migratePassword(@Nonnull String oldKeyOrServiceName, @Nullable String oldUsername,
                                @Nonnull String serviceName, @Nullable String key, @Nullable String userName) {
        CredentialAttributes oldKey = StringUtils.isNotBlank(oldUsername) ? new CredentialAttributes(oldKeyOrServiceName, userName) :
            new CredentialAttributes(oldKeyOrServiceName);
        CredentialAttributes newKey = makeKey(serviceName, key, userName);
        if (StringUtils.isBlank(passwordSafe.getPassword(newKey))) {
            passwordSafe.setPassword(newKey, passwordSafe.getPassword(oldKey));
        }
        passwordSafe.setPassword(oldKey, null);
    }

    @Nonnull
    private CredentialAttributes makeKey(String serviceName, @Nullable String key, @Nullable String userName) {
        String serverNameWithPrefix = serviceName;
        if (!StringUtils.contains(serviceName, "Azure IntelliJ Plugin")) {
            serverNameWithPrefix = StringUtils.join("Azure IntelliJ Plugin | " + serviceName);
        }
        if (StringUtils.isAllBlank(key, userName)) {
            return new CredentialAttributes(serverNameWithPrefix);
        } else if (StringUtils.isNoneBlank(key, userName)) {
            return new CredentialAttributes(generateServiceName(serverNameWithPrefix, key), userName);
        } else if (StringUtils.isNotBlank(key)) {
            return new CredentialAttributes(generateServiceName(serverNameWithPrefix, key));
        }
        return new CredentialAttributes(serverNameWithPrefix, userName);
    }

    private String encrypt(String str) {
        try {
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR);
            SecretKeySpec skeySpec = new SecretKeySpec(randomKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(str.getBytes());
            return Base64Util.encodeToString(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private String decrypt(String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR);
            SecretKeySpec skeySpec = new SecretKeySpec(randomKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] original = cipher.doFinal(Base64Util.decodeString(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}
