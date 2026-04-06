package org.wx.core.wxBase.unit;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AesUtil {

    // 生成固定长度 32 字节 key（AES-256）
    private static byte[] fixKey(String key) {
        byte[] bytes = new byte[32]; // 256 bit
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(keyBytes, 0, bytes, 0, Math.min(keyBytes.length, bytes.length));
        return bytes;
    }

    public static String encrypt(String data, String salt) {
        try {
            SecretKeySpec key = new SecretKeySpec(fixKey(salt), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("加密失败: " + e.getMessage());
        }
    }

    public static String decrypt(String encryptedData, String salt) {
        try {
            SecretKeySpec key = new SecretKeySpec(fixKey(salt), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("解密失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String decrypt = decrypt("Qswo8W+BpHZzdg6Vm6ikmhEU7O8dzyVPW2+F3hgCu326Z7wLr1RyTYazFG51rULjAzvwJ03r4a/mYsHtDLnIxbjiI4BqNHKGLETML20aeYE=", "WXMAX");
        System.out.println(decrypt);
    }
}
