package com.common.key;

/**
 * @ClassName CheckKey
 * @Data 2019/11/8 11:12
 * @Auther Luo
 **/

import com.common.hash.SM3Digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class CheckKey {
    /**
     * @param type KeyType.ED25519 or KeyType.ECCSM2
     * @param key  Private toolkit.key or public toolkit.key
     * @return true or false
     */
    public static boolean CheckSum(KeyType type, byte[] key) {
        return checkKey(type, key);
    }

    /**
     * get toolkit.hash
     *
     * @param type KeyType.ED25519 or KeyType.ECCSM2
     * @param data Data before toolkit.hash
     * @return data after toolkit.hash
     */
    public static byte[] CalHash(KeyType type, byte[] data) {
        byte[] result = null;
        switch (type) {
            case ED25519: {
                MessageDigest sha256 = null;
                try {
                    sha256 = MessageDigest.getInstance("SHA-256");

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                sha256.update(data);
                result = sha256.digest();
                break;
            }
            case ECCSM2: {
                result = SM3Digest.Hash(data);
            }
        }
        return result;
    }

    private static boolean checkKey(KeyType type, byte[] key) {
        boolean SumIsRight = true;
        byte[] checkSrc = new byte[key.length - 4];
        byte[] checkSum = new byte[4];
        System.arraycopy(key, 0, checkSrc, 0, checkSrc.length);
        System.arraycopy(key, checkSrc.length, checkSum, 0, 4);

        byte[] hash1 = CalHash(type, checkSrc);
        byte[] hash2 = CalHash(type, hash1);

        byte[] HashSum = new byte[4];
        System.arraycopy(hash2, 0, HashSum, 0, 4);
        if (!Arrays.equals(HashSum, checkSum)) {
            SumIsRight = false;
        }
        return SumIsRight;
    }

}

