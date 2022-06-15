package com.common.encryption;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AES {
    //解密
    public static String decrypt(String Base64Key, byte[] decPass) {
        try {
            SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(Base64Key), "AES");
            //指定使用AES解密
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] bytes = cipher.doFinal(decPass);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //加密
    public static byte[] encryption(String Base64Key, String Password) {
        try {
            SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(Base64Key), "AES");
            //指定使用AES加密
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] bytes = cipher.doFinal(Password.getBytes("utf-8"));
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
