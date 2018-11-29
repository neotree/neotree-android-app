/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Ubiqueworks Ltd and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.neotree.support.realm;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.x500.X500Principal;

import io.realm.RealmConfiguration;

@TargetApi(23)
public class EncryptionKeyStore {

    private static final String STORAGE_PREF_NAME = ".realm_key";
    private static final String STORAGE_PREF_KEY = "iv_and_encrypted_key";

    private static final String KEYSTORE_PROVIDER_NAME = "AndroidKeyStore";
    private static final String KEY_ALIAS = "realm_key";
    private static final String KEY_COMMON_NAME = "RealmEncryption";

    private static final String CIPHER_API_18 = "RSA/ECB/PKCS1Padding";
    private static final String CIPHER_API_23 = KeyProperties.KEY_ALGORITHM_AES + "/"
            + KeyProperties.BLOCK_MODE_CBC + "/"
            + KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String CIPHER = (isApi23()) ? CIPHER_API_23 : CIPHER_API_18;
    private static final String TYPE_RSA = "RSA";

    private static final ByteOrder ORDER_FOR_ENCRYPTED_DATA = ByteOrder.BIG_ENDIAN;

    private static EncryptionKeyStore sInstance;

    private final SecureRandom mSecureRandom = new SecureRandom();
    private final KeyStore mKeyStore = prepareKeyStore();

    private EncryptionKeyStore() {

    }

    private static boolean isApi23() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    public static byte[] generateOrGetRealmEncryptionKey(Context context) {
        ensureInstance();

        Context appContext = context.getApplicationContext();
        byte[] encryptedRealmKey = sInstance.loadEncryptedRealmKey(appContext);
        if (encryptedRealmKey == null || !sInstance.keystoreContainsEncryptionKey()) {
            final byte[] realmKey = sInstance.generateKeyForRealm();
            sInstance.generateKeyInKeystore(context);
            encryptedRealmKey = sInstance.encryptAndSaveKeyForRealm(appContext, realmKey);
            Arrays.fill(realmKey, (byte) 0);
        }
        return sInstance.decryptKeyForRealm(encryptedRealmKey);
    }

    @SuppressLint("CommitPrefEdits")
    public static void reset(Context context) {
        ensureInstance();
        sInstance.getPreference(context).edit().clear().commit();
    }

    private static void ensureInstance() {
        if (sInstance == null) {
            sInstance = new EncryptionKeyStore();
        }
    }

    private boolean keystoreContainsEncryptionKey() {
        try {
            return mKeyStore.containsAlias(KEY_ALIAS);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] generateKeyForRealm() {
        final byte[] keyForRealm = new byte[RealmConfiguration.KEY_LENGTH];
        mSecureRandom.nextBytes(keyForRealm);
        return keyForRealm;
    }

    @SuppressWarnings("deprecation")
    private void generateKeyInKeystore(Context context) {
        try {
            if (isApi23()) {
                final KeyGenerator keyGenerator;
                keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                        KEYSTORE_PROVIDER_NAME);

                final KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(false)
                        .build();

                try {
                    keyGenerator.init(keySpec);
                } catch (InvalidAlgorithmParameterException e) {
                    throw new RuntimeException(e);
                }
                keyGenerator.generateKey();
            } else {
                Calendar start = new GregorianCalendar();
                Calendar end = new GregorianCalendar();
                end.add(Calendar.YEAR, 25);

                KeyPairGeneratorSpec spec =
                        new KeyPairGeneratorSpec.Builder(context)
                                .setAlias(KEY_ALIAS)
                                .setSubject(new X500Principal("CN=" + KEY_COMMON_NAME))
                                .setSerialNumber(BigInteger.valueOf(1337))
                                .setStartDate(start.getTime())
                                .setEndDate(end.getTime())
                                .build();

                final KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance(TYPE_RSA,
                        KEYSTORE_PROVIDER_NAME);
                kpGenerator.initialize(spec);
                kpGenerator.generateKeyPair();
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    private KeyStore prepareKeyStore() {
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_NAME);
            ks.load(null);
            return ks;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Cipher prepareCipher() {
        final Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return cipher;
    }

    private byte[] encryptAndSaveKeyForRealm(Context context, byte[] keyForRealm) {
        final KeyStore ks = prepareKeyStore();
        final Cipher cipher = prepareCipher();

        final byte[] iv;
        final byte[] encryptedKeyForRealm;
        try {
            final Key key;
            if (isApi23()) {
                key = ks.getKey(KEY_ALIAS, null);
            } else {
                final KeyStore.PrivateKeyEntry privateKeyEntry;
                try {
                    privateKeyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(KEY_ALIAS, null);
                    key = privateKeyEntry.getCertificate().getPublicKey();
                } catch (UnrecoverableEntryException e) {
                    throw new RuntimeException("key for encryption is invalid", e);
                }
            }
            cipher.init(Cipher.ENCRYPT_MODE, key);

            encryptedKeyForRealm = cipher.doFinal(keyForRealm);
            iv = cipher.getIV();
        } catch (InvalidKeyException | UnrecoverableKeyException | NoSuchAlgorithmException
                | KeyStoreException | BadPaddingException | IllegalBlockSizeException e) {
            throw new RuntimeException("key for encryption is invalid", e);
        }

        final int ivLength = ((iv != null) ? iv.length : 0);
        final byte[] ivAndEncryptedKey = new byte[Integer.SIZE + ivLength + encryptedKeyForRealm.length];

        final ByteBuffer buffer = ByteBuffer.wrap(ivAndEncryptedKey);
        buffer.order(ORDER_FOR_ENCRYPTED_DATA);
        buffer.putInt(ivLength);
        if (ivLength > 0) {
            buffer.put(iv);
        }
        buffer.put(encryptedKeyForRealm);
        saveEncryptedRealmKey(context, ivAndEncryptedKey);
        return ivAndEncryptedKey;
    }

    private byte[] decryptKeyForRealm(byte[] ivAndEncryptedKey) {
        final Cipher cipher = prepareCipher();
        final KeyStore ks = prepareKeyStore();

        final ByteBuffer buffer = ByteBuffer.wrap(ivAndEncryptedKey);
        buffer.order(ORDER_FOR_ENCRYPTED_DATA);

        final int ivLength = buffer.getInt();
        final byte[] iv = (ivLength > 0) ? new byte[ivLength] : null;
        final byte[] encryptedKey = new byte[ivAndEncryptedKey.length - Integer.SIZE - ivLength];

        if (iv != null) {
            buffer.get(iv);
        }
        buffer.get(encryptedKey);

        try {
            final Key key = ks.getKey(KEY_ALIAS, null);

            if (iv != null) {
                final IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key);
            }

            return cipher.doFinal(encryptedKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("key is invalid.");
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | BadPaddingException
                | KeyStoreException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] loadEncryptedRealmKey(Context context) {
        final SharedPreferences pref = getPreference(context);
        final String encodedEncryptionKey = pref.getString(STORAGE_PREF_KEY, null);
        if (encodedEncryptionKey == null) {
            return null;
        }
        return Base64.decode(encodedEncryptionKey, Base64.DEFAULT);
    }

    private void saveEncryptedRealmKey(Context context, byte[] ivAndEncryptedKey) {
        String encodedEncryptionKey = Base64.encodeToString(ivAndEncryptedKey, Base64.NO_WRAP);
        getPreference(context).edit()
                .putString(STORAGE_PREF_KEY, encodedEncryptionKey)
                .apply();
    }

    private SharedPreferences getPreference(Context context) {
        return context.getSharedPreferences(STORAGE_PREF_NAME, Context.MODE_PRIVATE);
    }

}
