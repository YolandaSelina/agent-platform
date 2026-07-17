package com.rk.agent.llm.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加密器（AES-GCM）
 * 密钥从 agent.jwt.secret 派生（SHA-256 取前 32 字节）
 * 输出：base64(iv(12) || ciphertext+tag)
 */
public class KeyEncryptor {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private static volatile byte[] keyBytes;

    public static synchronized void init(String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            keyBytes = md.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("KeyEncryptor init failed", e);
        }
    }

    private static byte[] key() {
        if (keyBytes == null) {
            init("agent-platform-default-secret-please-change-in-production-32bytes");
        }
        return keyBytes;
    }

    public static String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer bb = ByteBuffer.allocate(iv.length + ct.length);
            bb.put(iv).put(ct);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new RuntimeException("KeyEncryptor.encrypt failed", e);
        }
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            byte[] ct = new byte[all.length - IV_LEN];
            System.arraycopy(all, IV_LEN, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 兼容未加密的明文（旧数据迁移）
            return encrypted;
        }
    }

    public static String mask(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
