package practice;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CipherService {
    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = "TopSecret1234567".getBytes();

    public static byte[] encrypt(byte[] plainText) throws Exception {
        SecretKeySpec secret_key = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secret_key);
        return cipher.doFinal(plainText);
    }

    public static byte[] decrypt(byte[] cipherText) throws Exception {
        SecretKeySpec secret_key = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secret_key);
        return cipher.doFinal(cipherText);
    }
}
