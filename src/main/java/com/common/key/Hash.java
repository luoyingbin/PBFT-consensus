package com.common.key;


import com.common.encryption.HexFormat;
import com.common.hash.SM3Digest;
import com.common.hash.Sha256Util;

public class Hash {
    public static String getEd25519HASH(byte[] data) {
        return hash(data, KeyType.ED25519);
    }

    public static String getSM3HASH(byte[] data) {
        return hash(data, KeyType.ECCSM2);
    }

    public static String hash(byte[] data, KeyType keyType) {
        String hash = null;
        switch (keyType) {
            case ED25519: {
                hash = HexFormat.encodeHexString(Sha256Util.getSHA2562(data));
                break;
            }
            case ECCSM2: {
                SM3Digest digest = new SM3Digest();
                digest.update(data, 0, data.length);
                byte[] hashsm = new byte[digest.getDigestSize()];
                digest.doFinal(hashsm, 0);
                hash = HexFormat.encodeHexString(hashsm);
                break;
            }
        }
        return hash;
    }
}
